package ee.ioc.phon.android.speak.service;

import android.content.Intent;

import java.io.IOException;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.utils.QueryUtils;
import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.AudioRecorderFactory;

public class WebSocketRecognitionService2 extends WebSocketRecognitionService {

    private String mUrl;

    @Override
    void configure(Intent recognizerIntent) throws IOException {
        AudioRecorder audioRecorder = AudioRecorderFactory.getAudioRecorder();
        ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, recognizerIntent.getExtras(), null);
        mUrl = "ws://localhost:82/duplex-speech-api/ws/speech"
                + audioRecorder.getWsArgs() + QueryUtils.getQueryParams(recognizerIntent, builder, "UTF-8");
        configureHandler(false, false);
    }

    @Override
    void connect() {
        startSocket(mUrl);
    }

}