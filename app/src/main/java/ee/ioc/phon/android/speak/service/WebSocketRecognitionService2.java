package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.util.Pair;

import java.io.IOException;
import java.util.List;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.utils.QueryUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class WebSocketRecognitionService2 extends WebSocketRecognitionService {

    private String mUrl;

    @Override
    protected String getEncoderType() {
        return PreferenceUtils.getPrefString(getSharedPreferences(), getResources(),
                R.string.keyImeAudioFormat, R.string.defaultAudioFormat);
    }

    @Override
    protected void configure(Intent recognizerIntent) throws IOException {
        ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, recognizerIntent.getExtras(), null);
        List<Pair<String, String>> list = QueryUtils.getQueryParams(recognizerIntent, builder);
        list.add(new Pair<>("content-type", getAudioRecorder().getContentType()));
        mUrl = QueryUtils.combine("ws://localhost:82/duplex-speech-api/ws/speech?key1=val1",
                QueryUtils.encodeKeyValuePairs(list, "UTF-8"));
        configureHandler(false, false);
    }

    @Override
    protected void connect() {
        startSocket(mUrl);
    }
}