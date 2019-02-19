package ee.ioc.phon.android.speak.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.OnSwipeTouchListener;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.ServiceLanguageChooser;
import ee.ioc.phon.android.speak.activity.ComboSelectorActivity;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.Command;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.view.MicButton;

public class SpeechInputView extends LinearLayout {

    private View mCentralButtons;
    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeAction;
    private Button mBComboSelector;
    private TextView mTvInstruction;
    private TextView mTvMessage;
    private RecyclerView mRvClipboard;

    private ComponentName mApp;
    private SpeechInputViewListener mListener;
    private SpeechRecognizer mRecognizer;
    private ServiceLanguageChooser mSlc;

    private OnSwipeTouchListener mOstl;
    private OnCursorTouchListener mOctl;

    private MicButton.State mState;

    // Y (yellow i.e. not-transcribing)
    // R (red, i.e. transcribing)
    private String mBtnType = "Y";

    private boolean mUiIsMinimized = false;

    // TODO: make it an attribute
    private int mSwipeType = 0;
    private final static String DASH_CUR = "――――――――――――――――――――";
    private final static String DASH_SEL = "■■■■■■■■■■■■■■■■■■■■";
    private final static int DASH_LENGTH = DASH_CUR.length();

    public interface SpeechInputViewListener {

        void onComboChange(String language, ComponentName service);

        void onPartialResult(List<String> text, boolean isSemiFinal);

        void onFinalResult(List<String> text, Bundle bundle);

        void onCommand(String text);

        /**
         * Switch to the next IME or ask the user to choose the IME.
         *
         * @param isAskUser Iff true then ask the user to choose the IME
         */
        void onSwitchIme(boolean isAskUser);

        /**
         * Switch to the previous IME (the IME that launched this IME).
         */
        void onSwitchToLastIme();

        /**
         * Perform an editor action (GO, NEXT, ...).
         *
         * @param actionId Action ID
         * @param hide     hide the IME after performing the action, iff true
         */
        void onAction(int actionId, boolean hide);

        void onDeleteLeftChar();

        void onDeleteLastWord();

        void goUp();

        void goDown();

        void moveRel(int numOfChars);

        void moveRelSel(int numOfChars, int type);

        void onExtendSel(String regex);

        void onAddNewline();

        void onAddSpace();

        void onSelectAll();

        void onReset();

        void onBufferReceived(byte[] buffer);

        void onError(int errorCode);

        void onStartListening();

        void onStopListening();
    }

    public SpeechInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // TODO: change the content description when the button changes
    // mBImeAction.setContentDescription(mContext.getString(R.string.cdImeNewline));
    public void setListener(final SpeechInputViewListener listener, EditorInfo editorInfo) {
        mListener = listener;
        // TODO: quick hack to add app to the matcher, not sure if we can access the
        // class name of the app
        if (editorInfo != null) {
            mApp = new ComponentName(editorInfo.packageName, editorInfo.packageName);
        }
        if (mBImeAction != null && editorInfo != null) {
            boolean overrideEnter = (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0;
            boolean useEnter = !overrideEnter;
            final int imeAction = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (overrideEnter) {
                boolean hide = true;
                if (imeAction == EditorInfo.IME_ACTION_GO) {
                    mBImeAction.setImageResource(R.drawable.ic_go);
                } else if (imeAction == EditorInfo.IME_ACTION_SEARCH) {
                    mBImeAction.setImageResource(R.drawable.ic_search);
                } else if (imeAction == EditorInfo.IME_ACTION_SEND) {
                    mBImeAction.setImageResource(R.drawable.ic_send);
                } else if (imeAction == EditorInfo.IME_ACTION_DONE) {
                    mBImeAction.setImageResource(R.drawable.ic_done);
                    hide = false;
                } else if (imeAction == EditorInfo.IME_ACTION_NEXT) {
                    mBImeAction.setImageResource(R.drawable.ic_next);
                    hide = false;
                } else {
                    useEnter = true;
                }
                final boolean finalHide = hide;
                mBImeAction.setOnClickListener(v -> {
                    if (finalHide) {
                        cancelOrDestroy();
                    }
                    mListener.onAction(imeAction, finalHide);
                });
            }

            // If no action was defined, then we show the Enter icon,
            // even if we were allowed to override Enter.
            if (useEnter) {
                mBImeAction.setImageResource(R.drawable.ic_newline);
                mBImeAction.setOnClickListener(v -> mListener.onAddNewline());
            }

            mBImeAction.setOnLongClickListener(v -> {
                toggleClipboard();
                return true;
            });
        }

        ImageButton buttonDelete = findViewById(R.id.bImeDelete);
        if (buttonDelete != null) {
            buttonDelete.setOnTouchListener(new OnPressAndHoldListener() {
                @Override
                public void onAction() {
                    mListener.onDeleteLeftChar();
                }
            });
        }

        mOstl = new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                mListener.onDeleteLastWord();
            }

            @Override
            public void onSwipeRight() {
                mListener.onAddNewline();
            }

            @Override
            public void onSwipeUp() {
                mListener.goUp();
            }

            @Override
            public void onSwipeDown() {
                mListener.goDown();
            }

            @Override
            public void onSingleTapMotion() {
                mListener.onReset();
            }

            @Override
            public void onDoubleTapMotion() {
                mListener.onAddSpace();
            }

            @Override
            public void onLongPressMotion() {
                mListener.onSelectAll();
            }
        };

        // TODO: move to utilities (48dp for the edges)
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int edge = Math.round(48 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        mOctl = new OnCursorTouchListener(edge) {
            @Override
            public void onMove(int numOfChars) {
                mListener.moveRel(numOfChars);
                showMessageArrow(numOfChars, DASH_CUR);
            }

            @Override
            public void onMoveSel(int numOfChars, int type) {
                mListener.moveRelSel(numOfChars, type);
                showMessageArrow(numOfChars, DASH_SEL);
            }

            @Override
            public void onLongPress() {
                // Selects current word.
                // The selection can be later changed, e.g. include punctuation.
                mListener.onExtendSel("\\w+");
                setBackgroundResource(R.drawable.rectangle_gradient_light);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            @Override
            public void onSingleTapMotion() {
                mListener.onReset();
            }

            @Override
            public void onDoubleTapMotion() {
                mListener.onAddSpace();
            }

            @Override
            public void onDown() {
                mBImeKeyboard.setVisibility(View.INVISIBLE);
                mBImeAction.setVisibility(View.INVISIBLE);
                if (mRvClipboard.getVisibility() == View.GONE) {
                    mBImeStartStop.setVisibility(View.INVISIBLE);
                    setVisibility(mTvInstruction, View.INVISIBLE);
                    if (mBComboSelector != null) {
                        mBComboSelector.setVisibility(View.INVISIBLE);
                    }
                    setVisibility(findViewById(R.id.rlKeyButtons), View.INVISIBLE);
                } else {
                    setVisibility(mRvClipboard, View.INVISIBLE);
                }
                showMessage("");
            }

            @Override
            public void onUp() {
                showMessage("");
                mBImeKeyboard.setVisibility(View.VISIBLE);
                mBImeAction.setVisibility(View.VISIBLE);
                if (mRvClipboard.getVisibility() == View.GONE) {
                    mBImeStartStop.setVisibility(View.VISIBLE);
                    setVisibility(mTvInstruction, View.VISIBLE);
                    if (mBComboSelector != null) {
                        mBComboSelector.setVisibility(View.VISIBLE);
                    }
                    setVisibility(findViewById(R.id.rlKeyButtons), View.VISIBLE);
                } else {
                    setVisibility(mRvClipboard, View.VISIBLE);
                }
                setBackgroundResource(R.drawable.rectangle_gradient);
            }

            @Override
            public void onSwipeUp() {
                mListener.onAction(EditorInfo.IME_ACTION_PREVIOUS, false);
            }

            @Override
            public void onSwipeDown() {
                mListener.onAction(EditorInfo.IME_ACTION_NEXT, false);
            }
        };
        setGuiInitState(0);
        makeComboChange();
    }

    public void init(int keys, CallerInfo callerInfo, int swipeType) {
        mSwipeType = swipeType;
        // These controls are optional (i.e. can be null),
        // except for mBImeStartStop (TODO: which should also be optional)
        mCentralButtons = findViewById(R.id.centralButtons);
        mBImeStartStop = findViewById(R.id.bImeStartStop);
        mBImeKeyboard = findViewById(R.id.bImeKeyboard);
        mBImeAction = findViewById(R.id.bImeAction);
        mBComboSelector = findViewById(R.id.tvComboSelector);
        mTvInstruction = findViewById(R.id.tvInstruction);
        mTvMessage = findViewById(R.id.tvMessage);
        mRvClipboard = findViewById(R.id.rvClipboard);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (mRvClipboard != null) {
            mRvClipboard.setVisibility(View.GONE);
            mRvClipboard.setHasFixedSize(true);
            // TODO: make span count configurable
            mRvClipboard.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }

        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = new ServiceLanguageChooser(getContext(), prefs, keys, callerInfo);
        if (mBComboSelector != null) {
            if (mSlc.size() > 1) {
                mBComboSelector.setVisibility(View.VISIBLE);
            } else {
                mBComboSelector.setVisibility(View.GONE);
                mBComboSelector = null;
            }
        }
        updateServiceLanguage(mSlc.getSpeechRecognizer());
        if (mBComboSelector != null) {
            updateComboSelector(mSlc);
        }
        showMessage("");

        TypedArray keysAsTypedArray = getResources().obtainTypedArray(keys);
        final int key = keysAsTypedArray.getResourceId(0, 0);
        int keyHelpText = keysAsTypedArray.getResourceId(8, 0);
        int defaultHelpText = keysAsTypedArray.getResourceId(9, 0);
        keysAsTypedArray.recycle();

        if (mTvInstruction != null) {
            if (PreferenceUtils.getPrefBoolean(prefs, getResources(), keyHelpText, defaultHelpText)) {
                mTvInstruction.setVisibility(View.VISIBLE);
            } else {
                mTvInstruction.setVisibility(View.GONE);
            }
        }

        mBImeStartStop.setOnTouchListener(new OnSwipeTouchListener(getContext()) {

            @Override
            public void onSwipeLeft() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_LEFT");
            }

            @Override
            public void onSwipeRight() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_RIGHT");
            }

            @Override
            public void onSwipeUp() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_UP");
            }

            @Override
            public void onSwipeDown() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_DOWN");
            }

            @Override
            public void onSingleTapMotion() {
                Log.i("Microphone button pressed: state = " + mState);
                switch (mState) {
                    case INIT:
                    case ERROR:
                        startListening(mSlc);
                        break;
                    case RECORDING:
                        stopListening();
                        break;
                    case LISTENING:
                    case TRANSCRIBING:
                        cancelOrDestroy();
                        setGuiInitState(0);
                        break;
                    default:
                }
            }

            @Override
            public void onDoubleTapMotion() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_DOUBLETAP");
            }

            @Override
            public void onLongPressMotion() {
                mListener.onCommand("K6_" + mBtnType + "_BTN_MIC_LONGPRESS");
            }

        });

        if (mBComboSelector != null) {
            mBComboSelector.setOnClickListener(v -> {
                nextCombo();
            });

            mBComboSelector.setOnLongClickListener(view -> {
                comboSelector(key);
                return true;
            });
        }
    }

    public void start() {
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            // TODO: fix this
            startListening(mSlc);
        }
    }

    // TODO: make public?
    private void stopListening() {
        if (mRecognizer != null) {
            mRecognizer.stopListening();
        }
        mListener.onStopListening();
    }

    public void cancel() {
        cancelOrDestroy();
        setGuiInitState(0);
    }

    public void showMessage(CharSequence message) {
        if (mTvMessage != null) {
            if (message == null || message.length() == 0) {
                setText(mTvMessage, "");
            } else {
                mTvMessage.setEllipsize(TextUtils.TruncateAt.START);
                mTvMessage.setPaintFlags(mTvMessage.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG & ~Paint.UNDERLINE_TEXT_FLAG);
                setText(mTvMessage, message);
            }
        }
    }

    public void showMessage(CharSequence message, boolean isSuccess) {
        if (mTvMessage != null) {
            if (message == null || message.length() == 0) {
                setText(mTvMessage, "");
            } else {
                mTvMessage.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                if (isSuccess) {
                    mTvMessage.setPaintFlags(mTvMessage.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG) | Paint.UNDERLINE_TEXT_FLAG);
                } else {
                    mTvMessage.setPaintFlags(mTvMessage.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG) | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                setText(mTvMessage, message);
            }
        }
    }


    private void toggleClipboard() {
        if (mRvClipboard.getVisibility() == View.GONE) {
            setVisibilityKeyboard(View.GONE);
            mRvClipboard.setVisibility(View.VISIBLE);
        } else {
            mRvClipboard.setVisibility(View.GONE);
            setVisibilityKeyboard(View.VISIBLE);
        }
    }

    private void makeComboChange() {
        mListener.onComboChange(mSlc.getLanguage(), mSlc.getService());
        if (mRvClipboard != null) {
            mRvClipboard.setAdapter(new ClipboardAdapter(mSlc.getLanguage(), mSlc.getService(), mApp));
        }
    }

    private void nextCombo() {
        if (mState == MicButton.State.RECORDING) {
            stopListening();
        }
        mSlc.next();
        makeComboChange();
        updateComboSelector(mSlc);
    }

    private void comboSelector(int key) {
        cancelOrDestroy();
        Context context = getContext();
        Intent intent = new Intent(context, ComboSelectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("key", context.getString(key));
        context.startActivity(intent);
    }

    private void toggleUi() {
        if (mUiIsMinimized) {
            maximizeUi();
        } else {
            minimizeUi();
        }
    }

    private void minimizeUi() {
        mUiIsMinimized = true;
        setVisibilityKeyboard(View.GONE);
        mBImeKeyboard.setImageResource(R.drawable.ic_arrow_upward);
        mBImeKeyboard.setOnClickListener(v -> toggleUi());
        setBackgroundResource(R.drawable.rectangle_gradient_red);
    }

    private void maximizeUi() {
        mUiIsMinimized = false;
        setVisibilityKeyboard(View.VISIBLE);
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            mBImeKeyboard.setImageResource(R.drawable.ic_ime);
            mBImeKeyboard.setOnClickListener(v -> mListener.onSwitchToLastIme());

            mBImeKeyboard.setOnLongClickListener(v -> {
                mListener.onSwitchIme(false);
                return true;
            });
        } else {
            mBImeKeyboard.setImageResource(R.drawable.ic_arrow_downward);
            mBImeKeyboard.setOnClickListener(v -> toggleUi());
        }
        setBackgroundResource(R.drawable.rectangle_gradient);
    }

    private void setVisibilityKeyboard(int visibility) {
        mCentralButtons.setVisibility(visibility);
        if (mBComboSelector != null) {
            //mBComboSelector.setVisibility(visibility);
        }
    }

    private void showMessageArrow(int numOfChars, String dash) {
        if (numOfChars < 0) {
            int num = -1 * numOfChars;
            if (DASH_LENGTH > num) {
                showMessage("◄" + dash.substring(0, num));
            }
        } else if (DASH_LENGTH > numOfChars) {
            showMessage(dash.substring(0, numOfChars) + "►");
        }
    }

    private static String selectFirstResult(List<String> results) {
        if (results == null || results.size() < 1) {
            return null;
        }
        return results.get(0);
    }

    private void setGuiState(MicButton.State state) {
        mState = state;
        setMicButtonState(mBImeStartStop, mState);
    }

    private void setGuiInitState(int message) {
        if (message == 0) {
            // Do not clear a possible error message
            //showMessage("");
            setGuiState(MicButton.State.INIT);
            setVisibility(findViewById(R.id.rlKeyButtons), View.VISIBLE);
        } else {
            setGuiState(MicButton.State.ERROR);
            showMessage(String.format(getResources().getString(R.string.labelSpeechInputViewMessage), getResources().getString(message)));
        }
        if (mSwipeType == 1) {
            setOnTouchListener(mOstl);
        } else if (mSwipeType == 2) {
            // Turning from GONE to VISIBLE
            findViewById(R.id.rlKeyButtons).setVisibility(View.VISIBLE);
            setOnTouchListener(mOctl);
        }
        if (mBImeKeyboard != null) {
            maximizeUi();
        }
        setText(mTvInstruction, R.string.buttonImeSpeak);
    }

    private static String lastChars(List<String> results, boolean isFinal) {
        return lastChars(selectFirstResult(results), isFinal);
    }

    private static String lastChars(String str, boolean isFinal) {
        if (str == null) {
            str = "";
        } else {
            str = str.replaceAll("\\n", "↲");
        }
        if (isFinal) {
            return str + "▪";
        }
        return str;
    }

    private static void setText(final TextView textView, final CharSequence text) {
        if (textView != null && textView.getVisibility() != View.GONE) {
            textView.post(() -> textView.setText(text));
        }
    }

    private static void setText(final TextView textView, final int text) {
        if (textView != null && textView.getVisibility() != View.GONE) {
            textView.post(() -> textView.setText(text));
        }
    }

    private static void setMicButtonVolumeLevel(final MicButton button, final float rmsdB) {
        if (button != null) {
            button.post(() -> button.setVolumeLevel(rmsdB));
        }
    }

    private static void setMicButtonState(final MicButton button, final MicButton.State state) {
        if (button != null) {
            button.post(() -> button.setState(state));
        }
    }

    private static void setVisibility(final View view, final int visibility) {
        if (view != null && view.getVisibility() != View.GONE) {
            view.post(() -> view.setVisibility(visibility));
        }
    }

    private void updateComboSelector(ServiceLanguageChooser slc) {
        Combo combo = new Combo(getContext(), slc.getCombo());
        mBComboSelector.setText(combo.getLongLabel());
    }

    private void updateServiceLanguage(SpeechRecognizer sr) {
        cancelOrDestroy();
        mRecognizer = sr;
        mRecognizer.setRecognitionListener(new SpeechInputRecognitionListener());
    }

    private void startListening(ServiceLanguageChooser slc) {
        setGuiState(MicButton.State.WAITING);
        updateServiceLanguage(slc.getSpeechRecognizer());
        mRecognizer.startListening(slc.getIntent());
        mListener.onStartListening();
        setVisibility(findViewById(R.id.rlKeyButtons), View.INVISIBLE);
        if (mBImeKeyboard != null) {
            maximizeUi();
        }
    }

    /**
     * TODO: not sure if it is better to call cancel or destroy
     * Note that SpeechRecognizer#destroy calls cancel first.
     */
    private void cancelOrDestroy() {
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
    }


    private class SpeechInputRecognitionListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.i("onReadyForSpeech: state = " + mState);
            setGuiState(MicButton.State.LISTENING);
            mBtnType = "R";
            setText(mTvInstruction, R.string.buttonImeStop);
            showMessage("");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i("onBeginningOfSpeech: state = " + mState);
            setGuiState(MicButton.State.RECORDING);
            mBtnType = "R";
        }

        @Override
        public void onEndOfSpeech() {
            Log.i("onEndOfSpeech: state = " + mState);
            // We go into the TRANSCRIBING-state only if we were in the RECORDING-state,
            // otherwise we ignore this event. This improves compatibility with
            // Google Voice Search, which calls EndOfSpeech after onResults.
            if (mState == MicButton.State.RECORDING) {
                setGuiState(MicButton.State.TRANSCRIBING);
                setText(mTvInstruction, R.string.statusImeTranscribing);
            }
        }

        /**
         * We process all possible SpeechRecognizer errors. Most of them
         * are generated by our implementation, others can be generated by the
         * framework, e.g. ERROR_CLIENT results from
         * "stopListening called with no preceding startListening".
         *
         * @param errorCode SpeechRecognizer error code
         */
        @Override
        public void onError(final int errorCode) {
            Log.i("onError: " + errorCode);
            mListener.onError(errorCode);

            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    setGuiInitState(R.string.errorImeResultAudioError);
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    setGuiInitState(R.string.errorImeResultRecognizerBusy);
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    setGuiInitState(R.string.errorImeResultServerError);
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    setGuiInitState(R.string.errorImeResultNetworkError);
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    setGuiInitState(R.string.errorImeResultNetworkTimeoutError);
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    setGuiInitState(R.string.errorImeResultClientError);
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    setGuiInitState(R.string.errorImeResultInsufficientPermissions);
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    setGuiInitState(R.string.errorImeResultNoMatch);
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    setGuiInitState(R.string.errorImeResultSpeechTimeout);
                    break;
                default:
                    Log.e("This might happen in future Android versions: code " + errorCode);
                    setGuiInitState(R.string.errorImeResultClientError);
                    break;
            }
            mBtnType = "Y";
        }

        @Override
        public void onPartialResults(final Bundle bundle) {
            Log.i("onPartialResults: state = " + mState);
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (results != null && !results.isEmpty()) {
                // This can be true only with kaldi-gstreamer-server
                // ... and with Tilde's version of kaldi-gstreamer-server
                boolean isSemiFinal = bundle.getBoolean(Extras.EXTRA_SEMI_FINAL)
                        || bundle.getBoolean("com.tilde.tildesbalss.extra.SEMI_FINAL");
                showMessage(lastChars(results, isSemiFinal));
                mListener.onPartialResult(results, isSemiFinal);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // TODO: future work: not sure how this can be generated by the service
            Log.i("onEvent: type = " + eventType);
        }

        @Override
        public void onResults(final Bundle bundle) {
            Log.i("onResults: state = " + mState);
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.i("onResults: results = " + results);
            if (results == null || results.isEmpty()) {
                // If we got empty results then assume that the session ended,
                // e.g. cancel was called.
                // TODO: not sure why this was needed
                //mListener.onFinalResult(Collections.<String>emptyList(), bundle);
            } else {
                showMessage(lastChars(results, true));
                mListener.onFinalResult(results, bundle);
            }
            setGuiInitState(0);
            mBtnType = "Y";
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //Log.i("onRmsChanged");
            setMicButtonVolumeLevel(mBImeStartStop, rmsdB);
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.i("View: onBufferReceived: " + buffer.length);
            mListener.onBufferReceived(buffer);
        }
    }

    private class ClipboardAdapter extends RecyclerView.Adapter<ClipboardAdapter.MyViewHolder> {
        private final List<String> mDataset;
        private final Map<String, String> mClipboard;
        private final SharedPreferences mPrefs;
        private final Resources mRes;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView mView;

            public MyViewHolder(TextView v) {
                super(v);
                mView = v;
            }
        }

        /**
         * TODO: improve specification of header (load only the columns that are needed)
         * TODO: implement putPrefMapMap (takes map instead of key and val)
         * TODO: improve dealing with nulls
         * TODO: support named clipboards
         */
        public ClipboardAdapter(String lang, ComponentName service, ComponentName app) {
            Context context = getContext();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mRes = getResources();
            mDataset = new ArrayList<>();
            mClipboard = new HashMap<>();
            for (UtteranceRewriter ur : Utils.genRewriters(mPrefs, mRes, null, lang, service, app)) {
                for (Command command : ur.getCommands()) {
                    String key = command.get(UtteranceRewriter.HEADER_COMMENT);
                    String val = command.get(UtteranceRewriter.HEADER_UTTERANCE);
                    key = key == null ? val : key;
                    val = val == null ? key : val;
                    Log.i("save to clipboard: " + key + "->" + val);
                    mDataset.add(key);
                    mClipboard.put(key, val);
                }
            }
            Collections.sort(mDataset, (o1, o2) -> mClipboard.get(o1).compareTo(mClipboard.get(o2)));
        }

        @Override
        public ClipboardAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ClipboardAdapter.MyViewHolder((TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_clip, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ClipboardAdapter.MyViewHolder holder, int position) {
            final String key = mDataset.get(position);
            final String val = mClipboard.get(key);
            holder.mView.setText(key);
            holder.mView.setOnClickListener(view -> mListener.onFinalResult(
                    Collections.singletonList(val), new Bundle()));
            holder.mView.setOnLongClickListener(v -> {
                // TODO: delete with confirmation
                if (key.equals(val)) {
                    showMessage(key + "=");
                } else {
                    showMessage(key + "->" + val);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
