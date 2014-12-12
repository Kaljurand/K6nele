package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

        if (mRecognizer == null) {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext(),
                    new ComponentName("ee.ioc.phon.android.speak",
                            "ee.ioc.phon.android.speak.WebSocketRecognizer"));
            mRecognizer.setRecognitionListener(getRecognizerListener());
        } else {
            mRecognizer.setRecognitionListener(getRecognizerListener());
            mRecognizer.cancel();
        }

        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.setEnabled(false);
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
        });

        // TODO: rather add it to the whole LinearLayout
        mTvInstruction.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mListener.onAddSpace();
                return true;
            }
        });
    }

    public void start() {
        if (mState == Constants.State.INIT || mState == Constants.State.ERROR) {
            // TODO: fix this
            //mBImeStartStop.setEnabled(false);
            //mRecognizer.startListening(mIntent);
        }
    }

    public void closeSession() {
        if (mRecognizer != null) {
            mRecognizer.cancel();
        }
    }

    private RecognitionListener getRecognizerListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.i("onReadyForSpeech");
                setGuiState(Constants.State.LISTENING);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i("onBeginningOfSpeech");
                setGuiState(Constants.State.RECORDING);
                setText(mTvInstruction, R.string.buttonImeStop);
                setText(mTvMessage, "");
                setVisibility(mBImeKeyboard, View.INVISIBLE);
                setVisibility(mBImeGo, View.INVISIBLE);
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("onEndOfSpeech");
                setGuiState(Constants.State.TRANSCRIBING);
                setText(mTvInstruction, R.string.statusImeTranscribing);
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
                Log.i("onPartialResults");
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                if (text != null) {
                    mListener.onPartialResult(text);
                    setText(mTvMessage, lastChars(text, false));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO: not sure how this can be generated by the service
            }

            @Override
            public void onResults(final Bundle bundle) {
                Log.i("onResults");
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                if (text != null) {
                    mListener.onFinalResult(text);
                    setText(mTvMessage, lastChars(text, true));
                }
                // If we are in the transcribing-state while receiving the final package,
                // then we assume that the socket will close soon, and we move to the INIT state.
                // TODO: there is no callback for the socket close event, otherwise this if-then
                // would not be needed.
                if (mState == Constants.State.TRANSCRIBING) {
                    setGuiInitState(0);
                } else if (text == null) {
                    // If we got empty results then assume that cancel was called
                    setGuiInitState(0);
                }
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
}