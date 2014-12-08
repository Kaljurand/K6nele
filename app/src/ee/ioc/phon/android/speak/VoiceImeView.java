package ee.ioc.phon.android.speak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

        void deleteLastWord();
    }

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeGo;
    private TextView mTvInstruction;
    private TextView mTvMessage;

    private VoiceImeViewListener mListener;
    private SpeechRecognizer mRecognizer;
    private SharedPreferences mPrefs;

    private Constants.State mState;

    public VoiceImeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeLeft() {
                mListener.deleteLastWord();
            }

            @Override
            public void onSwipeRight() {
                mListener.onFinalResult("\n");
            }
        });

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setListener(final EditorInfo attribute, final VoiceImeViewListener listener) {
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBImeGo = (ImageButton) findViewById(R.id.bImeGo);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvMessage = (TextView) findViewById(R.id.tvMessage);

        setGuiInitState(0);

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext(),
                new ComponentName("ee.ioc.phon.android.speak",
                        "ee.ioc.phon.android.speak.WebSocketRecognizer"));

        mRecognizer.setRecognitionListener(getRecognizerListener());

        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("mBImeStartStop.setOnClickListener: " + mState);
                if (mState == Constants.State.INIT || mState == Constants.State.ERROR) {
                    mRecognizer.startListening(getRecognizerIntent(getContext(), attribute));
                } else if (mState == Constants.State.RECORDING) {
                    mRecognizer.stopListening();
                } else if (mState == Constants.State.TRANSCRIBING) {
                    mRecognizer.cancel();
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

        // Launch recognition immediately (if set so)
        if (Utils.getPrefBoolean(mPrefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart)) {
            mRecognizer.startListening(getRecognizerIntent(getContext(), attribute));
        }
    }

    public void closeSession() {
        if (mRecognizer != null) mRecognizer.cancel();
    }

    private RecognitionListener getRecognizerListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.i("onReadyForSpeech");
                setGuiInitState(0);
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i("onBeginningOfSpeech");
                mState = Constants.State.RECORDING;
                setMicButtonState(mBImeStartStop, mState);
                setText(mTvInstruction, R.string.buttonImeStop);
                setText(mTvMessage, "");
                setVisibility(mBImeKeyboard, View.INVISIBLE);
                setVisibility(mBImeGo, View.INVISIBLE);
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("onEndOfSpeech");
                mState = Constants.State.TRANSCRIBING;
                setMicButtonState(mBImeStartStop, mState);
                setText(mTvInstruction, R.string.statusImeTranscribing);
            }

            @Override
            public void onError(final int errorCode) {
                Log.i("onError");

                setMicButtonState(mBImeStartStop, Constants.State.ERROR);
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
                    default:
                        setGuiInitState(R.string.errorImeResultClientError);
                }
            }

            @Override
            public void onPartialResults(final Bundle bundle) {
                Log.i("onPartialResults");
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                mListener.onPartialResult(text);
                setText(mTvMessage, lastChars(text, false));
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // TODO: not sure how this can be generated by the service
            }

            @Override
            public void onResults(final Bundle bundle) {
                Log.i("onResults");
                String text = selectSingleResult(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                mListener.onFinalResult(text);
                setText(mTvMessage, lastChars(text, true));

                // If we are in the transcribing-state while receiving the final package,
                // then we assume that the socket will close soon, and we move to the INIT state.
                // TODO: there is no callback for the socket close event, otherwise this if-then
                // would not be needed.
                if (mState == Constants.State.TRANSCRIBING) {
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
            return "";
        }
        return results.get(0);
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

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof SpannableString) {
            SpannableString ss = (SpannableString) o;
            return ss.subSequence(0, ss.length()).toString();
        }
        return o.toString();
    }

    private static Intent getRecognizerIntent(Context context, EditorInfo attribute) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        intent.putExtra(Extras.EXTRA_UNLIMITED_DURATION, true);
        intent.putExtra(Extras.EXTRA_EDITOR_INFO, toBundle(attribute));
        return intent;
    }

    private static Bundle toBundle(EditorInfo attribute) {
        Bundle bundle = new Bundle();
        bundle.putBundle("extras", attribute.extras);
        bundle.putString("actionLabel", asString(attribute.actionLabel));
        bundle.putString("fieldName", asString(attribute.fieldName));
        bundle.putString("hintText", asString(attribute.hintText));
        bundle.putString("inputType", String.valueOf(attribute.inputType));
        bundle.putString("label", asString(attribute.label));
        // This line gets the actual caller package registered in the package registry.
        // The key needs to be "packageName".
        bundle.putString("packageName", asString(attribute.packageName));
        return bundle;
    }
}