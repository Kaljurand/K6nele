package ee.ioc.phon.android.speak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class VoiceImeView extends LinearLayout {

    public interface VoiceImeViewListener {
        void onPartialResult(ArrayList<String> text);

        void onFinalResult(ArrayList<String> text);

        void onSwitchIme(boolean isAskUser);

        void onGo();

        void onDeleteLastWord();

        void onAddNewline();

        void onAddSpace();

        void onBufferReceived(byte[] buffer);
    }

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeGo;
    private Button mBComboSelector;
    private TextView mTvInstruction;
    private TextView mTvMessage;

    private VoiceImeViewListener mListener;
    private SpeechRecognizer mRecognizer;
    private ServiceLanguageChooser mSlc;

    private Constants.State mState;

    public VoiceImeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setListener(int keys, CallerInfo callerInfo, final VoiceImeViewListener listener) {
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBImeGo = (ImageButton) findViewById(R.id.bImeGo);
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
        int keyAudioCues = keysAsTypedArray.getResourceId(6, 0);
        int defaultAudioCues = keysAsTypedArray.getResourceId(7, 0);
        int keyHelpText = keysAsTypedArray.getResourceId(8, 0);
        int defaultHelpText = keysAsTypedArray.getResourceId(9, 0);
        keysAsTypedArray.recycle();

        mBImeStartStop.setAudioCuesEnabled(PreferenceUtils.getPrefBoolean(prefs, getResources(), keyAudioCues, defaultAudioCues));

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
                        mRecognizer.startListening(mSlc.getIntent());
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
                Utils.startActivityIfAvailable(context, intent);
                return true;
            }
        });

        if (mBImeKeyboard != null && mBImeGo != null) {
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

            mBImeGo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
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
            public void onDoubleTapMotion() {
                mListener.onAddSpace();
            }
        });
    }

    public void start() {
        if (mState == Constants.State.INIT || mState == Constants.State.ERROR) {
            // TODO: fix this
            mRecognizer.startListening(mSlc.getIntent());
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
                setVisibility(mBComboSelector, View.INVISIBLE);
                if (mBImeKeyboard != null && mBImeGo != null) {
                    setVisibility(mBImeKeyboard, View.INVISIBLE);
                    setVisibility(mBImeGo, View.INVISIBLE);
                }
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
                ArrayList<String> text = selectResults(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
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
                ArrayList<String> text = selectResults(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                if (text == null) {
                    // If we got empty results then assume that the session ended,
                    // e.g. cancel was called.
                    // TODO: return null?
                    mListener.onFinalResult(new ArrayList<String>());
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
                Log.i("View: onBufferReceived: " + buffer.length);
                mListener.onBufferReceived(buffer);
            }
        };
    }

    private static ArrayList<String> selectResults(ArrayList<String> results) {
        if (results == null || results.size() < 1) {
            return null;
        }
        return results;
    }

    private static String selectFirstResult(ArrayList<String> results) {
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
            String m = "[ " + getResources().getString(message) + " ]";
            setText(mTvMessage, m);
        }
        setVisibility(mBComboSelector, View.VISIBLE);

        if (mBImeKeyboard != null && mBImeGo != null) {
            setVisibility(mBImeKeyboard, View.VISIBLE);
            setVisibility(mBImeGo, View.VISIBLE);
        }
    }

    private static String lastChars(ArrayList<String> results, boolean isFinal) {
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
        if (view != null && view.getVisibility() != View.GONE) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(visibility);
                }
            });
        }
    }

    private void updateServiceLanguage(ServiceLanguageChooser slc) {
        // Cancel a possibly running service
        closeSession();
        Pair<String, String> pair = Utils.getLabel(getContext(), slc.getCombo());
        mBComboSelector.setText(pair.second + " · " + pair.first);
        mRecognizer = slc.getSpeechRecognizer();
        mRecognizer.setRecognitionListener(getRecognizerListener());
    }
}