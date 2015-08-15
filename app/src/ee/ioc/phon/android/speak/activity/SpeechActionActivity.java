package ee.ioc.phon.android.speak.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.VoiceImeView;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class SpeechActionActivity extends AbstractRecognizerIntentActivity {

    void showError() {
        // TODO
    }

    /**
     * TODO: get the audio
     */
    Uri getAudioUri() {
        return null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpActivity(R.layout.activity_recognizer);
        setUpExtras();
    }

    @Override
    public void onStart() {
        super.onStart();

        setUpSettingsButton();

        VoiceImeView view = (VoiceImeView) findViewById(R.id.vVoiceImeView);
        EditorInfo ei = new EditorInfo();
        view.setListener(ei, getVoiceImeViewListener());

        // Launch recognition immediately (if set so)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart)) {
            Log.i("Auto-starting");
            view.start();
        }
    }


    private VoiceImeView.VoiceImeViewListener getVoiceImeViewListener() {
        return new VoiceImeView.VoiceImeViewListener() {

            @Override
            public void onPartialResult(ArrayList<String> results) {
                // Ignore the partial results
            }

            @Override
            public void onFinalResult(ArrayList<String> results) {
                if (results != null && results.size() > 0) {
                    returnOrForwardMatches(results);
                }
            }

            @Override
            public void onSwitchIme(boolean isAskUser) {
                // Not applicable
            }

            @Override
            public void onGo() {
                // Not applicable
            }

            @Override
            public void onDeleteLastWord() {
                // Not applicable
            }

            @Override
            public void onAddNewline() {
                // Not applicable
            }

            @Override
            public void onAddSpace() {
                // Not applicable
            }
        };
    }
}