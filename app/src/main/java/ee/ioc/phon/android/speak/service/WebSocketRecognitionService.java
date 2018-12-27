package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.QueryUtils;
import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.EncodedAudioRecorder;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.service.AbstractRecognitionService;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * Implements RecognitionService, connects to the server via WebSocket.
 */
public class WebSocketRecognitionService extends AbstractRecognitionService {

    // When does the chunk sending start and what is its interval
    private static final int TASK_DELAY_SEND = 100;
    private static final int TASK_INTERVAL_SEND = 200;
    // Limit to the number of hypotheses that the service will return
    // TODO: make configurable
    private static final int MAX_HYPOTHESES = 100;
    // Pretty-print results
    // TODO: make configurable
    private static final boolean PRETTY_PRINT = true;

    private static final String EOS = "EOS";

    private static final String PROTOCOL = "";

    private static final int MSG_RESULT = 1;
    private static final int MSG_ERROR = 2;

    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;

    private MyHandler mMyHandler;

    private Runnable mSendRunnable;

    private WebSocket mWebSocket;

    private String mUrl;

    private boolean mIsEosSent;

    private int mNumBytesSent;

    @Override
    protected void configure(Intent recognizerIntent) throws IOException {
        ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, getExtras(), null);
        mUrl = getServerUrl(R.string.keyWsServer, R.string.defaultWsServer)
                + getAudioRecorder().getWsArgs() + QueryUtils.getQueryParams(recognizerIntent, builder, "UTF-8");
        boolean isUnlimitedDuration = getExtras().getBoolean(Extras.EXTRA_UNLIMITED_DURATION, false)
                || getExtras().getBoolean(Extras.EXTRA_DICTATION_MODE, false);
        configureHandler(isUnlimitedDuration,
                getExtras().getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false));
    }

    @Override
    protected void connect() {
        startSocket(mUrl);
    }

    @Override
    protected void disconnect() {
        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendRunnable);
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }

        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.end(); // TODO: or close?
            mWebSocket = null;
        }
        Log.i("Number of bytes sent: " + mNumBytesSent);
    }

    @Override
    protected String getEncoderType() {
        return PreferenceUtils.getPrefString(getSharedPreferences(), getResources(),
                R.string.keyImeAudioFormat, R.string.defaultAudioFormat);
    }

    @Override
    protected boolean isAudioCues() {
        return PreferenceUtils.getPrefBoolean(getSharedPreferences(), getResources(), R.string.keyImeAudioCues, R.bool.defaultImeAudioCues);
    }

    protected void configureHandler(boolean isUnlimitedDuration, boolean isPartialResults) {
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

        AsyncHttpClient.getDefaultInstance().websocket(url, PROTOCOL, (ex, webSocket) -> {
            mWebSocket = webSocket;

            if (ex != null) {
                handleException(ex);
                return;
            }

            webSocket.setStringCallback(s -> {
                Log.i(s);
                handleResult(s);
            });

            webSocket.setClosedCallback(ex1 -> {
                if (ex1 == null) {
                    Log.e("ClosedCallback");
                    handleFinish(mIsEosSent);
                } else {
                    Log.e("ClosedCallback: ", ex1);
                    handleException(ex1);
                }
            });

            webSocket.setEndCallback(ex12 -> {
                if (ex12 == null) {
                    Log.e("EndCallback");
                    handleFinish(mIsEosSent);
                } else {
                    Log.e("EndCallback: ", ex12);
                    handleException(ex12);
                }
            });

            startSending(webSocket);
        });
    }


    private void startSending(final WebSocket webSocket) {
        mNumBytesSent = 0;
        HandlerThread thread = new HandlerThread("WsSendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mSendLooper = thread.getLooper();
        mSendHandler = new Handler(mSendLooper);

        // Send chunks to the server
        mSendRunnable = new Runnable() {
            public void run() {
                if (webSocket != null && webSocket.isOpen()) {
                    AudioRecorder recorder = getRecorder();
                    if (recorder == null || recorder.getState() != AudioRecorder.State.RECORDING) {
                        Log.i("Sending: EOS (recorder == null)");
                        webSocket.send(EOS);
                        mIsEosSent = true;
                    } else {
                        byte[] buffer = recorder.consumeRecordingAndTruncate();
                        if (recorder instanceof EncodedAudioRecorder) {
                            send(webSocket, ((EncodedAudioRecorder) recorder).consumeRecordingEncAndTruncate());
                        } else {
                            send(webSocket, buffer);
                        }
                        if (buffer.length > 0) {
                            onBufferReceived(buffer);
                        }
                        boolean success = mSendHandler.postDelayed(this, TASK_INTERVAL_SEND);
                        if (!success) {
                            Log.i("mSendHandler.postDelayed returned false");
                        }
                    }
                }
            }
        };

        mSendHandler.postDelayed(mSendRunnable, TASK_DELAY_SEND);
    }

    void send(WebSocket webSocket, byte[] buffer) {
        if (buffer != null && buffer.length > 0) {
            webSocket.send(buffer);
            mNumBytesSent += buffer.length;
            Log.i("Sent bytes: " + buffer.length);
        }
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
                                ArrayList<String> hypotheses = responseResult.getHypotheses(MAX_HYPOTHESES, PRETTY_PRINT);
                                if (hypotheses.isEmpty()) {
                                    Log.i("Empty final result (" + hypotheses + "), stopping");
                                    outerClass.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                                } else {
                                    // We stop listening unless the caller explicitly asks us to carry on,
                                    // by setting EXTRA_UNLIMITED_DURATION=true
                                    if (mIsUnlimitedDuration) {
                                        outerClass.onPartialResults(toResultsBundle(hypotheses, true));
                                    } else {
                                        outerClass.mIsEosSent = true;
                                        outerClass.onEndOfSpeech();
                                        outerClass.onResults(toResultsBundle(hypotheses, true));
                                    }
                                }
                            } else {
                                // We fire this only if the caller wanted partial results
                                if (mIsPartialResults) {
                                    ArrayList<String> hypotheses = responseResult.getHypotheses(MAX_HYPOTHESES, PRETTY_PRINT);
                                    if (hypotheses.isEmpty()) {
                                        Log.i("Empty non-final result (" + hypotheses + "), ignoring");
                                    } else {
                                        outerClass.onPartialResults(toResultsBundle(hypotheses, false));
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
                        } else if (statusCode == WebSocketResponse.STATUS_NO_VALID_FRAMES) {
                            outerClass.onError(SpeechRecognizer.ERROR_NO_MATCH);
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