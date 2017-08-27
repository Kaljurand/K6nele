package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;


public class ServiceLanguageChooser {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<String> mCombosAsList;
    private final CallerInfo mCallerInfo;
    private final int mKeyCurrentCombo;
    private int mIndex;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mIntent;
    private String mLanguage = null;
    private ComponentName mRecognizerComponentName = null;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, int keys, CallerInfo callerInfo) {

        mContext = context;
        mPrefs = prefs;
        mCallerInfo = callerInfo;

        // If SERVICE_COMPONENT is defined, we do not use the combos selected in the settings.
        String comboOverride = null;
        Bundle extras = callerInfo.getExtras();
        if (extras.containsKey(Extras.EXTRA_SERVICE_COMPONENT)) {
            comboOverride = extras.getString(Extras.EXTRA_SERVICE_COMPONENT);
            if (extras.containsKey(RecognizerIntent.EXTRA_LANGUAGE)) {
                comboOverride = RecognitionServiceManager.createComboString(comboOverride, extras.getString(RecognizerIntent.EXTRA_LANGUAGE));
            }
        }

        if (comboOverride == null) {
            Resources res = context.getResources();
            TypedArray keysAsTypedArray = res.obtainTypedArray(keys);
            int keyCombo = keysAsTypedArray.getResourceId(0, 0);
            mKeyCurrentCombo = keysAsTypedArray.getResourceId(1, 0);
            int defaultCombos = keysAsTypedArray.getResourceId(2, 0);
            keysAsTypedArray.recycle();

            Set<String> mCombos = PreferenceUtils.getPrefStringSet(prefs, res, keyCombo);

            if (mCombos == null || mCombos.isEmpty()) {
                // If the user has chosen an empty set of combos
                mCombosAsList = PreferenceUtils.getStringListFromStringArray(res, defaultCombos);
            } else {
                mCombosAsList = new ArrayList<>(mCombos);
            }

            String currentCombo = PreferenceUtils.getPrefString(prefs, res, mKeyCurrentCombo);
            mIndex = mCombosAsList.indexOf(currentCombo);
            // If the current combo was not found among the choices then select the first combo.
            if (mIndex == -1) {
                mIndex = 0;
                PreferenceUtils.putPrefString(prefs, res, mKeyCurrentCombo, mCombosAsList.get(0));
            }
        } else {
            mCombosAsList = Collections.singletonList(comboOverride);
            mIndex = 0;
            mKeyCurrentCombo = -1;
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

    /**
     * Switch to the "next" combo and set it as default.
     * Only done if there are more than 1 combos, meaning that defining SERVICE_COMPONENT (which
     * creates a single-element combo list) does not change the default combo.
     */
    public void next() {
        if (size() > 1) {
            if (++mIndex >= size()) {
                mIndex = 0;
            }
            PreferenceUtils.putPrefString(mPrefs, mContext.getResources(), mKeyCurrentCombo, mCombosAsList.get(mIndex));
        }
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
}