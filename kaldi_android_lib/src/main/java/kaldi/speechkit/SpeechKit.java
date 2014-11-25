package kaldi.speechkit;

import org.apache.http.message.BasicNameValuePair;

import java.util.List;

import kaldi.speechkit.Recognizer.Listener;

public class SpeechKit {
    private String mWsServiceUrl;
    private List<BasicNameValuePair> mEditorInfo;

    private SpeechKit(String wsServiceUrl, List<BasicNameValuePair> editorInfo) {
        mWsServiceUrl = wsServiceUrl;
        mEditorInfo = editorInfo;
    }

    public static SpeechKit initialize(String wsService, List<BasicNameValuePair> editorInfo) {
        return new SpeechKit(wsService, editorInfo);
    }

    public Recognizer createRecognizer(String language, Listener _listener) {
        return new Recognizer(mWsServiceUrl, language, _listener, mEditorInfo);
    }

    public String getServiceUrl() {
        return mWsServiceUrl;
    }

}