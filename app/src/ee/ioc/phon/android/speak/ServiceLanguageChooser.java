package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.utils.IntentUtils;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

/**
 * TODO: cleanup
 */
public class ServiceLanguageChooser {

    private final Context mContext;
    private final String mPackageName;
    private final SharedPreferences mPrefs;
    private final List<String> mCombosAsList;
    private final EditorInfo mAttribute;
    private final int mKeyImeCurrentCombo;
    private int mIndex;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mIntent;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, EditorInfo attribute, int keys, String packageName) {

        mContext = context;
        mPackageName = packageName;
        mPrefs = prefs;
        mAttribute = attribute;

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

    private void update() {
        String language = null;
        String[] splits = TextUtils.split(getCombo(), ";");

        ComponentName recognizerComponentName = ComponentName.unflattenFromString(splits[0]);
        if (splits.length > 1) {
            language = splits[1];
        }

        if (recognizerComponentName == null) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        } else {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext, recognizerComponentName);
        }

        mIntent = IntentUtils.getRecognizerIntent(mPackageName, mAttribute, language);
    }


    public String getCombo() {
        return mCombosAsList.get(mIndex);
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