package ee.ioc.phon.android.speak.service;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.android.speak.view.SpeechInputView;

public class SpeechInputMethodService extends InputMethodService {

    private InputMethodManager mInputMethodManager;

    private SpeechInputView mInputView;

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
        closeSession();
    }

    @Override
    public View onCreateInputView() {
        Log.i("onCreateInputView");
        mInputView = (SpeechInputView) getLayoutInflater().inflate(R.layout.voice_ime_view, null);
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
        closeSession();
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        Log.i("onStartInputView: " + attribute.inputType + "/" + attribute.imeOptions + "/" + restarting);

        closeSession();

        if (restarting) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Bundle extras = new Bundle();
        extras.putBoolean(Extras.EXTRA_UNLIMITED_DURATION,
                !PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyImeAutoStopAfterPause, R.bool.defaultImeAutoStopAfterPause));
        CallerInfo callerInfo = new CallerInfo(extras, attribute, getPackageName());

        mInputView.setListener(R.array.keysIme, callerInfo, new SpeechInputView.VoiceImeViewListener() {

            TextUpdater mTextUpdater = new TextUpdater();

            @Override
            public void onPartialResult(ArrayList<String> results) {
                mTextUpdater.commitPartialResult(getCurrentInputConnection(), selectFirstResult(results));
            }

            @Override
            public void onFinalResult(ArrayList<String> results) {
                mTextUpdater.commitFinalResult(getCurrentInputConnection(), selectFirstResult(results));
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
                mTextUpdater.deleteWord(getCurrentInputConnection());
            }

            @Override
            public void onAddNewline() {
                mTextUpdater.addNewline(getCurrentInputConnection());
            }

            @Override
            public void onAddSpace() {
                mTextUpdater.addSpace(getCurrentInputConnection());
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // TODO: store buffer
            }

            @Override
            public void onSelectAll() {
                // TODO: show ContextMenu
                getCurrentInputConnection().performContextMenuAction(android.R.id.selectAll);
            }

            @Override
            public void onReset() {
                // TODO: hide ContextMenu (if visible)
                InputConnection ic = getCurrentInputConnection();
                CharSequence cs = ic.getSelectedText(0);
                if (cs != null) {
                    int len = cs.length();
                    ic.setSelection(len, len);
                }
            }
        });

        // Launch recognition immediately (if set so)
        if (PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyImeAutoStart, R.bool.defaultImeAutoStart)) {
            Log.i("Auto-starting");
            mInputView.start();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        Log.i("onFinishInputView: " + finishingInput);
        closeSession();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        Log.i("onCurrentInputMethodSubtypeChanged: " + subtype);
        closeSession();
    }

    private void closeSession() {
        if (mInputView != null) {
            mInputView.closeSession();
        }
    }

    private void handleClose() {
        requestHideSelf(0);
        closeSession();
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
        closeSession();
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
     * Performs the Search-action, e.g. to launch search on a searchbar.
     */
    private void performGo() {
        // Does not work on Google Searchbar
        // getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_DONE);

        // Works in Google Searchbar, GF Translator, but NOT in the Firefox search widget
        //getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_GO);

        getCurrentInputConnection().performEditorAction(EditorInfo.IME_ACTION_SEARCH);
    }


    private static class TextUpdater {

        // Maximum number of characters that left-swipe is willing to delete
        private static final int MAX_DELETABLE_CONTEXT = 100;
        private static final Pattern WHITESPACE = Pattern.compile("\\s{1,}");

        private String mPrevText = "";

        public TextUpdater() {
        }


        /**
         * Writes the text into the text field and forgets the previous entry.
         */
        public void commitFinalResult(InputConnection ic, String text) {
            commitText(ic, text);
            mPrevText = "";
        }

        /**
         * Writes the text into the text field and stores it for future reference.
         */
        public void commitPartialResult(InputConnection ic, String text) {
            commitText(ic, text);
            mPrevText = text;
        }

        public void addNewline(InputConnection ic) {
            if (ic != null) {
                ic.commitText("\n", 1);
            }
        }

        public void addSpace(InputConnection ic) {
            if (ic != null) {
                ic.commitText(" ", 1);
            }
        }

        /**
         * Deletes all characters up to the leftmost whitespace from the cursor (including the whitespace).
         * If something is selected then delete the selection.
         * TODO: maybe expensive?
         */
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public void deleteWord(InputConnection ic) {
            if (ic != null) {
                // If something is selected then delete the selection and return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                        && ic.getSelectedText(0) != null) {
                    ic.commitText("", 0);
                } else {
                    CharSequence beforeCursor = ic.getTextBeforeCursor(MAX_DELETABLE_CONTEXT, 0);
                    if (beforeCursor != null) {
                        int beforeCursorLength = beforeCursor.length();
                        Matcher m = WHITESPACE.matcher(beforeCursor);
                        int lastIndex = 0;
                        while (m.find()) {
                            lastIndex = m.start();
                        }
                        if (lastIndex > 0) {
                            ic.deleteSurroundingText(beforeCursorLength - lastIndex, 0);
                        } else if (beforeCursorLength < MAX_DELETABLE_CONTEXT) {
                            ic.deleteSurroundingText(beforeCursorLength, 0);
                        }
                    }
                }
            }
        }

        /**
         * Updates the text field, modifying only the parts that have changed.
         */
        private void commitText(InputConnection ic, String text) {
            if (ic != null && text != null && text.length() > 0) {
                // Calculate the length of the text that has changed
                String commonPrefix = greatestCommonPrefix(mPrevText, text);
                int commonPrefixLength = commonPrefix.length();
                int prevLength = mPrevText.length();
                int deletableLength = prevLength - commonPrefixLength;

                if (deletableLength > 0) {
                    ic.deleteSurroundingText(deletableLength, 0);
                }

                if (commonPrefixLength == text.length()) {
                    return;
                }

                // We look at the left context of the cursor
                // to decide which glue symbol to use and whether to capitalize the text.
                CharSequence leftContext = ic.getTextBeforeCursor(MAX_DELETABLE_CONTEXT, 0);
                String glue = "";
                if (commonPrefixLength == 0) {
                    char firstChar = text.charAt(0);

                    if (!(leftContext.length() == 0
                            || Constants.CHARACTERS_WS.contains(firstChar)
                            || Constants.CHARACTERS_PUNCT.contains(firstChar)
                            || Constants.CHARACTERS_WS.contains(leftContext.charAt(leftContext.length() - 1)))) {
                        glue = " ";
                    }
                } else {
                    text = text.substring(commonPrefixLength);
                }

                // Capitalize if required by left context
                String leftContextTrimmed = leftContext.toString().trim();
                if (leftContextTrimmed.length() == 0
                        || Constants.CHARACTERS_EOS.contains(leftContextTrimmed.charAt(leftContextTrimmed.length() - 1))) {
                    // Since the text can start with whitespace (newline),
                    // we capitalize the first non-whitespace character.
                    int firstNonWhitespaceIndex = -1;
                    for (int i = 0; i < text.length(); i++) {
                        if (!Constants.CHARACTERS_WS.contains(text.charAt(i))) {
                            firstNonWhitespaceIndex = i;
                            break;
                        }
                    }
                    if (firstNonWhitespaceIndex > -1) {
                        String newText = text.substring(0, firstNonWhitespaceIndex)
                                + Character.toUpperCase(text.charAt(firstNonWhitespaceIndex));
                        if (firstNonWhitespaceIndex < text.length() - 1) {
                            newText += text.substring(firstNonWhitespaceIndex + 1);
                        }
                        text = newText;
                    }
                }

                ic.commitText(glue + text, 1);
            }
        }

    }

    private static String greatestCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }

    private static String selectFirstResult(ArrayList<String> results) {
        if (results == null || results.size() < 1) {
            return "";
        }
        return results.get(0);
    }
}