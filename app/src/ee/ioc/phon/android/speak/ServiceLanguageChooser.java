package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private final Set<String> mCombos;
    private final List<String> mCombosAsList;
    private final EditorInfo mAttribute;
    private int mIndex = 0;
    private SpeechRecognizer mSpeechRecognizer;
    private Intent mIntent;

    public ServiceLanguageChooser(Context context, SharedPreferences prefs, EditorInfo attribute) {

        mContext = context;
        mAttribute = attribute;
        mCombos =
                PreferenceUtils.getPrefStringSet(prefs, context.getResources(), R.string.keyImeRecognitionServiceLanguage);
        mCombosAsList = new ArrayList<>(mCombos);
    }

    public SpeechRecognizer getSpeechRecognizer() {
        return mSpeechRecognizer;
    }

    public Intent getIntent() {
        return mIntent;
    }


    public String getLabel() {
        String recognizer = "UNDEF";
        String language = "UNDEF";
        String[] splits = getCombo();
        if (splits.length > 0) {
            recognizer = splits[0];
        }
        if (splits.length > 1) {
            language = splits[1];
        }
        return language + "@" + recognizer;
    }

    private String[] getCombo() {
        String selectedCombo;

        if (mCombosAsList == null && mCombosAsList.isEmpty()) {
            selectedCombo = mContext.getResources().getString(R.string.defaultImeRecognizerServiceLanguage);
        } else {
            selectedCombo = mCombosAsList.get(mIndex);
        }
        return TextUtils.split(selectedCombo, ";");
    }

    public void next() {
        if (++mIndex >= mCombosAsList.size()) {
            mIndex = 0;
        }

        String language = null;
        String[] splits = getCombo();

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
}