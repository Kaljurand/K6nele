package ee.ioc.phon.android.speak.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import ee.ioc.phon.android.speak.ComboSelectorActivity;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.OnSwipeTouchListener;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.ServiceLanguageChooser;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.service.UtteranceRewriter;
import ee.ioc.phon.android.speak.utils.IntentUtils;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.RecognitionServiceManager;
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

    public interface SpeechInputViewListener {
        void onPartialResult(List<String> text);

        void onFinalResult(List<String> text);

        void onSwitchIme(boolean isAskUser);

        void onGo();

        void onDeleteLastWord();

        void onAddNewline();

        void onAddSpace();

        void onSelectAll();

        void onReset();

        void onBufferReceived(byte[] buffer);
    }

    public SpeechInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setListener(int keys, CallerInfo callerInfo, final SpeechInputViewListener listener) {
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBComboSelector = (Button) findViewById(R.id.tvComboSelector);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvMessage = (TextView) findViewById(R.id.tvMessage);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // TODO: check for null? (test by deinstalling a recognizer but not changing K6nele settings)
        mSlc = new ServiceLanguageChooser(getContext(), prefs, keys, callerInfo);
        if (mSlc.size() > 1) {
            mBComboSelector.setVisibility(View.VISIBLE);
        } else {
            mBComboSelector.setVisibility(View.GONE);
        }
        updateServiceLanguage(mSlc);

        setText(mTvMessage, "");
        setGuiInitState(0);

        TypedArray keysAsTypedArray = getResources().obtainTypedArray(keys);
        final int key = keysAsTypedArray.getResourceId(0, 0);
        int keyHelpText = keysAsTypedArray.getResourceId(8, 0);
        int defaultHelpText = keysAsTypedArray.getResourceId(9, 0);
        keysAsTypedArray.recycle();

        if (PreferenceUtils.getPrefBoolean(prefs, getResources(), keyHelpText, defaultHelpText)) {
            mTvInstruction.setVisibility(View.VISIBLE);
        } else {
            mTvInstruction.setVisibility(View.GONE);
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
                        break;
                    default:
                }
            }
        });

        mBComboSelector.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlc.next();
                updateServiceLanguage(mSlc);
            }
        });

        mBComboSelector.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Context context = getContext();
                Intent intent = new Intent(context, ComboSelectorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("key", getContext().getString(key));
                IntentUtils.startActivityIfAvailable(context, intent);
                return true;
            }
        });

        ImageButton buttonGo = (ImageButton) findViewById(R.id.bImeGo);
        if (mBImeKeyboard != null && buttonGo != null) {
            mBImeKeyboard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onSwitchIme(false);
                }
            });

            mBImeKeyboard.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mListener.onSwitchIme(true);
                    return true;
                }
            });

            buttonGo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelOrDestroy();
                    mListener.onGo();
                }
            });
        }


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

    public void start() {
        if (mState == MicButton.State.INIT || mState == MicButton.State.ERROR) {
            // TODO: fix this
            mRecognizer.startListening(mSlc.getIntent());
        }
    }

    public void closeSession() {
        cancelOrDestroy();
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
            //setText(mTvMessage, "");
            setGuiState(MicButton.State.INIT);
        } else {
            setGuiState(MicButton.State.ERROR);
            setText(mTvMessage, String.format(getResources().getString(R.string.labelSpeechInputViewMessage), getResources().getString(message)));
        }
        setEnabled(mBComboSelector, true);
        if (mBImeKeyboard != null) {
            setVisibility(mBImeKeyboard, View.VISIBLE);
        }
        setText(mTvInstruction, R.string.buttonImeSpeak);
    }

    private static String lastChars(List<String> results, boolean isFinal) {
        String str = selectFirstResult(results);
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

    private static void setEnabled(final View view, final boolean enabled) {
        if (view != null && view.isEnabled() != enabled) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setEnabled(enabled);
                    if (view instanceof TextView) {
                        if (enabled) {
                            ((TextView) view).setTextColor(view.getResources().getColor(R.color.grey100));
                        } else {
                            ((TextView) view).setTextColor(view.getResources().getColor(R.color.grey400));
                        }
                    }
                }
            });
        }
    }

    private void updateServiceLanguage(ServiceLanguageChooser slc) {
        // Cancel a possibly running service
        cancelOrDestroy();
        Pair<String, String> pair = RecognitionServiceManager.getLabel(getContext(), slc.getCombo());
        mBComboSelector.setText(String.format(getResources().getString(R.string.labelComboItem), pair.first, pair.second));
        mRecognizer = slc.getSpeechRecognizer();
        mRecognizer.setRecognitionListener(new SpeechInputRecognitionListener());
    }

    private void startListening(ServiceLanguageChooser slc) {
        setGuiState(MicButton.State.WAITING);
        if (mRecognizer == null) {
            updateServiceLanguage(slc);
        }
        mRecognizer.startListening(slc.getIntent());
    }

    private void stopListening() {
        if (mRecognizer != null) {
            mRecognizer.stopListening();
        }
    }

    /**
     * TODO: not sure if its better to call cancel or destroy
     * Note that SpeechRecognizer#destroy calls cancel first.
     */
    private void cancelOrDestroy() {
        // We enter the INIT-state here, just in case cancel or destroy does not take us there
        setGuiInitState(0);
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
    }


    private UtteranceRewriter getUtteranceRewriter(SharedPreferences prefs) {
        if (PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyRewrite, R.bool.defaultRewrite)) {
            return new UtteranceRewriter(PreferenceUtils.getPrefString(prefs, getResources(), R.string.keyRewritesFile, R.string.empty));
        }
        return new UtteranceRewriter();
    }


    private class SpeechInputRecognitionListener implements RecognitionListener {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final UtteranceRewriter utteranceRewriter = getUtteranceRewriter(prefs);

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.i("onReadyForSpeech: state = " + mState);
            setGuiState(MicButton.State.LISTENING);
            setText(mTvInstruction, R.string.buttonImeStop);
            setText(mTvMessage, "");
            setEnabled(mBComboSelector, false);
            if (mBImeKeyboard != null) {
                setVisibility(mBImeKeyboard, View.INVISIBLE);
            }
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

                    Context context = getContext();
                    if (context instanceof Activity) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                0);
                    } else {
                        // TODO: Create a notification that asks for the permission
                        Log.i("context = " + context.getClass());
                    }
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
            List<String> resultsRewritten = utteranceRewriter.rewrite(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            if (!resultsRewritten.isEmpty()) {
                // This can be true only with kaldi-gstreamer-server
                boolean isSemiFinal = bundle.getBoolean(Extras.EXTRA_SEMI_FINAL);
                setText(mTvMessage, lastChars(resultsRewritten, isSemiFinal));
                if (isSemiFinal) {
                    mListener.onFinalResult(resultsRewritten);
                } else {
                    mListener.onPartialResult(resultsRewritten);
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
            List<String> resultsRewritten = utteranceRewriter.rewrite(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
            if (resultsRewritten.isEmpty()) {
                // If we got empty results then assume that the session ended,
                // e.g. cancel was called.
                mListener.onFinalResult(Collections.<String>emptyList());
            } else {
                setText(mTvMessage, lastChars(resultsRewritten, true));
                mListener.onFinalResult(resultsRewritten);
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