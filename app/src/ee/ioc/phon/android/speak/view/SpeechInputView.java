package ee.ioc.phon.android.speak.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.OnSwipeTouchListener;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.ServiceLanguageChooser;
import ee.ioc.phon.android.speak.activity.ComboSelectorActivity;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.model.Combo;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.utils.IntentUtils;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.view.MicButton;

public class SpeechInputView extends LinearLayout {

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private Button mBComboSelector;
    private TextView mTvInstruction;
    private TextView mTvMessage;

    private SpeechInputViewListener mListener;
    private SpeechRecognizer mRecognizer;
    private ServiceLanguageChooser mSlc;

    private MicButton.State mState;

    // TODO: make it an attribute
    private boolean mIsEnableSwipe = false;

    public interface SpeechInputViewListener {
        void onComboChange(String language, ComponentName service);

        void onPartialResult(List<String> text);

        void onFinalResult(List<String> text, Bundle bundle);

        /**
         * Switch to the next IME or ask the user to choose the IME.
         *
         * @param isAskUser
         */
        void onSwitchIme(boolean isAskUser);

        /**
         * Switch to the previous IME (the IME that launched this IME)
         */
        void onSwitchToLastIme();

        void onSearch();

        void onDeleteLastWord();

        void goUp();

        void goDown();

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

    public void setListener(final SpeechInputViewListener listener) {
        mListener = listener;
        ImageButton buttonSearch = (ImageButton) findViewById(R.id.bImeSearch);
        if (mBImeKeyboard != null && buttonSearch != null) {
            mBImeKeyboard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSwitchToLastIme();
                }
            });

            mBImeKeyboard.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mListener.onSwitchIme(false);
                    return true;
                }
            });

            buttonSearch.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelOrDestroy();
                    mListener.onSearch();
                }
            });

            buttonSearch.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // TODO: do something interesting
                    return true;
                }
            });
        }

        if (mIsEnableSwipe) {
            setOnTouchListener(new OnSwipeTouchListener(getContext()) {
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
            });
        }

        mListener.onComboChange(mSlc.getLanguage(), mSlc.getService());
    }

    public void init(int keys, CallerInfo callerInfo, boolean isEnableSwipe) {
        mIsEnableSwipe = isEnableSwipe;
        // These controls are optional, except for mBImeStartStop (TODO: which should also be optional)
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBComboSelector = (Button) findViewById(R.id.tvComboSelector);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvMessage = (TextView) findViewById(R.id.tvMessage);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = new ServiceLanguageChooser(getContext(), prefs, keys, callerInfo);
        if (mBComboSelector != null) {
            if (mSlc.size() > 1) {
                mBComboSelector.setVisibility(View.VISIBLE);
            } else {
                mBComboSelector.setVisibility(View.GONE);
            }
        }
        updateServiceLanguage(mSlc.getSpeechRecognizer());
        if (mBComboSelector != null) {
            updateComboSelector(mSlc);
        }
        showMessage("");
        setGuiInitState(0);

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

        // This button can be pressed in any state.
        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
        });

        if (mBComboSelector != null) {
            mBComboSelector.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mState == MicButton.State.RECORDING) {
                        stopListening();
                    }
                    mSlc.next();
                    mListener.onComboChange(mSlc.getLanguage(), mSlc.getService());
                    updateComboSelector(mSlc);
                }
            });

            mBComboSelector.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    cancelOrDestroy();
                    Context context = getContext();
                    Intent intent = new Intent(context, ComboSelectorActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("key", context.getString(key));
                    IntentUtils.startActivityIfAvailable(context, intent);
                    return true;
                }
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
        } else {
            setGuiState(MicButton.State.ERROR);
            showMessage(String.format(getResources().getString(R.string.labelSpeechInputViewMessage), getResources().getString(message)));
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
        if (textView != null) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                }
            });
        }
    }

    private static void setText(final TextView textView, final int text) {
        if (textView != null) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                }
            });
        }
    }

    private static void setMicButtonVolumeLevel(final MicButton button, final float rmsdB) {
        if (button != null) {
            button.post(new Runnable() {
                @Override
                public void run() {
                    button.setVolumeLevel(rmsdB);
                }
            });
        }
    }

    private static void setMicButtonState(final MicButton button, final MicButton.State state) {
        if (button != null) {
            button.post(new Runnable() {
                @Override
                public void run() {
                    button.setState(state);
                }
            });
        }
    }

    private static void setVisibility(final View view, final int visibility) {
        if (view != null && view.getVisibility() != View.GONE) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(visibility);
                }
            });
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
    }

    /**
     * TODO: not sure if its better to call cancel or destroy
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
            setText(mTvInstruction, R.string.buttonImeStop);
            showMessage("");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i("onBeginningOfSpeech: state = " + mState);
            setGuiState(MicButton.State.RECORDING);
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
        }

        @Override
        public void onPartialResults(final Bundle bundle) {
            Log.i("onPartialResults: state = " + mState);
            ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (results != null && !results.isEmpty()) {
                // This can be true only with kaldi-gstreamer-server
                boolean isSemiFinal = bundle.getBoolean(Extras.EXTRA_SEMI_FINAL);
                showMessage(lastChars(results, isSemiFinal));
                if (isSemiFinal) {
                    mListener.onFinalResult(results, bundle);
                } else {
                    mListener.onPartialResult(results);
                }
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