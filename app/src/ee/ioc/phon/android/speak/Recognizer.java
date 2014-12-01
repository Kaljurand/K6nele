package ee.ioc.phon.android.speak;

import android.os.Handler;
import android.os.Message;
import android.speech.SpeechRecognizer;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;
import java.util.concurrent.TimeoutException;

// TODO: remove logging
// TODO: communicate audio errors

public class Recognizer {

    private static final String WS_ARGS =
            "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)16000,+format=(string)S16LE,+channels=(int)1";

    private String mWsServiceUrl;
    private PcmRecorder mPcmRecorder;
    private Listener mRecogListener;
    private List<BasicNameValuePair> mHeadersWithEditorInfo;

    private Thread thRecord;
    private Handler mHandlerResult;
    private Handler mHandlerError;
    private Handler mHandlerFinish;
    private boolean mIsRecording;

    private WebSocket mWebSocket;

    public Recognizer(String wsServiceUrl, String language, Listener listener, List<BasicNameValuePair> editorInfo) {
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


    public boolean isRecording() {
        return mIsRecording;
    }

    public void setListener(Listener listener) {
        mRecogListener = listener;
    }

    public void start() {
        mPcmRecorder = new PcmRecorder();
        newsocket(mWsServiceUrl + "speech" + WS_ARGS + "&" + URLEncodedUtils.format(mHeadersWithEditorInfo, "utf-8"),
                mPcmRecorder);
    }

    public void stopRecording() {
        stopRecord();
        if (mWebSocket != null) {
            mWebSocket.send("EOS");
        }
        // TODO: fire this only if the recorder says that it is done
        mRecogListener.onRecordingDone();
        mIsRecording = false;
    }

    public void cancel() {
        stopRecord();
        if (mWebSocket != null) {
            mWebSocket.send("EOS");
            mWebSocket.close(); // TODO: or end?
        }
        mIsRecording = false;
    }

    private void startRecord() {
        thRecord = new Thread(mPcmRecorder);
        thRecord.start();
        mPcmRecorder.setRecording(true);
        mIsRecording = true;
    }

    private void stopRecord() {
        mPcmRecorder.setRecording(false);
    }


    private void newsocket(String get, final PcmRecorder audioRecorder) {
        Log.i(get);
        AsyncHttpClient.getDefaultInstance().websocket(get, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, final WebSocket webSocket) {
                if (ex != null) {
                    handelError(ex);
                    return;
                }

                mWebSocket = webSocket;

                audioRecorder.setListener(new PcmRecorder.RecorderListener() {
                    public void onRecorderBuffer(byte[] buffer) {
                        if (buffer != null) {
                            Log.i("read " + buffer.length);
                            webSocket.send(buffer);
                        }
                    }
                });

                // Start recording
                mRecogListener.onRecordingBegin();
                startRecord();

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        Log.i(s);
                        handelResult(s);
                    }
                });

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.e("ClosedCallback: ", ex);
                        if (ex == null) {
                            handelFinish();
                        } else {
                            handelError(ex);
                        }
                    }
                });

                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.e("EndCallback: ", ex);
                        if (ex == null) {
                            handelFinish();
                        } else {
                            handelError(ex);
                        }
                    }
                });
            }
        });
    }

    private void handelResult(String text) {
        Message msg = new Message();
        msg.obj = text;
        mHandlerResult.sendMessage(msg);
    }

    private void handelError(Exception error) {
        Message msg = new Message();
        msg.obj = error;
        mHandlerError.sendMessage(msg);
    }

    private void handelFinish() {
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
    }
}