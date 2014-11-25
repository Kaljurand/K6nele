package ee.ioc.phon.android.speak;

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

public class VoiceImeService extends InputMethodService {

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
     * TODO: send the input type to the server so that a relevant LM could be picked
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.i("onStartInput");
        super.onStartInput(attribute, restarting);
        Log.i("onStartInput: inputType: " + attribute.inputType);
        Log.i("onStartInput: imeOptions: " + attribute.imeOptions);

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                break;
            case InputType.TYPE_CLASS_DATETIME:
                break;
            case InputType.TYPE_CLASS_PHONE:
                break;
            case InputType.TYPE_CLASS_TEXT:
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    Log.i("onStartInput: password: " + variation);
                    handleLanguageSwitch();
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
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
                    handleLanguageSwitch();
                }

                @Override
                public void onGo() {
                    keyDownUp(KeyEvent.KEYCODE_ENTER);
                    handleClose();
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

    private void commitTyped(InputConnection inputConnection, String str, int prevLength) {
        if (inputConnection != null && str != null && str.length() > 0) {
            if (prevLength > 0) {
                inputConnection.deleteSurroundingText(prevLength, 0);
            }
            inputConnection.commitText(str, 1);
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
    private void handleLanguageSwitch() {
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