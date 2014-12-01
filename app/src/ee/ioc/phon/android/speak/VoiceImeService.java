package ee.ioc.phon.android.speak;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.text.InputType;
import android.view.KeyEvent;
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
        Log.i("onCreate");
        super.onCreate();
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    }

    @Override
    public void onInitializeInterface() {
        Log.i("onInitializeInterface");
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
        Log.i("onStartInput");
        super.onStartInput(attribute, restarting);
        Log.i("onStartInput: inputType: " + attribute.inputType);
        Log.i("onStartInput: imeOptions: " + attribute.imeOptions);

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                Log.i("onStartInput: NUMBER");
                break;
            case InputType.TYPE_CLASS_DATETIME:
                Log.i("onStartInput: DATETIME");
                switchIme();
                break;
            case InputType.TYPE_CLASS_PHONE:
                Log.i("onStartInput: PHONE");
                switchIme();
                break;
            case InputType.TYPE_CLASS_TEXT:
                Log.i("onStartInput: TEXT");
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    Log.i("onStartInput: password: " + variation);
                    // We refuse to recognize passwords for privacy reasons.
                    switchIme();
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                    Log.i("onStartInput: EMAIL_ADDRESS");
                    switchIme();
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
        Log.i("onFinishInput");
        super.onFinishInput();
        closeInputView();
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.i("onStartInputView");
        super.onStartInputView(attribute, restarting);
        closeInputView();

        if (mInputView != null) {
            mInputView.setListener(attribute, new VoiceImeView.VoiceImeViewListener() {

                int mPrevLength = 0;

                @Override
                public void onPartialResult(String text) {
                    commitTyped(getCurrentInputConnection(), text, mPrevLength);
                    mPrevLength = text.length();
                }

                @Override
                public void onFinalResult(String text) {
                    commitTyped(getCurrentInputConnection(), text + " ", mPrevLength);
                    mPrevLength = 0;
                }

                @Override
                public void onKeyboard() {
                    switchIme();
                }

                @Override
                public void onGo() {
                    keyDownUp(KeyEvent.KEYCODE_ENTER);
                    handleClose();
                }

                @Override
                public void deleteLastWord() {
                    handleDelete(getCurrentInputConnection());
                }
            });
        }
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
        mInputView.closeSession();
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
     * Switching to another IME (the previous one). Either when the user tries to edit
     * a password, or explicitly presses the "switch keyboard" button.
     * TODO: not sure it is the best way to do it
     */
    private void switchIme() {
        closeInputView();
        final IBinder token = getToken();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mInputMethodManager.switchToNextInputMethod(getToken(), false /* not onlyCurrentIme */);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mInputMethodManager.switchToLastInputMethod(token);
            } else {
                handleClose();
            }
        } catch (NoSuchMethodError e) {
            Log.e("cannot set the previous input method:");
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }


}