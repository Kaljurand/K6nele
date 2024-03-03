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
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;


public class ServiceLanguageChooser {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<String> mCombosAsList;
    private final CallerInfo mCallerInfo;
    private final String mAppId;
    private final int mKeyCurrentCombo;
    private int mIndex;
    private Intent mIntent;
    private String mLanguage = null;
    private ComponentName mRecognizerComponentName = null;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, int keys, CallerInfo callerInfo, String appId) {

        mContext = context;
        mPrefs = prefs;
        mCallerInfo = callerInfo;
        mAppId = appId;

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

            String currentCombo = PreferenceUtils.getPrefMapEntry(prefs, res, mKeyCurrentCombo, mAppId);
            mIndex = mCombosAsList.indexOf(currentCombo);
            // If the current combo was not found among the choices then select the first combo.
            if (mIndex == -1) {
                mIndex = 0;
                PreferenceUtils.putPrefMapEntry(prefs, res, mKeyCurrentCombo, mAppId, mCombosAsList.get(0));
            }
        } else {
            mCombosAsList = Collections.singletonList(comboOverride);
            mIndex = 0;
            mKeyCurrentCombo = -1;
        }
        update();
    }

    /**
     * Note that if the stored recognizer component name does not refer to an existing service on the device,
     * because it has been removed in the mean time, then SpeechRecognizer returns error 10
     * ("Bind to system recognition service failed with error 10").
     */
    public SpeechRecognizer getSpeechRecognizer() {
        return SpeechRecognizer.createSpeechRecognizer(mContext, mRecognizerComponentName);
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
            PreferenceUtils.putPrefMapEntry(mPrefs, mContext.getResources(), mKeyCurrentCombo, mAppId, mCombosAsList.get(mIndex));
        }
        update();
    }

    public String get(int position) {
        return mCombosAsList.get(position);
    }

    public boolean isSelected(int position) {
        return mIndex == position;
    }

    public void set(int position) {
        if (size() > 1) {
            if (position >= size()) {
                mIndex = 0;
            } else {
                mIndex = position;
            }
            PreferenceUtils.putPrefMapEntry(mPrefs, mContext.getResources(), mKeyCurrentCombo, mAppId, mCombosAsList.get(mIndex));
        }
        update();
    }

    public String getCombo() {
        return mCombosAsList.get(mIndex);
    }

    // TODO: can return null, but some callers expect non-null
    public String getLanguage() {
        return mLanguage;
    }

    // TODO: can return null, but some callers expect non-null
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

        // TODO: support other actions
        mIntent = Utils.getRecognizerIntent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, mCallerInfo, language);
        mLanguage = language;
    }
}