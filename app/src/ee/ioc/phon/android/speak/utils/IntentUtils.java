package ee.ioc.phon.android.speak.utils;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.SpannableString;
import android.util.SparseIntArray;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speechutils.Extras;

public final class IntentUtils {

    private IntentUtils() {
    }

    /**
     * @return table that maps SpeechRecognizer error codes to RecognizerIntent error codes
     */
    public static SparseIntArray createErrorCodesServiceToIntent() {
        SparseIntArray errorCodes = new SparseIntArray();
        errorCodes.put(SpeechRecognizer.ERROR_AUDIO, RecognizerIntent.RESULT_AUDIO_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_CLIENT, RecognizerIntent.RESULT_CLIENT_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, RecognizerIntent.RESULT_CLIENT_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_NETWORK, RecognizerIntent.RESULT_NETWORK_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_NETWORK_TIMEOUT, RecognizerIntent.RESULT_NETWORK_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_NO_MATCH, RecognizerIntent.RESULT_NO_MATCH);
        errorCodes.put(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, RecognizerIntent.RESULT_SERVER_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_SERVER, RecognizerIntent.RESULT_SERVER_ERROR);
        errorCodes.put(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, RecognizerIntent.RESULT_NO_MATCH);
        return errorCodes;
    }

    public static PendingIntent getPendingIntent(Bundle extras) {
        Parcelable extraResultsPendingIntentAsParceable = extras.getParcelable(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT);
        if (extraResultsPendingIntentAsParceable != null) {
            //PendingIntent.readPendingIntentOrNullFromParcel(mExtraResultsPendingIntent);
            if (extraResultsPendingIntentAsParceable instanceof PendingIntent) {
                return (PendingIntent) extraResultsPendingIntentAsParceable;
            }
        }
        return null;
    }

    public static Intent getAppIntent(Context c, String packageName) {
        PackageManager pm = c.getPackageManager();
        return pm.getLaunchIntentForPackage(packageName);
    }

    /**
     * Constructs a list of search intents.
     * The first one that can be handled by the device is launched.
     * In split-screen mode, launch the activity into the other screen. Test this by:
     * 1. Launch Kõnele, 2. Start split-screen, 3. Press Kõnele mic button and speak,
     * 4. The results should be loaded into the other window.
     *
     * @param context context
     * @param query   search query
     */
    public static void startSearchActivity(Context context, CharSequence query) {
        try {
            startActivityIfAvailable(context, parseIntentQuery(query));
        } catch (JSONException e) {
            Log.i("startSearchActivity: JSON: " + e.getMessage());
            // TODO: how to pass the search query to ACTION_ASSIST
            // TODO: maybe use SearchManager instead
            //Intent intent0 = new Intent(Intent.ACTION_ASSIST);
            //intent0.putExtra(Intent.EXTRA_ASSIST_CONTEXT, new Bundle());
            //intent0.putExtra(SearchManager.QUERY, query);
            //intent0.putExtra(Intent.EXTRA_ASSIST_INPUT_HINT_KEYBOARD, false);
            //intent0.putExtra(Intent.EXTRA_ASSIST_PACKAGE, context.getPackageName());
            startActivityIfAvailable(context,
                    getSearchIntent(Intent.ACTION_WEB_SEARCH, query),
                    getSearchIntent(Intent.ACTION_SEARCH, query));
        }
    }

    public static boolean startActivityIfAvailable(Context context, Intent... intents) {
        try {
            for (Intent intent : intents) {
                if (isActivityAvailable(context, intent)) {
                    context.startActivity(intent);
                    return true;
                } else {
                    Log.i("startActivityIfAvailable: not available: " + intent);
                }
            }
            Toast.makeText(context, R.string.errorFailedLaunchIntent, Toast.LENGTH_LONG).show();
        } catch (SecurityException e) {
            // This happens if the user constructs an intent for which we do not have a
            // permission, e.g. the SET_ALARM intent.
            Log.i("startActivityIfAvailable: " + e.getMessage());
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public static Intent getRecognizerIntent(String action, CallerInfo callerInfo, String language) {
        Intent intent = new Intent(action);
        intent.putExtras(callerInfo.getExtras());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, callerInfo.getPackageName());
        if (callerInfo.getEditorInfo() != null) {
            intent.putExtra(Extras.EXTRA_EDITOR_INFO, toBundle(callerInfo.getEditorInfo()));
        }
        // Declaring that in the IME we would like to allow longer pauses (2 sec).
        // The service might not implement these (e.g. Kõnele currently does not)
        // TODO: what is the difference of these two constants?
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000);

        if (language != null) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            // TODO: make this configurable
            intent.putExtra(Extras.EXTRA_ADDITIONAL_LANGUAGES, new String[]{});
        }
        return intent;
    }

    /**
     * Checks whether a speech recognition service is available on the system. If this method
     * returns {@code false}, {@link SpeechRecognizer#createSpeechRecognizer(Context, ComponentName)}
     * will fail.
     * Similar to {@link SpeechRecognizer#isRecognitionAvailable(Context)} but supports
     * restricting the intent query by component name.
     * <p/>
     * TODO: propose to add this to SpeechRecognizer
     * TODO: clarify what does "will fail" mean
     *
     * @param context       with which {@code SpeechRecognizer} will be created
     * @param componentName of the recognition service
     * @return {@code true} if recognition is available, {@code false} otherwise
     */
    public static boolean isRecognitionAvailable(final Context context, ComponentName componentName) {
        Intent intent = new Intent(RecognitionService.SERVICE_INTERFACE);
        intent.setComponent(componentName);
        final List<ResolveInfo> list = context.getPackageManager().queryIntentServices(intent, 0);
        return list != null && list.size() != 0;
    }


    private static Bundle toBundle(EditorInfo attribute) {
        Bundle bundle = new Bundle();
        bundle.putBundle("extras", attribute.extras);
        bundle.putInt("inputType", attribute.inputType);
        bundle.putInt("initialSelStart", attribute.initialSelStart);
        bundle.putInt("initialSelEnd", attribute.initialSelEnd);
        bundle.putString("actionLabel", asString(attribute.actionLabel));
        bundle.putString("fieldName", asString(attribute.fieldName));
        bundle.putString("hintText", asString(attribute.hintText));
        bundle.putString("label", asString(attribute.label));
        // This line gets the actual caller package registered in the package registry.
        // The key needs to be "packageName".
        bundle.putString("packageName", asString(attribute.packageName));
        return bundle;
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

    private static boolean isActivityAvailable(Context context, Intent intent) {
        final PackageManager mgr = context.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private static Intent getSearchIntent(String action, CharSequence query) {
        Intent intent = new Intent(action);
        intent.putExtra(SearchManager.QUERY, query);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }
        return intent;
    }

    /**
     * TODO: support: array extras, broadcast intent, voice interaction lauch mode, etc.
     *
     * @param query Intent serialized as JSON
     * @return Deserialized intent
     * @throws JSONException if parsing fails
     */
    private static Intent parseIntentQuery(CharSequence query) throws JSONException {
        Log.i("parseIntentQuery: " + query);
        JSONObject json = new JSONObject(query.toString());
        Intent intent = new Intent();
        String action = json.optString("action", null);
        if (action != null) {
            intent.setAction(action);
        }
        String component = json.optString("component", null);
        if (component != null) {
            intent.setComponent(ComponentName.unflattenFromString(component));
        }
        String packageName = json.optString("package", null);
        if (component != null) {
            intent.setPackage(packageName);
        }
        String data = json.optString("data", null);
        if (data != null) {
            intent.setData(Uri.parse(data));
        }
        JSONObject extras = json.optJSONObject("extras");
        if (extras != null) {
            Iterator<String> iter = extras.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object val = extras.get(key);
                if (val instanceof Long) {
                    intent.putExtra(key, (Long) val);
                } else if (val instanceof Integer) {
                    intent.putExtra(key, (Integer) val);
                } else if (val instanceof Boolean) {
                    intent.putExtra(key, (Boolean) val);
                } else if (val instanceof Double) {
                    intent.putExtra(key, (Double) val);
                } else if (val instanceof Float) {
                    intent.putExtra(key, (Float) val);
                } else if (val instanceof String) {
                    intent.putExtra(key, (String) val);
                }
            }
        }
        JSONArray categories = json.optJSONArray("categories");
        if (categories != null) {
            int length = categories.length();
            for (int i = 0; i < length; i++) {
                intent.addCategory(categories.getString(i));
            }
        }
        JSONArray flags = json.optJSONArray("flags");
        if (flags != null) {
            int length = flags.length();
            for (int i = 0; i < length; i++) {
                intent.addFlags(flags.getInt(i));
            }
        }

        Log.i("parseIntentQuery: " + intent);
        Log.i("parseIntentQuery: " + Utils.ppBundle(intent.getExtras()));
        return intent;
    }
}