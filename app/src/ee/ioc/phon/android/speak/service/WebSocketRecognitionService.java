package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RawAudioRecorder;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.android.speak.utils.QueryUtils;

/**
 * Implements RecognitionService, connects to the server via WebSocket.
 */
public class WebSocketRecognitionService extends AbstractRecognitionService {

    private static final String EOS = "EOS";

    private static final String PROTOCOL = "";

    private static final String DEFAULT_WS_ARGS =
            "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)16000,+format=(string)S16LE,+channels=(int)1";

    private static final int MSG_RESULT = 1;
    private static final int MSG_ERROR = 2;

    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;

    private MyHandler mMyHandler;

    private Runnable mSendRunnable;

    private WebSocket mWebSocket;

    private String mUrl;

    private boolean mIsEosSent;

    @Override
    void configure(Intent recognizerIntent) throws IOException {
        ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, getExtras(), null);
        mUrl = getServerUrl(R.string.keyWsServer, R.string.defaultWsServer)
                + DEFAULT_WS_ARGS + QueryUtils.getQueryParams(recognizerIntent, builder, "UTF-8");
        configureHandler(getExtras().getBoolean(Extras.EXTRA_UNLIMITED_DURATION, false),
                getExtras().getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false));
    }

    @Override
    void connect() {
        startSocket(mUrl);
    }

    @Override
    void disconnect() {
        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendRunnable);
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }

        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.end(); // TODO: or close?
            mWebSocket = null;
        }
    }

    @Override
    boolean isAudioCues() {
        return PreferenceUtils.getPrefBoolean(getSharedPreferences(), getResources(), R.string.keyImeAudioCues, R.bool.defaultImeAudioCues);
    }

    String getDefaultWsArgs() {
        return DEFAULT_WS_ARGS;
    }

    void configureHandler(boolean isUnlimitedDuration, boolean isPartialResults) {
        mMyHandler = new MyHandler(this, isUnlimitedDuration, isPartialResults);
    }

    private void handleResult(String text) {
        Message msg = new Message();
        msg.what = MSG_RESULT;
        msg.obj = text;
        mMyHandler.sendMessage(msg);
    }

    private void handleException(Exception error) {
        Message msg = new Message();
        msg.what = MSG_ERROR;
        msg.obj = error;
        mMyHandler.sendMessage(msg);
    }

    /**
     * Opens the socket and starts recording/sending.
     *
     * @param url Webservice URL
     */
    void startSocket(String url) {
        mIsEosSent = false;
        Log.i(url);

        AsyncHttpClient.getDefaultInstance().websocket(url, PROTOCOL, new AsyncHttpClient.WebSocketConnectCallback() {

            @Override
            public void onCompleted(Exception ex, final WebSocket webSocket) {
                mWebSocket = webSocket;

                if (ex != null) {
                    handleException(ex);
                    return;
                }

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        Log.i(s);
                        handleResult(s);
                    }
                });

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex == null) {
                            Log.e("ClosedCallback");
                            handleFinish(mIsEosSent);
                        } else {
                            Log.e("ClosedCallback: ", ex);
                            handleException(ex);
                        }
                    }
                });

                webSocket.setEndCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        if (ex == null) {
                            Log.e("EndCallback");
                            handleFinish(mIsEosSent);
                        } else {
                            Log.e("EndCallback: ", ex);
                            handleException(ex);
                        }
                    }
                });

                startSending(webSocket);
            }
        });
    }


    private void startSending(final WebSocket webSocket) {
        HandlerThread thread = new HandlerThread("WsSendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mSendLooper = thread.getLooper();
        mSendHandler = new Handler(mSendLooper);

        // Send chunks to the server
        mSendRunnable = new Runnable() {
            public void run() {
                if (webSocket != null && webSocket.isOpen()) {
                    RawAudioRecorder recorder = getRecorder();
                    if (recorder == null) {
                        Log.i("Sending: EOS (recorder == null)");
                        webSocket.send(EOS);
                        mIsEosSent = true;
                    } else {
                        RawAudioRecorder.State recorderState = recorder.getState();
                        byte[] buffer = recorder.consumeRecordingAndTruncate();
                        // We assume that if only 0 bytes have been recorded then the recording
                        // has finished and we can notify the server with "EOF".
                        if (buffer.length > 0 && recorderState == RawAudioRecorder.State.RECORDING) {
                            Log.i("Sending bytes: " + buffer.length);
                            webSocket.send(buffer);
                            onBufferReceived(buffer);
                            boolean success = mSendHandler.postDelayed(this, TASK_INTERVAL_IME_SEND);
                            if (!success) {
                                Log.i("mSendHandler.postDelayed returned false");
                            }
                        } else {
                            Log.i("Sending: EOS. Buffer length = " + buffer.length + ", recorder state = " + recorderState);
                            webSocket.send(EOS);
                            mIsEosSent = true;
                        }
                    }
                }
            }
        };

        mSendHandler.postDelayed(mSendRunnable, TASK_DELAY_IME_SEND);
    }


    private static class MyHandler extends Handler {
        private final WeakReference<WebSocketRecognitionService> mRef;
        private final boolean mIsUnlimitedDuration;
        private final boolean mIsPartialResults;

        public MyHandler(WebSocketRecognitionService c, boolean isUnlimitedDuration, boolean isPartialResults) {
            mRef = new WeakReference<>(c);
            mIsUnlimitedDuration = isUnlimitedDuration;
            mIsPartialResults = isPartialResults;
        }

        @Override
        public void handleMessage(Message msg) {
            WebSocketRecognitionService outerClass = mRef.get();
            if (outerClass != null) {
                if (msg.what == MSG_ERROR) {
                    Exception e = (Exception) msg.obj;
                    if (e instanceof TimeoutException) {
                        outerClass.onError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT);
                    } else {
                        outerClass.onError(SpeechRecognizer.ERROR_NETWORK);
                    }
                } else if (msg.what == MSG_RESULT) {
                    try {
                        WebSocketResponse response = new WebSocketResponse((String) msg.obj);
                        int statusCode = response.getStatus();
                        if (statusCode == WebSocketResponse.STATUS_SUCCESS && response.isResult()) {
                            WebSocketResponse.Result responseResult = response.parseResult();
                            if (responseResult.isFinal()) {
                                ArrayList<String> hypotheses = responseResult.getHypothesesPp();
                                if (hypotheses == null || hypotheses.isEmpty()) {
                                    Log.i("Empty final result (" + hypotheses + "), stopping");
                                    outerClass.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                                } else {
                                    // We stop listening unless the caller explicitly asks us to carry on,
                                    // by setting EXTRA_UNLIMITED_DURATION=true
                                    if (mIsUnlimitedDuration) {
                                        outerClass.onPartialResults(toBundle(hypotheses, true));
                                    } else {
                                        outerClass.mIsEosSent = true;
                                        outerClass.onEndOfSpeech();
                                        outerClass.onResults(toBundle(hypotheses, true));
                                    }
                                }
                            } else {
                                // We fire this only if the caller wanted partial results
                                if (mIsPartialResults) {
                                    ArrayList<String> hypotheses = responseResult.getHypothesesPp();
                                    if (hypotheses == null || hypotheses.isEmpty()) {
                                        Log.i("Empty non-final result (" + hypotheses + "), ignoring");
                                    } else {
                                        outerClass.onPartialResults(toBundle(hypotheses, false));
                                    }
                                }
                            }
                        } else if (statusCode == WebSocketResponse.STATUS_SUCCESS) {
                            // TODO: adaptation_state currently not handled
                        } else if (statusCode == WebSocketResponse.STATUS_ABORTED) {
                            outerClass.onError(SpeechRecognizer.ERROR_SERVER);
                        } else if (statusCode == WebSocketResponse.STATUS_NOT_AVAILABLE) {
                            outerClass.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                        } else if (statusCode == WebSocketResponse.STATUS_NO_SPEECH) {
                            outerClass.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                        } else {
                            // Server sent unsupported status code, client should be updated
                            outerClass.onError(SpeechRecognizer.ERROR_CLIENT);
                        }
                    } catch (WebSocketResponse.WebSocketResponseException e) {
                        // This results from a syntactically incorrect server response object
                        Log.e((String) msg.obj, e);
                        outerClass.onError(SpeechRecognizer.ERROR_SERVER);
                    }
                }
            }
        }
    }
}