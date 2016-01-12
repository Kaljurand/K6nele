package ee.ioc.phon.android.speak.service;

import android.content.Intent;

import java.io.IOException;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.android.speak.utils.QueryUtils;
import ee.ioc.phon.android.speechutils.AudioRecorder;

public class WebSocketRecognitionService2 extends WebSocketRecognitionService {

    private String mUrl;
    private AudioRecorder mAudioRecorder;

    @Override
    AudioRecorder getAudioRecorder() {
        if (mAudioRecorder == null) {
            mAudioRecorder = createAudioRecorder(PreferenceUtils.getPrefString(getSharedPreferences(), getResources(),
                    R.string.keyImeAudioEncoder, R.string.defaultAudioEncoder),
                    getSampleRate());
        }
        return mAudioRecorder;
    }

    @Override
    void configure(Intent recognizerIntent) throws IOException {
        ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, recognizerIntent.getExtras(), null);
        mUrl = "ws://localhost:82/duplex-speech-api/ws/speech"
                + getAudioRecorder().getWsArgs() + QueryUtils.getQueryParams(recognizerIntent, builder, "UTF-8");
        configureHandler(false, false);
    }

    @Override
    void connect() {
        startSocket(mUrl);
    }

}