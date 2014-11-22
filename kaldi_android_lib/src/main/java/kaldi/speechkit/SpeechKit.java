package kaldi.speechkit;


import android.content.Context;

import kaldi.speechkit.Recognizer.Listener;

public class SpeechKit {
    private String serverAddr;
    private int serverPort;


    public SpeechKit(String appId, String appKey, String serverAddr, int serverPort) {
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    public static SpeechKit initialize(Context c, String appId, String appKey, String serverAddr, int serverPort) {

        return new SpeechKit(appId, appKey, serverAddr, serverPort);
    }

    public void connect() {

    }

    public Recognizer createRecognizer(String language, Listener _listener) {
        return new Recognizer(this.serverAddr, this.serverPort, language, _listener);
    }

    public void setDefaultRecognizerPrompts() {

    }

    public String getHostAddr() {
        return serverAddr;
    }

    public int getPort() {
        return serverPort;
    }

    public String getSessionId() {
        // TODO Auto-generated method stub
        return null;
    }
}
