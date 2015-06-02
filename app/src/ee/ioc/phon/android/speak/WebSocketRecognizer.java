package ee.ioc.phon.android.speak;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class WebSocketRecognizer extends RecognitionService {

    private static final String PROTOCOL = "";

    private static final String WS_ARGS =
            "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)16000,+format=(string)S16LE,+channels=(int)1";

    private static final int MSG_RESULT = 1;
    private static final int MSG_ERROR = 2;

    private RawAudioRecorder mRecorder;
    private RecognitionService.Callback mListener;

    private MyHandler mMyHandler;

    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;
    private Handler mVolumeHandler = new Handler();

    private Runnable mSendTask;
    private Runnable mShowVolumeTask;

    private WebSocket mWebSocket;

    public void onDestroy() {
        super.onDestroy();
        onCancel0();
    }


    /**
     * Opens the socket and starts recording and sending the recorded packages.
     */
    @Override
    protected void onStartListening(final Intent recognizerIntent, RecognitionService.Callback listener) {
        Log.i("onStartListening");
        mListener = listener;

        mMyHandler = new MyHandler(this,
                listener,
                recognizerIntent.getBooleanExtra(Extras.EXTRA_UNLIMITED_DURATION, false),
                recognizerIntent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        );

        try {
            onReadyForSpeech(new Bundle());
            startRecord();
            onBeginningOfSpeech();
            startSocket(getWsServiceUrl(recognizerIntent) + "speech" + WS_ARGS + getQueryParams(recognizerIntent));
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_AUDIO);
        }
    }

    /**
     * Stops the recording and closes the socket.
     */
    @Override
    protected void onCancel(RecognitionService.Callback listener) {
        onCancel0();
        // Send empty results if recognition is cancelled
        // TEST: if it works with Google Translate and Slide IT
        onResults(new Bundle());
    }

    /**
     * Stops the recording and informs the socket that no more packages are coming.
     */
    @Override
    protected void onStopListening(RecognitionService.Callback listener) {
        stopRecording0();
        onEndOfSpeech();
    }


    private void stopRecording0() {
        if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);

        if (mRecorder != null) {
            mRecorder.release();
        }
    }

    private void onCancel0() {
        stopRecording0();

        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendTask);
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }

        if (mWebSocket != null && mWebSocket.isOpen()) {
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
            throw new IOException();
        }

        if (mRecorder.getState() != RawAudioRecorder.State.READY) {
            throw new IOException();
        }

        mRecorder.start();

        if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
            throw new IOException();
        }


        // Monitor the volume level
        mShowVolumeTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    onRmsChanged(mRecorder.getRmsdb());
                    mVolumeHandler.postDelayed(this, Constants.TASK_INTERVAL_VOL);
                }
            }
        };

        mVolumeHandler.postDelayed(mShowVolumeTask, Constants.TASK_DELAY_VOL);
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
                    handleException(ex);
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
                            handleException(ex);
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
                            handleException(ex);
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
                    if (webSocket != null && webSocket.isOpen()) {
                        RawAudioRecorder.State recorderState = mRecorder.getState();
                        Log.i("Recorder state = " + recorderState);
                        byte[] buffer = mRecorder.consumeRecordingAndTruncate();
                        // We assume that if only 0 bytes have been recorded then the recording
                        // has finished and we can notify the server with "EOF".
                        if (buffer.length > 0 && recorderState == RawAudioRecorder.State.RECORDING) {
                            Log.i("Sending bytes: " + buffer.length);
                            webSocket.send(buffer);
                            boolean success = mSendHandler.postDelayed(this, Constants.TASK_INTERVAL_IME_SEND);
                            if (!success) {
                                Log.i("mSendHandler.postDelayed returned false");
                            }
                        } else {
                            Log.i("Sending: EOS");
                            webSocket.send("EOS");
                        }
                    }
                }
            }
        };


        mSendHandler.postDelayed(mSendTask, Constants.TASK_DELAY_IME_SEND);
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

    // TODO: call onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT); if server initiates close
    // without having received EOS
    private void handleFinish() {
        onCancel(mListener);
    }

    private void onReadyForSpeech(Bundle bundle) {
        try {
            mListener.readyForSpeech(bundle);
        } catch (RemoteException e) {
        }
    }

    private void onRmsChanged(float rms) {
        try {
            mListener.rmsChanged(rms);
        } catch (RemoteException e) {
        }
    }

    private void onError(int errorCode) {
        // As soon as there is an error we shut down the socket and the recorder
        onCancel0();
        try {
            mListener.error(errorCode);
        } catch (RemoteException e) {
        }
    }

    private void onResults(Bundle bundle) {
        try {
            mListener.results(bundle);
        } catch (RemoteException e) {
        }
    }

    private void onPartialResults(Bundle bundle) {
        try {
            mListener.partialResults(bundle);
        } catch (RemoteException e) {
        }
    }

    private void onBeginningOfSpeech() {
        try {
            mListener.beginningOfSpeech();
        } catch (RemoteException e) {
        }
    }

    private void onEndOfSpeech() {
        try {
            mListener.endOfSpeech();
        } catch (RemoteException e) {
        }
    }

    private String getWsServiceUrl(Intent intent) {
        String url = intent.getStringExtra(Extras.EXTRA_SERVER_URL);
        if (url == null) {
            return PreferenceUtils.getPrefString(
                    PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()),
                    getResources(),
                    R.string.keyServerWs,
                    R.string.defaultServerWs);
        }
        return url;
    }

    /**
     * Extracts the editor info, and uses
     * ChunkedWebRecSessionBuilder to extract some additional extras.
     * TODO: unify this better
     */
    private String getQueryParams(Intent intent) {
        List<BasicNameValuePair> list = new ArrayList<>();
        flattenBundle("editorInfo_", list, intent.getBundleExtra(Extras.EXTRA_EDITOR_INFO));

        try {
            ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, intent.getExtras(), null);
            if (Log.DEBUG) Log.i(builder.toStringArrayList());
            // TODO: review these parameter names
            listAdd(list, "lang", builder.getLang());
            listAdd(list, "lm", toString(builder.getGrammarUrl()));
            listAdd(list, "output-lang", builder.getGrammarTargetLang());
            listAdd(list, "user-agent", builder.getUserAgentComment());
            listAdd(list, "calling-package", builder.getCaller());
            listAdd(list, "user-id", builder.getDeviceId());
            listAdd(list, "partial", "" + builder.isPartialResults());
        } catch (MalformedURLException e) {
        }

        if (list.size() == 0) {
            return "";
        }
        return "&" + URLEncodedUtils.format(list, "utf-8");
    }


    private static boolean listAdd(List<BasicNameValuePair> list, String key, String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        return list.add(new BasicNameValuePair(key, value));
    }

    private static void flattenBundle(String prefix, List<BasicNameValuePair> list, Bundle bundle) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    if (value instanceof Bundle) {
                        flattenBundle(prefix + key + "_", list, (Bundle) value);
                    } else {
                        list.add(new BasicNameValuePair(prefix + key, toString(value)));
                    }
                }
            }
        }
    }

    private static Bundle toBundle(ArrayList<String> hypotheses, boolean isFinal) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, hypotheses);
        bundle.putBoolean(Extras.EXTRA_SEMI_FINAL, isFinal);
        return bundle;
    }

    // TODO: replace by a built-in
    private static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }


    private static class MyHandler extends Handler {
        private final WeakReference<WebSocketRecognizer> mRef;
        private final RecognitionService.Callback mCallback;
        private final boolean mIsUnlimitedDuration;
        private final boolean mIsPartialResults;

        public MyHandler(WebSocketRecognizer c, RecognitionService.Callback callback, boolean isUnlimitedDuration, boolean isPartialResults) {
            mRef = new WeakReference<>(c);
            mCallback = callback;
            mIsUnlimitedDuration = isUnlimitedDuration;
            mIsPartialResults = isPartialResults;
        }

        @Override
        public void handleMessage(Message msg) {
            WebSocketRecognizer outerClass = mRef.get();
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
                                        outerClass.onStopListening(mCallback);
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