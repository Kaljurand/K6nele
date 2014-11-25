package kaldi.speechkit;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.codebutler.android_websockets.WebSocketClient;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Recognizer implements RecorderListener {

    private static final String WS_ARGS =
            "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)16000,+format=(string)S16LE,+channels=(int)1";

    protected static final String TAG = "Recognizer";
    private String mWsServiceUrl;
    private Listener recogListener;
    private static final List<BasicNameValuePair> extraHeaders = Arrays.asList(
            new BasicNameValuePair("Cookie", "session=abcd"),
            new BasicNameValuePair("content-type", "audio/x-raw"),
            new BasicNameValuePair("+layout", "(string)interleaved"),
            new BasicNameValuePair("+rate", "16000"),
            new BasicNameValuePair("+format", "S16LE"),
            new BasicNameValuePair("+channels", "1")
    );
    private WebSocketClient ws_client_speech;
    private WebSocketClient ws_client_status;
    private PcmRecorder recorderInstance;
    private WebSocketClient.Listener server_status_listener;
    private WebSocketClient.Listener server_speech_listener;
    private Thread thRecord;
    private Handler _handler_partialResult;
    private Handler _handler_Error;
    private Handler _handler_Finish;
    private boolean mIsRecording;

    public Recognizer(String wsServiceUrl, String language, Listener listener, List<BasicNameValuePair> editorInfo) {
        mWsServiceUrl = wsServiceUrl;
        this.recogListener = listener;
        List<BasicNameValuePair> headersWithEditorInfo = new ArrayList<BasicNameValuePair>(extraHeaders);
        headersWithEditorInfo.addAll(editorInfo);

        _handler_partialResult = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = (String) msg.obj;
                Result tmpResult = null;
                try {
                    tmpResult = Result.parseResult(text);
                } catch (JSONException e) {
                    recogListener.onError(new Exception(e.getMessage()));
                }

                if (tmpResult == null) {
                    recogListener.onError(new Exception(text));
                } else if (tmpResult.isFinal())
                    recogListener.onFinalResult(tmpResult);
                else
                    recogListener.onPartialResult(tmpResult);

            }
        };

        _handler_Error = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Exception error = (Exception) msg.obj;
                recogListener.onError(error);

            }
        };
        _handler_Finish = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String reason = (String) msg.obj;
                recogListener.onFinish(reason);

            }
        };
        server_speech_listener = new WebSocketClient.Listener() {

            @Override
            public void onMessage(byte[] data) {

            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, message);
                handelResult(message);
            }

            @Override
            public void onError(Exception error) {
                //recogListener.onError(error);
                handelError(error);
            }

            @Override
            public void onDisconnect(int code, String reason) {
                Log.d(TAG, "Disconnect! " + reason);
                handelFinish(reason);
            }

            @Override
            public void onConnect() {
                recogListener.onRecordingBegin();
                startRecord();
            }
        };

        server_status_listener = new WebSocketClient.Listener() {

            @Override
            public void onMessage(byte[] data) {
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onError(Exception error) {
            }

            @Override
            public void onDisconnect(int code, String reason) {
            }

            @Override
            public void onConnect() {
            }
        };


        ws_client_speech = new WebSocketClient(
                URI.create(mWsServiceUrl + "speech" + WS_ARGS),
                server_speech_listener,
                headersWithEditorInfo);

        // TODO: do we need the extra headers for the status
        /*
        ws_client_status = new WebSocketClient(
                URI.create("ws://" + serverAddr + ":" + serverPort + WS_DIR + "status"),
                server_status_listener,
                extraHeaders);
                */

        // Initial recorder
        recorderInstance = new PcmRecorder();

        recorderInstance.setListener(Recognizer.this);

    }

    private void handelResult(String text) {
        Message msg = new Message();
        String textTochange = text;
        msg.obj = textTochange;
        _handler_partialResult.sendMessage(msg);
    }

    private void handelError(Exception error) {
        Message msg = new Message();
        msg.obj = error;
        _handler_Error.sendMessage(msg);
    }

    private void handelFinish(String reason) {
        Message msg = new Message();
        msg.obj = reason;
        _handler_Finish.sendMessage(msg);
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void start() {

        //if (! ws_client_status.isConnected())
        //	ws_client_status.connect();
        if (!ws_client_speech.isConnected())
            ws_client_speech.connect();

        mIsRecording = true;
    }

    public void setListener(Listener _listener) {
        // TODO Auto-generated method stub
        this.recogListener = _listener;
    }

    public void cancel() {
        // TODO Auto-generated method stub

        // Stop recording

        // Stop web socket
        if (ws_client_speech != null && ws_client_speech.isConnected()) {
            ws_client_speech.disconnect();
        }
        if (ws_client_status != null && ws_client_speech.isConnected()) {
            ws_client_status.disconnect();
        }
        mIsRecording = false;
    }

    public float getAudioLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void stopRecording() {
        // TODO Auto-generated method stub
        stopRecord();
        // Send EOS signal
        if (ws_client_speech.isConnected())
            ws_client_speech.send("EOS");
        // Notify
        recogListener.onRecordingDone();
        mIsRecording = false;
    }

    public void startRecord() {
        // TODO Auto-generated method stub
        thRecord = new Thread(recorderInstance);
        thRecord.start();
        recorderInstance.setRecording(true);
    }

    public void stopRecord() {
        recorderInstance.setRecording(false);

    }

    @Override
    public void onRecorderBuffer(byte[] buffer) {
        // TODO: not properly closed
        Log.d(TAG, "read " + buffer.length);
        ws_client_speech.send(buffer);
    }

    public interface Listener {
        abstract void onRecordingBegin();

        abstract void onRecordingDone();

        abstract void onError(Exception error);

        abstract void onPartialResult(Result text);

        abstract void onFinalResult(Result result);

        abstract void onFinish(String reason);
    }

}