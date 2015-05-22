package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.utils.PreferenceUtils;

/**
 * TODO: cleanup
 */
public class ServiceLanguageChooser {

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final List<String> mCombosAsList;
    private final EditorInfo mAttribute;
    private int mIndex;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mIntent;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, EditorInfo attribute) {

        mContext = context;
        mPrefs = prefs;
        mAttribute = attribute;

        Resources res = context.getResources();
        Set<String> mCombos = PreferenceUtils.getPrefStringSet(prefs, res, R.string.keyImeRecognitionServiceLanguage, R.array.defaultImeRecognizerServiceLanguage);

        if (mCombos.isEmpty()) {
            // If the user has chosen an empty set of combos
            mCombosAsList = PreferenceUtils.getStringListFromStringArray(res, R.array.defaultImeRecognizerServiceLanguage);
        } else {
            mCombosAsList = new ArrayList<>(mCombos);
        }

        String currentCombo = PreferenceUtils.getPrefString(prefs, res, R.string.keyCurrentCombo);
        Log.i("GET: " + currentCombo);
        mIndex = mCombosAsList.indexOf(currentCombo);
        // If the current combo was not found among the choices then select the first combo.
        if (mIndex == -1) {
            Log.i("NOT FOUND");
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

        mIntent = getRecognizerIntent(mContext, mAttribute, language);
    }


    private static Intent getRecognizerIntent(Context context, EditorInfo attribute, String language) {
        // TODO: try with another action, or without an action
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(Extras.EXTRA_UNLIMITED_DURATION, true);
        intent.putExtra(Extras.EXTRA_EDITOR_INFO, toBundle(attribute));
        // Declaring that in the IME we would like to allow longer pauses (2 sec).
        // The service might not implement these (e.g. Kõnele currently does not)
        // TODO: what is the difference of these two constants?
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        if (language != null) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            // TODO: make this configurable
            intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{});
        }
        return intent;
    }


    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof SpannableString) {
            SpannableString ss = (SpannableString) o;
            return ss.subSequence(0, ss.length()).toString();
        }
        return o.toString();
    }


    private static Bundle toBundle(EditorInfo attribute) {
        Bundle bundle = new Bundle();
        bundle.putBundle("extras", attribute.extras);
        bundle.putString("actionLabel", asString(attribute.actionLabel));
        bundle.putString("fieldName", asString(attribute.fieldName));
        bundle.putString("hintText", asString(attribute.hintText));
        bundle.putString("inputType", String.valueOf(attribute.inputType));
        bundle.putString("label", asString(attribute.label));
        // This line gets the actual caller package registered in the package registry.
        // The key needs to be "packageName".
        bundle.putString("packageName", asString(attribute.packageName));
        return bundle;
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
        Log.i("PUT: " + mCombosAsList.get(mIndex));
        PreferenceUtils.putPrefString(prefs, res, R.string.keyCurrentCombo, mCombosAsList.get(mIndex));
    }
}