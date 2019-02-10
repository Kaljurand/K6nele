package ee.ioc.phon.android.speak.service;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.activity.PermissionsRequesterActivity;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speak.view.AbstractSpeechInputViewListener;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.CommandEditor;
import ee.ioc.phon.android.speechutils.editor.CommandEditorResult;
import ee.ioc.phon.android.speechutils.editor.InputConnectionCommandEditor;
import ee.ioc.phon.android.speechutils.editor.Op;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

public class SpeechInputMethodService extends InputMethodService {

    private InputMethodManager mInputMethodManager;
    private SpeechInputView mInputView;
    private CommandEditor mCommandEditor;
    private boolean mShowPartialResults;
    private SharedPreferences mPrefs;
    private Resources mRes;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("onCreate");
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mCommandEditor = new InputConnectionCommandEditor(getApplicationContext());
    }

    /**
     * This is called at configuration change. We just kill the running session.
     * TODO: better handle configuration changes
     */
    @Override
    public void onInitializeInterface() {
        Log.i("onInitializeInterface");
        closeSession();
    }

    @Override
    public View onCreateInputView() {
        Log.i("onCreateInputView");
        mInputView = (SpeechInputView) getLayoutInflater().inflate(R.layout.voice_ime_view, null, false);
        return mInputView;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        // Returning true only if Wear
        return getResources().getBoolean(R.bool.isWatch);
    }

    /**
     * We check the type of editor control and if we probably cannot handle it (e.g. dates)
     * or do not want to (e.g. passwords) then we hand the editing over to another keyboard.
     * TODO: handle inputType = 0
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        String type = "UNKNOWN";

        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                type = "NUMBER";
                break;
            case InputType.TYPE_CLASS_DATETIME:
                type = "DATETIME";
                switchToLastIme();
                break;
            case InputType.TYPE_CLASS_PHONE:
                type = "PHONE";
                break;
            case InputType.TYPE_CLASS_TEXT:
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                type = "TEXT/";
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // We refuse to recognize passwords for privacy reasons.
                    type += "PASSWORD || VISIBLE_PASSWORD";
                    switchToLastIme();
                } else if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    type += "EMAIL_ADDRESS";
                } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                    // URI bar of Chrome and Firefox, can also handle search queries, thus supported
                    type += "URI";
                } else if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // List filtering? Used in the Dialer search bar, thus supported
                    type += "FILTER";
                } else {
                    type += variation;
                }

                // This is used in the standard search bar (e.g. in Google Play).
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    type += "FLAG_AUTO_COMPLETE";
                }
                break;

            default:
        }
        Log.i("onStartInput: " + type + ", " + attribute.inputType + ", " + attribute.imeOptions + ", " + restarting);
    }

    /**
     * Note that when editing a HTML page, then switching between form fields might fail to call
     * this method with restarting=false, we thus always update the editor info (incl. inputType).
     */
    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        Log.i("onStartInputView: " + editorInfo.inputType + "/" + editorInfo.imeOptions + "/" + restarting);

        InputConnection ic = getCurrentInputConnection();
        // InputConnectionCommandEditor cannot be called with a null InputConnection.
        // We do not expect this to happen, but Google Play crash reports show that it does.
        if (ic == null) {
            Toast.makeText(getApplicationContext(), R.string.errorFailedGetCurrentInputConnection, Toast.LENGTH_LONG).show();
            switchToLastIme();
            return;
        }
        ((InputConnectionCommandEditor) mCommandEditor).setInputConnection(ic);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mRes = getResources();
        mInputView.init(
                R.array.keysIme,
                new CallerInfo(makeExtras(mPrefs, mRes), editorInfo, getPackageName()),
                PreferenceUtils.getPrefInt(mPrefs, mRes, R.string.keyImeMode, R.string.defaultImeMode));

        // TODO: update this less often (in onStart)
        closeSession();

        if (restarting) {
            return;
        }

        mInputView.setListener(getSpeechInputViewListener(getMyWindow(), editorInfo.packageName), editorInfo);
        mShowPartialResults = PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyImeShowPartialResults, R.bool.defaultImeShowPartialResults);

        // Launch recognition immediately (if set so)
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyImeAutoStart, R.bool.defaultImeAutoStart)) {
            Log.i("Auto-starting");
            mInputView.start();
        }
    }

    /**
     * Called when the input view is being hidden from the user.
     * This will be called either prior to hiding the window,
     * or prior to switching to another target for editing.
     *
     * @param finishingInput If true, onFinishInput() will be called immediately after.
     */
    @Override
    public void onFinishInputView(boolean finishingInput) {
        // TODO: maybe do not call super
        super.onFinishInputView(finishingInput);
        Log.i("onFinishInputView: " + finishingInput);
        if (!finishingInput) {
            closeSession();
        }
    }

    /**
     * Called to inform the input method that text input has finished in the last editor.
     * At this point there may be a call to onStartInput(EditorInfo, boolean) to perform input in a new editor,
     * or the input method may be left idle.
     * This method is not called when input restarts in the same editor.
     */
    @Override
    public void onFinishInput() {
        // TODO: maybe do not call super
        super.onFinishInput();
        Log.i("onFinishInput");
        closeSession();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Log.i("onCurrentInputMethodSubtypeChanged: " + subtype + ": " + subtype.getExtraValue());
        closeSession();
    }

    private void closeSession() {
        if (mInputView != null) {
            mInputView.cancel();
        }
        Window window = getMyWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private IBinder getToken() {
        Window window = getMyWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private Window getMyWindow() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window;
    }

    /**
     * Switch to another IME by selecting it from the list of all active IMEs (isAskUser==true), or
     * by taking the next IME in the IME rotation (isAskUser==false on JELLY_BEAN).
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void switchIme(boolean isAskUser) {
        closeSession();
        if (isAskUser) {
            mInputMethodManager.showInputMethodPicker();
        } else {
            final IBinder token = getToken();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mInputMethodManager.switchToNextInputMethod(token, false /* not onlyCurrentIme */);
                } else {
                    mInputMethodManager.switchToLastInputMethod(token);
                }
            } catch (NoSuchMethodError e) {
                Log.e("IME switch failed", e);
            }
        }
    }

    /**
     * Switch to the previous IME, either when the user tries to edit an unsupported field (e.g. password),
     * or when they explicitly want to be taken back to the previous IME e.g. in case of a one-shot
     * speech input.
     */
    private void switchToLastIme() {
        closeSession();
        mInputMethodManager.switchToLastInputMethod(getToken());
    }

    private static String getText(@NonNull List<String> results) {
        if (results.size() > 0) {
            return results.get(0);
        }
        return "";
    }

    private static Bundle makeExtras(SharedPreferences prefs, Resources res) {
        Bundle extras = new Bundle();
        boolean isUnlimitedDuration = !PreferenceUtils.getPrefBoolean(prefs, res,
                R.string.keyImeAutoStopAfterPause, R.bool.defaultImeAutoStopAfterPause);
        extras.putBoolean(Extras.EXTRA_UNLIMITED_DURATION, isUnlimitedDuration);
        extras.putBoolean(Extras.EXTRA_DICTATION_MODE, isUnlimitedDuration);
        return extras;
    }

    private SpeechInputView.SpeechInputViewListener getSpeechInputViewListener(final Window window, final String packageName) {
        return new AbstractSpeechInputViewListener() {

            // TODO: quick hack to add app to the matcher, not sure if we can access the
            // class name of the app
            private ComponentName app = new ComponentName(packageName, packageName);

            private void runOp(Op op) {
                mCommandEditor.runOp(op, false);
            }

            private void setKeepScreenOn(boolean b) {
                if (window != null) {
                    if (b) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
            }

            @Override
            public void onComboChange(String language, ComponentName service) {
                // TODO: name of the rewrites table configurable
                mCommandEditor.setRewriters(Utils.makeList(Utils.genRewriters(mPrefs, mRes, null, language, service, app)));
            }

            @Override
            public void onPartialResult(List<String> results, boolean isSemiFinal) {
                if (isSemiFinal) {
                    CommandEditorResult editorResult = mCommandEditor.commitFinalResult(getText(results));
                    if (editorResult != null && mInputView != null && editorResult.isCommand()) {
                        mInputView.showMessage(editorResult.ppCommand(), editorResult.isSuccess());
                    }
                } else {
                    if (mShowPartialResults) {
                        mCommandEditor.commitPartialResult(getText(results));
                    }
                }
            }

            @Override
            public void onFinalResult(List<String> results, Bundle bundle) {
                CommandEditorResult editorResult = mCommandEditor.commitFinalResult(getText(results));
                if (editorResult != null && mInputView != null && editorResult.isCommand()) {
                    mInputView.showMessage(editorResult.ppCommand(), editorResult.isSuccess());
                }
                setKeepScreenOn(false);
            }

            @Override
            public void onCommand(String text) {
                Op op = mCommandEditor.getOpOrNull(text, false);
                if (op != null) {
                    boolean success = mCommandEditor.runOp(op);
                    if (mInputView != null) {
                        // TODO: show executed command or replacement text, not op.toString()
                        mInputView.showMessage(op.toString(), success);
                    }
                    setKeepScreenOn(false);
                }
            }

            @Override
            public void onSwitchIme(boolean isAskUser) {
                switchIme(isAskUser);
            }

            @Override
            public void onSwitchToLastIme() {
                switchToLastIme();
            }

            @Override
            public void onAction(int editorAction, boolean hide) {
                if (hide) {
                    closeSession();
                }
                runOp(mCommandEditor.imeAction(editorAction));
                if (hide) {
                    requestHideSelf(0);
                }
            }

            @Override
            public void onDeleteLeftChar() {
                // TODO: indicate somehow (e.g. vibration, different background color) that the Op failed
                runOp(mCommandEditor.deleteChars(-1));
                // TODO: might be better, i.e. able to delete non-text (checkboxes), but not undoable
                //runOp(mCommandEditor.keyCode(KeyEvent.KEYCODE_DEL));
            }

            @Override
            public void onDeleteLastWord() {
                runOp(mCommandEditor.deleteLeftWord());
            }

            @Override
            public void onAddNewline() {
                runOp(mCommandEditor.replaceSel("\n"));
            }

            @Override
            public void goUp() {
                runOp(mCommandEditor.keyUp());
            }

            @Override
            public void goDown() {
                runOp(mCommandEditor.keyDown());
            }

            @Override
            public void moveRel(int numOfChars) {
                runOp(mCommandEditor.moveRel(numOfChars));
            }

            @Override
            public void moveRelSel(int numOfChars, int type) {
                runOp(mCommandEditor.moveRelSel(numOfChars, type));
            }

            @Override
            public void onExtendSel(String regex) {
                runOp(mCommandEditor.selectRe(regex, false));
            }

            @Override
            public void onAddSpace() {
                runOp(mCommandEditor.replaceSel(" "));
            }

            @Override
            public void onSelectAll() {
                // TODO: show ContextMenu
                runOp(mCommandEditor.selectAll());
            }

            @Override
            public void onReset() {
                // TODO: hide ContextMenu (if visible)
                runOp(mCommandEditor.moveRel(0));
            }

            @Override
            public void onStartListening() {
                Log.i("IME: onStartListening");
                mCommandEditor.reset();
                setKeepScreenOn(true);
            }

            @Override
            public void onStopListening() {
                Log.i("IME: onStopListening");
                setKeepScreenOn(false);
            }

            // TODO: add onCancel()

            @Override
            public void onError(int errorCode) {
                setKeepScreenOn(false);
                Log.i("IME: onError: " + errorCode);
                if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    Intent intent = new Intent(SpeechInputMethodService.this, PermissionsRequesterActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    SpeechInputMethodService.this.startActivity(intent);
                }
            }
        };
    }
}
