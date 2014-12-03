package ee.ioc.phon.android.speak;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.speech.SpeechRecognizer;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class WebSocketRecognizer {

    private static final String PROTOCOL = "";

    private static final String WS_ARGS =
            "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)16000,+format=(string)S16LE,+channels=(int)1";

    private String mWsServiceUrl;
    private RawAudioRecorder mRecorder;
    private Listener mRecogListener;
    private List<BasicNameValuePair> mHeadersWithEditorInfo;

    private Handler mHandlerResult;
    private Handler mHandlerError;
    private Handler mHandlerFinish;

    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;
    private Handler mVolumeHandler = new Handler();

    private Runnable mSendTask;
    private Runnable mShowVolumeTask;

    private WebSocket mWebSocket;

    public WebSocketRecognizer(String wsServiceUrl, Listener listener, List<BasicNameValuePair> editorInfo) {
        mWsServiceUrl = wsServiceUrl;
        mRecogListener = listener;
        mHeadersWithEditorInfo = editorInfo;

        mHandlerResult = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    Response response = Response.parseResponse((String) msg.obj);
                    if (response instanceof Response.ResponseResult) {
                        Response.ResponseResult responseResult = (Response.ResponseResult) response;
                        String text = responseResult.getText();
                        if (responseResult.isFinal()) {
                            mRecogListener.onFinalResult(text);
                        } else {
                            mRecogListener.onPartialResult(text);
                        }
                    } else if (response instanceof Response.ResponseMessage) {
                        Response.ResponseMessage responseMessage = (Response.ResponseMessage) response;
                        Log.i(responseMessage.getStatus() + ": " + responseMessage.getMessage());
                        mRecogListener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                    }
                } catch (Response.ResponseException e) {
                    Log.e((String) msg.obj, e);
                    mRecogListener.onError(SpeechRecognizer.ERROR_SERVER);
                }
            }
        };

        mHandlerError = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Exception e = (Exception) msg.obj;
                Log.e("Socket error?", e);
                if (e instanceof TimeoutException) {
                    mRecogListener.onError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT);
                } else {
                    mRecogListener.onError(SpeechRecognizer.ERROR_NETWORK);
                }
            }
        };

        mHandlerFinish = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mRecogListener.onFinish();
            }
        };
    }


    /**
     * @return true iff audio recorder is currently recording
     */
    public boolean isRecording() {
        return mRecorder.getState() == RawAudioRecorder.State.RECORDING;
    }


    /**
     * Opens the socket and starts recording and sending the recorded packages.
     */
    public void start() {
        Log.i("start");
        try {
            startRecord();
            mRecogListener.onRecordingBegin();
            startSocket(mWsServiceUrl + "speech" + WS_ARGS + "&" + URLEncodedUtils.format(mHeadersWithEditorInfo, "utf-8"));
        } catch (IOException e) {
            mRecogListener.onError(SpeechRecognizer.ERROR_AUDIO);
        }
    }

    /**
     * Stops the recording and informs the socket that no more packages are coming.
     */
    public void stopRecording() {
        stopRecording0();
        mRecogListener.onRecordingDone();
    }

    // TODO: review this
    public void stopRecording0() {
        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendTask);
        if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);
        if (mWebSocket != null) {
            mWebSocket.send("EOS");
        }

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }

        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }
    }

    /**
     * Stops the recording and closes the socket.
     */
    public void cancel() {
        stopRecording0();
        if (mWebSocket != null) {
            mWebSocket.end(); // TODO: or close?
        }
    }


    /**
     * Starts recording.
     *
     * @throws IOException if there was an error, e.g. another app is currently recording
     */
    private void startRecord() throws IOException {
        mRecorder = new RawAudioRecorder();
        if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
            mRecorder = null;
            throw new IOException();
        }

        if (mRecorder.getState() != RawAudioRecorder.State.READY) {
            throw new IOException();
        }

        mRecorder.start();

        if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
            throw new IOException();
        }
    }


    /**
     * Opens the socket and starts recording/sending.
     *
     * @param url Webservice URL
     */
    private void startSocket(String url) {
        Log.i(url);

        AsyncHttpClient.getDefaultInstance().websocket(url, PROTOCOL, new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, final WebSocket webSocket) {
                if (ex != null) {
                    handleError(ex);
                    return;
                }

                mWebSocket = webSocket;
                startSending(webSocket);

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        Log.i(s);
                        handleResult(s);
                    }
                });

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.e("ClosedCallback: ", ex);
                        if (ex == null) {
                            handleFinish();
                        } else {
                            handleError(ex);
                        }
                    }
                });

                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.e("EndCallback: ", ex);
                        if (ex == null) {
                            handleFinish();
                        } else {
                            handleError(ex);
                        }
                    }
                });
            }
        });
    }


    private void startSending(final WebSocket webSocket) {
        HandlerThread thread = new HandlerThread("SendHandlerThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mSendLooper = thread.getLooper();
        mSendHandler = new Handler(mSendLooper);

        // Send chunks to the server
        mSendTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    byte[] buffer = mRecorder.consumeRecordingAndTruncate();
                    webSocket.send(buffer);
                    mSendHandler.postDelayed(this, Constants.TASK_INTERVAL_IME_SEND);
                }
            }
        };

        // Monitor the volume level
        mShowVolumeTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    mRecogListener.onRmsChanged(mRecorder.getRmsdb());
                    mVolumeHandler.postDelayed(this, Constants.TASK_INTERVAL_VOL);

                }
            }
        };

        mSendHandler.postDelayed(mSendTask, Constants.TASK_DELAY_IME_SEND);
        mVolumeHandler.postDelayed(mShowVolumeTask, Constants.TASK_DELAY_VOL);
    }

    private void handleResult(String text) {
        Message msg = new Message();
        msg.obj = text;
        mHandlerResult.sendMessage(msg);
    }

    private void handleError(Exception error) {
        Message msg = new Message();
        msg.obj = error;
        mHandlerError.sendMessage(msg);
    }

    private void handleFinish() {
        Message msg = new Message();
        mHandlerFinish.sendMessage(msg);
    }


    /**
     * These methods will be called by the GUI.
     */
    public interface Listener {
        void onRecordingBegin();

        void onRecordingDone();

        void onError(int errorCode);

        void onPartialResult(String text);

        void onFinalResult(String text);

        void onFinish();

        void onRmsChanged(float rms);
    }
}