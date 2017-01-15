package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;


public class ServiceLanguageChooser {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<String> mCombosAsList;
    private final CallerInfo mCallerInfo;
    private final int mKeyImeCurrentCombo;
    private int mIndex;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mIntent;
    private String mLanguage = null;
    private ComponentName mRecognizerComponentName = null;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, int keys, CallerInfo callerInfo) {

        mContext = context;
        mPrefs = prefs;
        mCallerInfo = callerInfo;

        Resources res = context.getResources();

        TypedArray keysAsTypedArray = res.obtainTypedArray(keys);
        int keyImeCombo = keysAsTypedArray.getResourceId(0, 0);
        mKeyImeCurrentCombo = keysAsTypedArray.getResourceId(1, 0);
        int defaultImeCombos = keysAsTypedArray.getResourceId(2, 0);
        keysAsTypedArray.recycle();

        Set<String> mCombos = PreferenceUtils.getPrefStringSet(prefs, res, keyImeCombo);

        if (mCombos == null || mCombos.isEmpty()) {
            // If the user has chosen an empty set of combos
            mCombosAsList = PreferenceUtils.getStringListFromStringArray(res, defaultImeCombos);
        } else {
            mCombosAsList = new ArrayList<>(mCombos);
        }

        String currentCombo = PreferenceUtils.getPrefString(prefs, res, mKeyImeCurrentCombo);
        mIndex = mCombosAsList.indexOf(currentCombo);
        // If the current combo was not found among the choices then select the first combo.
        if (mIndex == -1) {
            incIndex(prefs, res);
        }
        update();
    }


    public SpeechRecognizer getSpeechRecognizer() {
        return mSpeechRecognizer;
    }

    public int size() {
        return mCombosAsList.size();
    }

    public Intent getIntent() {
        return mIntent;
    }

    public void next() {
        incIndex(mPrefs, mContext.getResources());
        update();
    }

    public String getCombo() {
        return mCombosAsList.get(mIndex);
    }

    public String getLanguage() {
        return mLanguage;
    }

    public ComponentName getService() {
        return mRecognizerComponentName;
    }

    private void update() {
        String language = null;
        String[] splits = TextUtils.split(getCombo(), ";");

        mRecognizerComponentName = ComponentName.unflattenFromString(splits[0]);
        if (splits.length > 1) {
            language = splits[1];
        }

        // If the stored combo name does not refer to an existing service on the device then we use
        // the default service. This can happen if services get removed or renamed.
        if (mRecognizerComponentName == null || !IntentUtils.isRecognitionAvailable(mContext, mRecognizerComponentName)) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        } else {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext, mRecognizerComponentName);
        }

        // TODO: support other actions
        mIntent = Utils.getRecognizerIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, mCallerInfo, language);
        mLanguage = language;
    }


    /**
     * If called with mIndex == -1 then mIndex is set to 0
     */
    private void incIndex(SharedPreferences prefs, Resources res) {
        if (++mIndex >= mCombosAsList.size()) {
            mIndex = 0;
        }
        PreferenceUtils.putPrefString(prefs, res, mKeyImeCurrentCombo, mCombosAsList.get(mIndex));
    }
}