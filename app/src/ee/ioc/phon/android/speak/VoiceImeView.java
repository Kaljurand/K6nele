package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class VoiceImeView extends LinearLayout {

    interface VoiceImeViewListener {
        void onPartialResult(String text);

        void onFinalResult(String text);

        void onSwitchIme(boolean isAskUser);

        void onGo();

        void onDeleteLastWord();

        void onAddNewline();

        void onAddSpace();
    }

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeGo;
    private TextView mTvInstruction;
    private TextView mTvMessage;

    private VoiceImeViewListener mListener;
    private SpeechRecognizer mRecognizer;

    private Intent mIntent;
    private Constants.State mState;

    public VoiceImeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Intent intent, final VoiceImeViewListener listener) {
        mIntent = intent;
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBImeGo = (ImageButton) findViewById(R.id.bImeGo);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvMessage = (TextView) findViewById(R.id.tvMessage);

        setGuiInitState(0);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mBImeStartStop.setAudioCuesEnabled(Utils.getPrefBoolean(prefs, getResources(), R.string.keyImeAudioCues, R.bool.defaultImeAudioCues));

        if (Utils.getPrefBoolean(prefs, getResources(), R.string.keyImeHelpText, R.bool.defaultImeHelpText)) {
            mTvInstruction.setVisibility(View.VISIBLE);
        } else {
            mTvInstruction.setVisibility(View.GONE);
        }

        // Cancel a possibly running service and start a new one
        closeSession();
        mRecognizer = createSpeechRecognizer(prefs);
        mRecognizer.setRecognitionListener(getRecognizerListener());

        // This button can be pressed in any state.
        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("Microphone button pressed: state = " + mState);
                switch (mState) {
                    case INIT:
                    case ERROR:
                        mRecognizer.startListening(mIntent);
                        break;
                    case RECORDING:
                        mRecognizer.stopListening();
                        break;
                    case LISTENING:
                    case TRANSCRIBING:
                        // We enter the INIT-state here, just in case cancel() does not take us there
                        setGuiInitState(0);
                        mRecognizer.cancel();
                        break;
                    default:
                }
            }
        });

        mBImeGo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onGo();
            }
        });

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
            public void onDoubleTapMotion() {
                mListener.onAddSpace();
            }
        });
    }

    public void start() {
        if (mState == Constants.State.INIT || mState == Constants.State.ERROR) {
            // TODO: fix this
            //mRecognizer.startListening(mIntent);
        }
    }

    public void closeSession() {
        if (mRecognizer != null) {
            mRecognizer.cancel();
            // TODO: maybe set to null
        }
    }

    private RecognitionListener getRecognizerListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.i("onReadyForSpeech: state = " + mState);
                setGuiState(Constants.State.LISTENING);
                setText(mTvInstruction, R.string.buttonImeStop);
                setText(mTvMessage, "");
                setVisibility(mBImeKeyboard, View.INVISIBLE);
                setVisibility(mBImeGo, View.INVISIBLE);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i("onBeginningOfSpeech: state = " + mState);
                setGuiState(Constants.State.RECORDING);
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("onEndOfSpeech: state = " + mState);
                // We go into the TRANSCRIBING-state only if we were in the RECORDING-state,
                // otherwise we ignore this event. This improves compatibility with
                // Google Voice Search, which calls EndOfSpeech after onResults.
                if (mState == Constants.State.RECORDING) {
                    setGuiState(Constants.State.TRANSCRIBING);
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
                setGuiState(Constants.State.ERROR);

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
                }
            }

            @Override
            public void onPartialResults(final Bundle bundle) {
                Log.i("onPartialResults: state = " + mState);
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                if (text == null) {
                    // This shouldn't really happen
                } else {
                    // This can be true only with kaldi-gstreamer-server
                    boolean isSemiFinal = bundle.getBoolean(Extras.EXTRA_SEMI_FINAL);
                    if (isSemiFinal) {
                        mListener.onFinalResult(text);
                    } else {
                        mListener.onPartialResult(text);
                    }
                    setText(mTvMessage, lastChars(text, isSemiFinal));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO: future work: not sure how this can be generated by the service
            }

            @Override
            public void onResults(final Bundle bundle) {
                Log.i("onResults: state = " + mState);
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                if (text == null) {
                    // If we got empty results then assume that the session ended,
                    // e.g. cancel was called.
                    mListener.onFinalResult("");
                } else {
                    mListener.onFinalResult(text);
                    setText(mTvMessage, lastChars(text, true));
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
                // TODO: future work
            }
        };
    }

    private static String selectSingleResult(ArrayList<String> results) {
        if (results == null || results.size() < 1) {
            return null;
        }
        return results.get(0);
    }

    private void setGuiState(Constants.State state) {
        mState = state;
        setMicButtonState(mBImeStartStop, mState);
    }

    private void setGuiInitState(int message) {
        mState = Constants.State.INIT;
        setMicButtonState(mBImeStartStop, mState);
        setText(mTvInstruction, R.string.buttonImeSpeak);
        if (message == 0) {
            // Do not clear a possible error message
            //setText(mTvMessage, "");
        } else {
            setText(mTvMessage, message);
        }
        setVisibility(mBImeKeyboard, View.VISIBLE);
        setVisibility(mBImeGo, View.VISIBLE);
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

    private static void setMicButtonState(final MicButton button, final Constants.State state) {
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
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(visibility);
                }
            });
        }
    }

    /**
     * Constructs SpeechRecognizer based on the user settings.
     * 1. If the user has selected no service then return K6nele(fast)
     * 2. If the user has selected "default" then return the system default
     * 3. Otherwise return the service that the user has selected
     *
     * @return SpeechRecognizer
     */
    private SpeechRecognizer createSpeechRecognizer(SharedPreferences prefs) {
        String selectedRecognizerService =
                Utils.getPrefString(prefs, getResources(), R.string.keyImeRecognitionService);

        if (selectedRecognizerService == null) {
            selectedRecognizerService = getResources().getString(R.string.defaultImeRecognizerService);
        } else if (selectedRecognizerService.equals(getResources().getString(R.string.keyDefaultRecognitionService))) {
            return SpeechRecognizer.createSpeechRecognizer(getContext());
        }

        String[] pkgAndCls = selectedRecognizerService.split("\\|");

        return SpeechRecognizer.createSpeechRecognizer(getContext(), new ComponentName(pkgAndCls[0], pkgAndCls[1]));
    }
}