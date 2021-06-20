package ee.ioc.phon.android.speak.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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
import ee.ioc.phon.android.speak.activity.RewritesActivity;
import ee.ioc.phon.android.speak.activity.RewritesSelectorActivity;
import ee.ioc.phon.android.speak.adapter.ClipboardAdapter;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.editor.CommandMatcher;
import ee.ioc.phon.android.speechutils.editor.CommandMatcherFactory;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.view.MicButton;

public class SpeechInputView extends LinearLayoutCompat {

    private static final String[] EMPTY_STRING_ARRAY = {};

    private View mCentralButtons;
    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeDragHandle;
    private ImageButton mBImeAction;
    private ImageButton mBUiMode;
    private ComboSelectorView mComboSelectorView;
    private TextView mTvInstruction;
    private TextView mTvMessage;
    private RecyclerView mRvClipboard;
    private RelativeLayout mRlClipboard;
    private RelativeLayout mRlMiddle;
    private RelativeLayout mRlBottomBar;
    private LinearLayout mLlEmpty;

    private ComponentName mApp;
    private String mAppId = "";
    private SpeechInputViewListener mListener;
    private SpeechRecognizer mRecognizer;

    private MicButton.State mState;

    private int mUiState = -1;

    // Y (yellow i.e. not-transcribing)
    // R (red, i.e. transcribing)
    private String mBtnType = "Y";

    // TODO: make it an attribute
    private boolean mSwipeType;
    private final static String DASH_CUR = "――――――――――――――――――――";
    private final static String DASH_SEL = "■■■■■■■■■■■■■■■■■■■■";
    private final static int DASH_LENGTH = DASH_CUR.length();
    private final static String NEW_TAB_LABEL = "+";

    int mOrientation;
    int mHeight;

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
        Resources res = getResources();
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
                mBImeAction.setOnClickListener(v -> {
                    if (finalHide) {
                        cancelOrDestroy();
                    }
                    mListener.onAction(imeAction, finalHide);
                });
            }

            // If no action was defined, then we show the Enter icon,
            // even if we were allowed to override Enter.
            CharSequence tooltipText = editorInfo.actionLabel;
            if (useEnter) {
                mBImeAction.setImageResource(R.drawable.ic_newline);
                mBImeAction.setOnClickListener(v -> mListener.onAddNewline());
                tooltipText = res.getString(R.string.cdNewline);
            }

            // The content description is based on the text field's action label,
            // which might not always be present, or be the best description of the content.
            CharSequence contentDescription;
            if (tooltipText == null || tooltipText.length() == 0) {
                contentDescription = res.getString(R.string.cdUndefined);
                tooltipText = String.format(getResources().getString(R.string.labelSpeechInputViewMessage), contentDescription);
            } else {
                contentDescription = tooltipText;
            }

            mBImeAction.setContentDescription(contentDescription);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBImeAction.setTooltipText(tooltipText);
            }

            // if mBImeKeyboard is available then we are in the IME mode where changing
            // the UI mode is possible.
            if (mBImeKeyboard != null) {
                Context context = getContext();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                mBUiMode.setImageResource(R.drawable.ic_baseline_mic_24);
                mBUiMode.setOnClickListener(v -> changeState());
                mBUiMode.setContentDescription(res.getString(R.string.cdMicrophone));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mBUiMode.setTooltipText(res.getString(R.string.cdMicrophone));
                }

                mBImeKeyboard.setImageResource(R.drawable.ic_ime);
                mBImeKeyboard.setOnClickListener(v -> mListener.onSwitchToLastIme());
                mBImeKeyboard.setOnLongClickListener(v -> {
                    mListener.onSwitchIme(false);
                    return true;
                });

                showUi(PreferenceUtils.getPrefMapEntryInt(prefs, res, R.string.mapAppToHeight, mAppId + "::" + mOrientation, mHeight / 20));

                mBImeDragHandle.setImageResource(R.drawable.ic_baseline_drag_handle_24);

                mBImeDragHandle.setOnTouchListener(new View.OnTouchListener() {
                    int mDownY;
                    int mMoveY;
                    int mParamsHeight;

                    @Override
                    public boolean onTouch(View view, MotionEvent evt) {
                        int y = (int) evt.getRawY();

                        switch (evt.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                ViewGroup.LayoutParams paramsDown = mRlMiddle.getLayoutParams();
                                mParamsHeight = paramsDown.height;
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                                mDownY = y;
                                break;
                            case MotionEvent.ACTION_MOVE:
                                int dMoveY = mMoveY - y;
                                // Do not react to small moves
                                if (dMoveY > 5 || dMoveY < -5) {
                                    mMoveY = y;
                                    int dDownY = mDownY - y;
                                    boolean isModeChange = showUi(mParamsHeight + dDownY);
                                    if (isModeChange) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                    }
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                ViewGroup.LayoutParams paramsUp = mRlMiddle.getLayoutParams();
                                PreferenceUtils.putPrefMapEntry(prefs, res, R.string.mapAppToHeight, mAppId + "::" + mOrientation, paramsUp.height);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
            }
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

        // TODO: move to utilities (48dp for the edges)
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int edge = Math.round(48 * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        if (mSwipeType) {
            setOnTouchListener(new OnCursorTouchListener(edge) {
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
                    mBImeDragHandle.setVisibility(View.INVISIBLE);
                    mBImeAction.setVisibility(View.INVISIBLE);
                    if (mUiState != 0) {
                        setVisibility(mBUiMode, View.INVISIBLE);
                    }
                    if (mRlClipboard.getVisibility() == View.GONE) {
                        setVisibility(mCentralButtons, View.INVISIBLE);
                    } else {
                        setVisibility(mRlClipboard, View.INVISIBLE);
                    }
                    setVisibility(mComboSelectorView, View.INVISIBLE);
                    showMessage("");
                }

                @Override
                public void onUp() {
                    showMessage("");
                    mBImeKeyboard.setVisibility(View.VISIBLE);
                    mBImeDragHandle.setVisibility(View.VISIBLE);
                    mBImeAction.setVisibility(View.VISIBLE);
                    if (mUiState != 0) {
                        setVisibility(mBUiMode, View.VISIBLE);
                    }
                    if (mRlClipboard.getVisibility() == View.GONE) {
                        setVisibility(mCentralButtons, View.VISIBLE);
                    } else {
                        setVisibility(mRlClipboard, View.VISIBLE);
                    }
                    setVisibility(mComboSelectorView, View.VISIBLE);
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
            });
        }
        setGuiInitState(0);

        mComboSelectorView.click();
    }

    public void init(int keys, CallerInfo callerInfo, boolean swipeType, ComponentName app) {
        mSwipeType = swipeType;
        // These controls are optional (i.e. can be null),
        // except for mBImeStartStop (TODO: which should also be optional)
        mCentralButtons = findViewById(R.id.centralButtons);
        mBImeStartStop = findViewById(R.id.bImeStartStop);
        mBImeKeyboard = findViewById(R.id.bImeKeyboard);
        mBImeDragHandle = findViewById(R.id.bImeDragHandle);
        mBImeAction = findViewById(R.id.bImeAction);
        // TODO: rename to SmallMic or something similar
        mBUiMode = findViewById(R.id.bClipboard);
        mComboSelectorView = findViewById(R.id.vComboSelector);
        mTvInstruction = findViewById(R.id.tvInstruction);
        mTvMessage = findViewById(R.id.tvMessage);
        mRvClipboard = findViewById(R.id.rvClipboard);
        mRlClipboard = findViewById(R.id.rlClipboard);
        mRlMiddle = findViewById(R.id.rlMiddle);
        mRlBottomBar = findViewById(R.id.rlBottomBar);
        mLlEmpty = findViewById(R.id.empty);

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = getResources();

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mOrientation = display.getOrientation();
        mHeight = display.getHeight();
        Log.i("Display: orientation: " + mOrientation);
        Log.i("Display: height: " + mHeight);

        mApp = app;
        mAppId = mApp == null ? "" : mApp.flattenToShortString();

        if (mRvClipboard != null) {
            //mRvClipboard.setHasFixedSize(true);
            // TODO: make span count configurable
            mRvClipboard.setLayoutManager(new GridLayoutManager(context, getResources().getInteger(R.integer.spanCount)));
        }

        if (swipeType) {
            // Turning from GONE to VISIBLE
            findViewById(R.id.rlKeyButtons).setVisibility(View.VISIBLE);
        }

        showMessage("");

        TypedArray keysAsTypedArray = res.obtainTypedArray(keys);
        final int key = keysAsTypedArray.getResourceId(0, 0);
        int keyHelpText = keysAsTypedArray.getResourceId(7, 0);
        int defaultHelpText = keysAsTypedArray.getResourceId(8, 0);
        keysAsTypedArray.recycle();

        mComboSelectorView.init(context, prefs, keys, callerInfo, mAppId, key, (language, service) -> {
            if (mState == MicButton.State.RECORDING) {
                stopListening();
            }
            mListener.onComboChange(language, service);
            if (mRvClipboard != null) {
                updateClipboard(getContext(), language, service, mApp);
            }
        });

        updateServiceLanguage(mComboSelectorView.getSpeechRecognizer());

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
    }

    /**
     * Performs an action after a press on the mic button, and given a current state.
     */
    private void changeState() {
        Log.i("Microphone button pressed: state = " + mState);
        switch (mState) {
            case INIT:
            case ERROR:
                startListening(mComboSelectorView.getSpeechRecognizer(), mComboSelectorView.getIntent());
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

    public void start() {
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            // TODO: fix this
            startListening(mComboSelectorView.getSpeechRecognizer(), mComboSelectorView.getIntent());
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
                if (NEW_TAB_LABEL.equals(tab.getTag())) {
                    Intent intent = new Intent(getContext(), RewritesSelectorActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    mRvClipboard.setAdapter(getClipboardAdapter(prefs, res, name, commandMatcher));
                    setTabName(prefs, res, appId, name);
                }
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
        TabLayout.Tab tab = tabs.newTab();
        tab.setText(NEW_TAB_LABEL);
        tab.setTag(NEW_TAB_LABEL);
        tabs.addTab(tab, false);

        // If the previously selected rewrites table is not among the defaults anymore then
        // we select the first one (but do not save it).
        if (tabs.getSelectedTabPosition() == -1) {
            tabs.getTabAt(0).select();
        }

        LinearLayout tabStrip = (LinearLayout) tabs.getChildAt(0);
        // We exclude the NEW_TAB_LABEL
        for (int i = 0; i < tabStrip.getChildCount() - 1; i++) {
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

    /**
     * TODO: animate state change
     */
    private boolean showUi(int height) {
        // TODO: Hide IME if dragged to the bottom
        //if (height <= 0) {
        //    mListener.onAction(0, true);
        //    return false;
        //}
        // TODO: These constants should depend on the orientation
        final int mHeightSmall = mHeight / 30;
        final int mHeightLarge = mHeight / 5;

        if (height < 0) {
            height = 0;
        } else if (height >= mHeight) {
            height = mHeightLarge;
        }

        ViewGroup.LayoutParams params = mRlMiddle.getLayoutParams();
        params.height = height;
        mRlMiddle.setLayoutParams(params);

        int state;
        if (height >= mHeightLarge) {
            state = 1;
        } else if (height <= mHeightSmall) {
            state = 2;
        } else {
            state = 0;
        }
        if (state != mUiState) {
            mUiState = state;
            if (state == 1) {
                // Buttons
                mCentralButtons.setVisibility(View.GONE);
                mBUiMode.setVisibility(View.VISIBLE);
                mRlClipboard.setVisibility(View.VISIBLE);
                // On Watch, remove message bar and bottom bar to make more room for the buttons.
                if (getResources().getBoolean(R.bool.isWatch)) {
                    mTvMessage.setVisibility(View.GONE);
                    if (mRlBottomBar != null) {
                        mRlBottomBar.setVisibility(View.GONE);
                    }
                }
            } else if (state == 2) {
                mCentralButtons.setVisibility(View.GONE);
                mRlClipboard.setVisibility(View.GONE);
                mBUiMode.setVisibility(View.VISIBLE);
                mTvMessage.setVisibility(View.VISIBLE);
                if (mRlBottomBar != null) {
                    mRlBottomBar.setVisibility(View.VISIBLE);
                }
            } else {
                mRlClipboard.setVisibility(View.GONE);
                // mBUiMode should be not be shown but should still take place
                mBUiMode.setVisibility(View.INVISIBLE);
                mCentralButtons.setVisibility(View.VISIBLE);
                mTvMessage.setVisibility(View.VISIBLE);
                if (mRlBottomBar != null) {
                    mRlBottomBar.setVisibility(View.VISIBLE);
                }
            }
            //showMessage("[" + height + ", " + state + "]");
            //invalidate();
            mRlMiddle.invalidate();
            return true;
        }
        //showMessage("[" + height + "]");
        mRlMiddle.invalidate();
        return false;
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

        if (mBUiMode != null) {
            mBUiMode.setColorFilter(null);
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

    private void updateServiceLanguage(SpeechRecognizer sr) {
        cancelOrDestroy();
        mRecognizer = sr;
        mRecognizer.setRecognitionListener(new SpeechInputRecognitionListener());
    }

    private void startListening(SpeechRecognizer sr, Intent intent) {
        setGuiState(MicButton.State.WAITING);
        updateServiceLanguage(sr);
        try {
            mRecognizer.startListening(intent);
            mListener.onStartListening();
            // Increases the counter of the app that calls the recognition service.
            // TODO: we could define it slightly differently, e.g. only count successful recognitions,
            // count also commands executed via swipes and/or buttons (but maybe not count every deletion
            // and cursor movement).
            // TODO: we could also count languages, services, etc.
            PackageNameRegistry.increaseAppCount(getContext(), intent.getExtras(), null);
        } catch (SecurityException e) {
            // TODO: review this.
            // On Android 11 we get the SecurityException when calling "Kõnele service" and Kõnele does not
            // have the mic permission. This does not happen when calling Google's service. Instead
            // there is a callback to RecognitionListener.onError. We act here as if this callback happened.
            mListener.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
        }
    }

    /**
     * TODO: not sure if it is better to call cancel or destroy
     * Note that SpeechRecognizer#destroy calls cancel first.
     */
    private void cancelOrDestroy() {
        mBtnType = "Y";
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
            if (mBUiMode != null) {
                mBUiMode.setColorFilter(MicButton.COLOR_LISTENING);
            }
            mBtnType = "R";
            setText(mTvInstruction, R.string.buttonImeStop);
            showMessage("");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i("onBeginningOfSpeech: state = " + mState);
            setGuiState(MicButton.State.RECORDING);
            mBtnType = "R";
            setVisibility(findViewById(R.id.rlKeyButtons), View.INVISIBLE);
        }

        @Override
        public void onEndOfSpeech() {
            Log.i("onEndOfSpeech: state = " + mState);
            // We go into the TRANSCRIBING-state only if we were in the RECORDING-state,
            // otherwise we ignore this event. This improves compatibility with
            // Google Voice Search, which calls EndOfSpeech after onResults.
            if (mState == MicButton.State.RECORDING) {
                setGuiState(MicButton.State.TRANSCRIBING);
                if (mBUiMode != null) {
                    mBUiMode.setColorFilter(MicButton.COLOR_TRANSCRIBING);
                }
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
