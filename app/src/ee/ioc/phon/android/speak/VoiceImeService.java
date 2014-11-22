package ee.ioc.phon.android.speak;

import android.inputmethodservice.InputMethodService;
import android.view.View;
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
        mInputView.setListener(new VoiceImeView.VoiceImeViewListener() {

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
        });
        return mInputView;
    }

    /**
     * TODO: send the input type to the server so that a relevant LM could be picked
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.i("onStartInput");
        super.onStartInput(attribute, restarting);
        Log.i("imeOptions: " + attribute.imeOptions);
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
            mInputView.closing();
        }
    }
}