package ee.ioc.phon.android.speak.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.OnSwipeTouchListener;
import ee.ioc.phon.android.speak.PackageNameRegistry;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.ServiceLanguageChooser;
import ee.ioc.phon.android.speak.activity.ComboSelectorActivity;
import ee.ioc.phon.android.speak.activity.RewritesActivity;
import ee.ioc.phon.android.speak.activity.RewritesSelectorActivity;
import ee.ioc.phon.android.speak.adapter.ClipboardAdapter;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.CommandMatcher;
import ee.ioc.phon.android.speechutils.editor.CommandMatcherFactory;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.view.MicButton;

public class SpeechInputView extends LinearLayoutCompat {

    private static final String[] EMPTY_STRING_ARRAY = {};

    // TODO: get the colors from speechutils
    private static final int COLOR_RECORDING = Color.argb(255, 204, 0, 0);
    private static final int COLOR_TRANSCRIBING = Color.argb(255, 153, 51, 204);

    private View mCentralButtons;
    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeAction;
    private ImageButton mBClipboard;
    private Button mBComboSelector;
    private TextView mTvInstruction;
    private TextView mTvMessage;
    private RecyclerView mRvClipboard;
    private RelativeLayout mRlClipboard;
    private LinearLayout mLlEmpty;

    private ComponentName mApp;
    private String mAppId = "";
    private SpeechInputViewListener mListener;
    private SpeechRecognizer mRecognizer;
    private ServiceLanguageChooser mSlc;

    private OnSwipeTouchListener mOstl;
    private OnCursorTouchListener mOctl;

    private MicButton.State mState;

    private String mUiState;

    // Y (yellow i.e. not-transcribing)
    // R (red, i.e. transcribing)
    private String mBtnType = "Y";

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

    public void setListener(final SpeechInputViewListener listener, EditorInfo editorInfo) {
        mListener = listener;
        // TODO: quick hack to add app to the matcher, not sure if we can access the
        // class name of the app
        if (editorInfo != null) {
            mApp = new ComponentName(editorInfo.packageName, editorInfo.packageName);
            mAppId = mApp.flattenToShortString();
        }
        if (mBImeAction != null && editorInfo != null) {
            // TODO: test
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
                // The content description is based on the text field's action label,
                // which might not always be present, or be the best description of the content.
                // TODO: fall back to a description like "go", "send" if action label is missing.
                mBImeAction.setContentDescription(editorInfo.actionLabel);
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

            mBClipboard.setOnClickListener(v -> {
                if (mUiState == null) {
                    mUiState = "1";
                } else if ("1".equals(mUiState)) {
                    mUiState = "2";
                } else {
                    mUiState = null;
                }
                Context context = getContext();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                Resources res = getResources();
                showUi(prefs, res, mUiState);
            });
            // TODO: experimental: long press controls mic
            mBClipboard.setOnLongClickListener(v -> {
                changeState();
                return true;
            });
        }

        ImageButton buttonDelete = findViewById(R.id.bImeDelete);
        if (buttonDelete != null) {
            buttonDelete.setImageResource(R.drawable.ic_backspace);
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
                setVisibility(mBClipboard, View.INVISIBLE);
                if (mRlClipboard.getVisibility() == View.GONE) {
                    setVisibility(mCentralButtons, View.INVISIBLE);
                    if (mBComboSelector != null) {
                        mBComboSelector.setVisibility(View.INVISIBLE);
                    }
                } else {
                    setVisibility(mRlClipboard, View.INVISIBLE);
                }
                showMessage("");
            }

            @Override
            public void onUp() {
                showMessage("");
                mBImeKeyboard.setVisibility(View.VISIBLE);
                mBImeAction.setVisibility(View.VISIBLE);
                setVisibility(mBClipboard, View.VISIBLE);
                if (mRlClipboard.getVisibility() == View.GONE) {
                    setVisibility(mCentralButtons, View.VISIBLE);
                    if (mBComboSelector != null) {
                        mBComboSelector.setVisibility(View.VISIBLE);
                    }
                } else {
                    setVisibility(mRlClipboard, View.VISIBLE);
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
        mBClipboard = findViewById(R.id.bClipboard);
        mBComboSelector = findViewById(R.id.tvComboSelector);
        mTvInstruction = findViewById(R.id.tvInstruction);
        mTvMessage = findViewById(R.id.tvMessage);
        mRvClipboard = findViewById(R.id.rvClipboard);
        mRlClipboard = findViewById(R.id.rlClipboard);
        mLlEmpty = findViewById(R.id.empty);

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = getResources();

        if (mRvClipboard != null) {
            mRvClipboard.setHasFixedSize(true);
            // TODO: make span count configurable
            mRvClipboard.setLayoutManager(new GridLayoutManager(context, getResources().getInteger(R.integer.spanCount)));
        }

        if (mSwipeType == 2) {
            // Turning from GONE to VISIBLE
            findViewById(R.id.rlKeyButtons).setVisibility(View.VISIBLE);
        }

        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = new ServiceLanguageChooser(context, prefs, keys, callerInfo);
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

        TypedArray keysAsTypedArray = res.obtainTypedArray(keys);
        final int key = keysAsTypedArray.getResourceId(0, 0);
        int keyHelpText = keysAsTypedArray.getResourceId(7, 0);
        int defaultHelpText = keysAsTypedArray.getResourceId(8, 0);
        keysAsTypedArray.recycle();

        if (mTvInstruction != null) {
            if (PreferenceUtils.getPrefBoolean(prefs, res, keyHelpText, defaultHelpText)) {
                mTvInstruction.setVisibility(View.VISIBLE);
            } else {
                mTvInstruction.setVisibility(View.GONE);
            }
        }

        mBImeStartStop.setOnTouchListener(new OnSwipeTouchListener(getContext(), mBImeStartStop) {

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
                changeState();
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

        // if mBImeKeyboard is available then we are in the IME mode where toggling
        // the UI size is possible.
        if (mBImeKeyboard != null) {
            showUi(prefs, res);
        }
    }

    /**
     * Performs an action after a press on the mic button, and given a current state.
     */
    private void changeState() {
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

    // TODO: this does not make sense if the UI is in the clipboard mode
    public void start() {
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            // TODO: fix this
            startListening(mSlc);
        }
    }

    // TODO: make public?
    private void stopListening() {
        mBClipboard.setColorFilter(COLOR_TRANSCRIBING);
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

    private void updateTouchListener(int type) {
        if (type == 1) {
            setOnTouchListener(mOstl);
        } else if (type == 2) {
            setOnTouchListener(mOctl);
        } else {
            setOnTouchListener(null);
        }
    }

    private void makeComboChange() {
        String language = mSlc.getLanguage();
        ComponentName service = mSlc.getService();
        mListener.onComboChange(language, service);
        if (mRvClipboard != null) {
            updateClipboard(getContext(), language, service, mApp);
        }
    }

    private ClipboardAdapter getClipboardAdapter(SharedPreferences prefs, Resources res, String tabName, CommandMatcher commandMatcher) {
        String rewritesAsStr = PreferenceUtils.getPrefMapEntry(prefs, res, R.string.keyRewritesMap, tabName);
        if (rewritesAsStr == null) {
            return null;
        }
        return new ClipboardAdapter(mListener, commandMatcher, rewritesAsStr);
    }

    /**
     * TODO: hide tabs without rewrites, or at least block the long press on an empty rewrites tab
     */
    private void updateClipboard(Context context, String language, ComponentName service, ComponentName app) {
        TabLayout tabs = findViewById(R.id.tlClipboardTabs);
        tabs.clearOnTabSelectedListeners();
        tabs.removeAllTabs();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = getResources();
        Set<String> defaults =
                PreferenceUtils.getPrefStringSet(prefs, res, R.string.defaultRewriteTables);
        if (defaults.isEmpty()) {
            mLlEmpty.setVisibility(View.VISIBLE);
            mRvClipboard.setVisibility(View.GONE);
            tabs.setVisibility(View.GONE);
            findViewById(R.id.buttonOpenRewrites).setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), RewritesSelectorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
            return;
        } else {
            mLlEmpty.setVisibility(View.GONE);
            mRvClipboard.setVisibility(View.VISIBLE);
            tabs.setVisibility(View.VISIBLE);
        }
        CommandMatcher commandMatcher = CommandMatcherFactory.createCommandFilter(language, service, app);

        String[] names = defaults.toArray(EMPTY_STRING_ARRAY);
        // TODO: defaults should be a list (not a set that needs to be sorted)
        Arrays.sort(names);
        String appId = app.flattenToShortString();
        String selectedTabName = getTabName(prefs, res, appId);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String name = tab.getText().toString();
                mRvClipboard.setAdapter(getClipboardAdapter(prefs, res, name, commandMatcher));
                setTabName(prefs, res, appId, name);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        for (String tabName : names) {
            TabLayout.Tab tab = tabs.newTab();
            tab.setText(tabName);
            tabs.addTab(tab, tabName.equals(selectedTabName));
        }
        // If the previously selected rewrites table is not among the defaults anymore then
        // we select the first one (but do not save it).
        if (tabs.getSelectedTabPosition() == -1) {
            tabs.getTabAt(0).select();
        }

        LinearLayout tabStrip = (LinearLayout) tabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            String name = tabs.getTabAt(i).getText().toString();
            // Long click loads the rewrites view (without populating the tab)
            tabStrip.getChildAt(i).setOnLongClickListener(v -> {
                Intent intent = new Intent(getContext(), RewritesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(RewritesActivity.EXTRA_NAME, name);
                intent.putExtra(RewritesActivity.EXTRA_LOCALE, language);
                intent.putExtra(RewritesActivity.EXTRA_APP, appId);
                intent.putExtra(RewritesActivity.EXTRA_SERVICE, service.flattenToShortString());
                context.startActivity(intent);
                return false;
            });
        }
    }

    private String getTabName(SharedPreferences prefs, Resources res, String appId) {
        return PreferenceUtils.getPrefMapEntry(prefs, res, R.string.mapClipboardTabName, appId);
    }

    private void setTabName(SharedPreferences prefs, Resources res, String appId, String name) {
        PreferenceUtils.putPrefMapEntry(prefs, res, R.string.mapClipboardTabName, appId, name);
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

    private void showUi(SharedPreferences prefs, Resources res) {
        String mUiState = PreferenceUtils.getPrefMapEntry(prefs, res, R.string.mapAppToMode, mAppId);
        mBClipboard.setImageResource(R.drawable.ic_baseline_swap_vert_24);
        showUi(prefs, res, mUiState);
    }

    private void showUi(SharedPreferences prefs, Resources res, String state) {
        PreferenceUtils.putPrefMapEntry(prefs, res, R.string.mapAppToMode, mAppId, state);

        if (mUiState == null) {
            updateTouchListener(mSwipeType);
            mRlClipboard.setVisibility(View.GONE);
            mCentralButtons.setVisibility(View.VISIBLE);
        } else if ("1".equals(mUiState)) {
            updateTouchListener(0);
            mCentralButtons.setVisibility(View.GONE);
            mRlClipboard.setVisibility(View.VISIBLE);
        } else {
            mCentralButtons.setVisibility(View.GONE);
            mRlClipboard.setVisibility(View.GONE);
        }
    }

    /*
    private void loadDrawable(ImageView view, int res) {
        view.setBackground(AppCompatResources.getDrawable(getContext(), res));
    }
    */

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
        if (mBImeStartStop != null) {
            mBImeStartStop.post(() -> mBImeStartStop.setState(mState));
        }
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
        updateTouchListener(mSwipeType);
        if (mBClipboard != null) {
            mBClipboard.setColorFilter(null);
        }
        if (mBImeKeyboard != null) {
            mBImeKeyboard.setImageResource(R.drawable.ic_ime);
            mBImeKeyboard.setOnClickListener(v -> mListener.onSwitchToLastIme());

            mBImeKeyboard.setOnLongClickListener(v -> {
                mListener.onSwitchIme(false);
                return true;
            });
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
        if (mBClipboard != null) {
            mBClipboard.setColorFilter(COLOR_RECORDING);
        }
        updateServiceLanguage(slc.getSpeechRecognizer());
        // Increases the counter of the app that calls the recognition service.
        // TODO: we could define it slightly differently, e.g. only count successful recognitions,
        // count also commands executed via swipes and/or buttons (but maybe not count every deletion
        // and cursor movement).
        // TODO: we could also count languages, services, etc.
        PackageNameRegistry.increaseAppCount(getContext(), slc.getIntent().getExtras(), null);
        mRecognizer.startListening(slc.getIntent());
        mListener.onStartListening();
        setVisibility(findViewById(R.id.rlKeyButtons), View.INVISIBLE);
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

}
