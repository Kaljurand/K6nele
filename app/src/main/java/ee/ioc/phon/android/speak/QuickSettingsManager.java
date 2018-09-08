package ee.ioc.phon.android.speak;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class QuickSettingsManager {

    private static final Set<String> COMBOS_MULTILINGUAL;

    static {
        Set<String> set = new HashSet<>();
        set.add("ee.ioc.phon.android.speak/.service.WebSocketRecognitionService");
        set.add("ee.ioc.phon.android.speak/.service.WebSocketRecognitionService;et-EE");
        set.add("ee.ioc.phon.android.speak/.service.HttpRecognitionService;et-EE");
        set.add("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService");
        set.add("com.google.android.googlequicksearchbox/com.google.android.voicesearch.serviceapi.GoogleRecognitionService;en-US");
        COMBOS_MULTILINGUAL = Collections.unmodifiableSet(set);
    }

    private SharedPreferences mPrefs;
    private Resources mRes;

    public QuickSettingsManager(SharedPreferences prefs, Resources res) {
        mPrefs = prefs;
        mRes = res;
    }

    public void setDefaultsDevel() {
        SharedPreferences.Editor editor = mPrefs.edit();

        // Speech keyboard
        editor.putStringSet(mRes.getString(R.string.keyImeCombo), COMBOS_MULTILINGUAL);
        editor.putBoolean(mRes.getString(R.string.keyImeHelpText), false);
        editor.putBoolean(mRes.getString(R.string.keyImeAutoStart), false);

        // Search panel
        editor.putStringSet(mRes.getString(R.string.keyCombo), COMBOS_MULTILINGUAL);
        editor.putBoolean(mRes.getString(R.string.keyHelpText), false);
        editor.putBoolean(mRes.getString(R.string.keyAutoStart), false);
        editor.putString(mRes.getString(R.string.keyMaxResults), "4");

        // HTTP service
        editor.putString(mRes.getString(R.string.keyAudioFormat), "audio/x-flac");
        editor.putBoolean(mRes.getString(R.string.keyAudioCues), false);
        editor.putString(mRes.getString(R.string.keyAutoStopAfterTime), "10");

        // WebSocket service
        editor.putString(mRes.getString(R.string.keyImeAudioFormat), "audio/x-flac");
        editor.putBoolean(mRes.getString(R.string.keyImeAudioCues), false);

        editor.apply();
    }
}