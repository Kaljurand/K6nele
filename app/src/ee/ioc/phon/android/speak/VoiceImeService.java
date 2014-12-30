package ee.ioc.phon.android.speak;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.SpannableString;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceImeService extends InputMethodService {

    // Maximum number of characters that left-swipe is willing to delete
    private static final int MAX_DELETABLE_CONTEXT = 100;
    private static final Pattern WHITESPACE = Pattern.compile("\\s{1,}");

    private InputMethodManager mInputMethodManager;

    private VoiceImeView mInputView;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("onCreate");
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }

    /**
     * This is called at configuration change. We just kill the running session.
     * TODO: better handle configuration changes
     */
    @Override
    public void onInitializeInterface() {
        Log.i("onInitializeInterface");
        closeInputView();
    }

    @Override
    public View onCreateInputView() {
        Log.i("onCreateInputView");
        mInputView = (VoiceImeView) getLayoutInflater().inflate(R.layout.voice_ime_view, null);
        return mInputView;
    }

    /**
     * We check the type of editor control and if we probably cannot handle it (email addresses,
     * dates) or do not want to (passwords) then we hand the editing over to an other keyboard.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        Log.i("onStartInput: " + attribute.inputType + "/" + attribute.imeOptions + "/" + restarting);

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                Log.i("onStartInput: NUMBER");
                break;
            case InputType.TYPE_CLASS_DATETIME:
                Log.i("onStartInput: DATETIME");
                switchIme(false);
                break;
            case InputType.TYPE_CLASS_PHONE:
                Log.i("onStartInput: PHONE");
                switchIme(false);
                break;
            case InputType.TYPE_CLASS_TEXT:
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                Log.i("onStartInput: TEXT, variation: " + variation);
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    Log.i("onStartInput: PASSWORD || VISIBLE_PASSWORD");
                    // We refuse to recognize passwords for privacy reasons.
                    switchIme(false);
                } else if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    Log.i("onStartInput: EMAIL_ADDRESS");
                    switchIme(false);
                } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                    Log.i("onStartInput: URI");
                    // URI bar of Chrome and Firefox, can also handle search queries, thus supported
                } else if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    Log.i("onStartInput: FILTER");
                    // List filtering? Used in the Dialer search bar, thus supported
                }

                // This is used in the standard search bar (e.g. in Google Play).
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    Log.i("onStartInput: FLAG_AUTO_COMPLETE");
                }
                break;

            default:
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        Log.i("onFinishInput");
        closeInputView();
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        Log.i("onStartInputView: " + attribute.inputType + "/" + attribute.imeOptions + "/" + restarting);

        closeInputView();

        if (restarting) {
            return;
        }

        mInputView.setListener(getRecognizerIntent(getApplicationContext(), attribute), new VoiceImeView.VoiceImeViewListener() {

            int mPrevLength = 0;

            @Override
            public void onPartialResult(String text) {
                commitTyped(getCurrentInputConnection(), text, mPrevLength);
                mPrevLength = text.length();
            }

            @Override
            public void onFinalResult(String text) {
                int lastIndex = text.length() - 1;
                if (lastIndex != -1) {
                    if (text.charAt(lastIndex) != '\n' && text.charAt(lastIndex) != ' ') {
                        text += " ";
                    }
                    commitTyped(getCurrentInputConnection(), text, mPrevLength);
                }
                mPrevLength = 0;
            }

            @Override
            public void onSwitchIme(boolean isAskUser) {
                switchIme(isAskUser);
            }

            @Override
            public void onGo() {
                performGo();
                handleClose();
            }

            @Override
            public void onDeleteLastWord() {
                handleDelete(getCurrentInputConnection());
            }

            @Override
            public void onAddNewline() {
                commitTyped(getCurrentInputConnection(), "\n", 0);
            }

            @Override
            public void onAddSpace() {
                commitTyped(getCurrentInputConnection(), " ", 0);
            }
        });

        // Launch recognition immediately (if set so)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (Utils.getPrefBoolean(prefs, getResources(), R.string.keyImeAutoStart, R.bool.defaultImeAutoStart)) {
            Log.i("Auto-starting");
            mInputView.start();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        Log.i("onFinishInputView: " + finishingInput);
        closeInputView();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Log.i("onCurrentInputMethodSubtypeChanged");
        closeInputView();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        Log.i("onUpdateSelection");
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
    }

    private static void commitTyped(InputConnection inputConnection, String str, int prevLength) {
        if (inputConnection != null && str != null && str.length() > 0) {
            if (prevLength > 0) {
                inputConnection.deleteSurroundingText(prevLength, 0);
            }
            inputConnection.commitText(str, 1);
        }
    }

    /**
     * Deletes all characters up to the leftmost whitespace from the cursor (including the whitespace)
     * TODO: maybe expensive?
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private static void handleDelete(InputConnection inputConnection) {
        if (inputConnection != null) {
            // If something is selected then delete the selection and return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                    && inputConnection.getSelectedText(0) != null) {
                inputConnection.commitText("", 0);
            } else {
                CharSequence beforeCursor = inputConnection.getTextBeforeCursor(MAX_DELETABLE_CONTEXT, 0);
                if (beforeCursor != null) {
                    int beforeCursorLength = beforeCursor.length();
                    Matcher m = WHITESPACE.matcher(beforeCursor);
                    int lastIndex = 0;
                    while (m.find()) {
                        lastIndex = m.start();
                    }
                    if (lastIndex > 0) {
                        inputConnection.deleteSurroundingText(beforeCursorLength - lastIndex, 0);
                    } else if (beforeCursorLength < MAX_DELETABLE_CONTEXT) {
                        inputConnection.deleteSurroundingText(beforeCursorLength, 0);
                    }
                }
            }
        }
    }

    private void closeInputView() {
        if (mInputView != null) {
            mInputView.closeSession();
        }
    }

    private void handleClose() {
        requestHideSelf(0);
        closeInputView();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    /**
     * Switching to another IME (ideally the previous one). Either when the user tries to edit
     * a password, or explicitly presses the "switch IME" button.
     * TODO: not sure it is the best way to do it
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void switchIme(boolean isAskUser) {
        closeInputView();
        if (isAskUser) {
            mInputMethodManager.showInputMethodPicker();
        } else {
            final IBinder token = getToken();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mInputMethodManager.switchToNextInputMethod(getToken(), false /* not onlyCurrentIme */);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    mInputMethodManager.switchToLastInputMethod(token);
                } else {
                    mInputMethodManager.showInputMethodPicker();
                }
            } catch (NoSuchMethodError e) {
                Log.e("IME switch failed", e);
            }
        }
    }


    /**
     * Performs the Go-action, e.g. to launch search on a searchbar.
     */
    private void performGo() {
        // Does not work on Google Searchbar
        // getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_DONE);

        // Works in Google Searchbar, GF Translator
        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_GO);
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

    private static Intent getRecognizerIntent(Context context, EditorInfo attribute) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(Extras.EXTRA_UNLIMITED_DURATION, true);
        intent.putExtra(Extras.EXTRA_EDITOR_INFO, toBundle(attribute));
        return intent;
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