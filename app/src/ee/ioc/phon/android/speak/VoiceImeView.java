package ee.ioc.phon.android.speak;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.message.BasicNameValuePair;

import java.util.Arrays;
import java.util.List;

public class VoiceImeView extends LinearLayout {

    interface VoiceImeViewListener {
        void onPartialResult(String text);

        void onFinalResult(String text);

        void onKeyboard();

        void onGo();

        void deleteLastWord();
    }

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeGo;
    private TextView mTvInstruction;
    private TextView mTvMessage;

    private VoiceImeViewListener mListener;
    private WebSocketRecognizer mRecognizer;
    private Context mContext;
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

        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setListener(EditorInfo attribute, final VoiceImeViewListener listener) {
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBImeGo = (ImageButton) findViewById(R.id.bImeGo);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvMessage = (TextView) findViewById(R.id.tvMessage);

        setGuiInitState(0);

        List<BasicNameValuePair> editorInfo = setEditorInfo(attribute);
        WebSocketRecognizer.Listener recognizerListener = getRecognizerListener();
        mRecognizer = new WebSocketRecognizer(getResources().getString(R.string.defaultWsService), recognizerListener, editorInfo);
        //setText(mTvMessage, editorInfo.toString());

        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mState == Constants.State.INIT) {
                    if (mRecognizer != null) mRecognizer.start();
                } else if (mState == Constants.State.RECORDING) {
                    if (mRecognizer != null) mRecognizer.stopRecording();
                } else if (mState == Constants.State.TRANSCRIBING) {
                    closeSession();
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
                mListener.onKeyboard();
            }
        });

        // Launch recognition immediately (if set so)
        if (mPrefs.getBoolean(getResources().getString(R.string.keyAutoStart),
                getResources().getBoolean(R.bool.defaultAutoStart))) {
            mRecognizer.start();
        }
    }


    private List<BasicNameValuePair> setEditorInfo(EditorInfo attribute) {
        String packageName = asString(attribute.packageName);
        return Arrays.asList(
                new BasicNameValuePair("action-label", asString(attribute.actionLabel)),
                new BasicNameValuePair("field-name", asString(attribute.fieldName)),
                new BasicNameValuePair("hint-text", asString(attribute.hintText)),
                new BasicNameValuePair("input-type", String.valueOf(attribute.inputType)),
                new BasicNameValuePair("label", asString(attribute.label)),
                new BasicNameValuePair("package-name", packageName),
                new BasicNameValuePair("user-agent",
                        Utils.makeUserAgentComment("K6nele",
                                Utils.getVersionName(mContext), packageName)),
                new BasicNameValuePair("user-id", Utils.getUniqueId(mPrefs))
        );
    }

    // TODO: close the session here
    void closeSession() {
        if (mRecognizer != null) {
            mRecognizer.cancel();
            mRecognizer = null;
        }
        setGuiInitState(0);
    }


    WebSocketRecognizer.Listener getRecognizerListener() {
        return new WebSocketRecognizer.Listener() {

            @Override
            public void onRecordingBegin() {
                Log.i("onRecordingBegin");
                mState = Constants.State.RECORDING;
                setMicButtonState(mBImeStartStop, mState);
                setText(mTvInstruction, R.string.buttonImeStop);
                setVisibility(mBImeKeyboard, View.INVISIBLE);
                setVisibility(mBImeGo, View.INVISIBLE);
            }

            @Override
            public void onRecordingDone() {
                Log.i("onRecordingDone");
                mState = Constants.State.TRANSCRIBING;
                setMicButtonState(mBImeStartStop, mState);
                setText(mTvInstruction, R.string.statusImeTranscribing);
            }

            @Override
            public void onError(final int errorCode) {
                Log.i("onError");
                if (mRecognizer != null) {
                    mRecognizer.cancel();
                    mRecognizer = null;
                }
                setMicButtonState(mBImeStartStop, Constants.State.ERROR);
                switch (errorCode) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        setGuiInitState(R.string.errorImeResultAudioError);
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        // TODO: fire this if no slots are available
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
            public void onPartialResult(final String text) {
                Log.i("onPartialResult");
                mListener.onPartialResult(text);
                setText(mTvMessage, lastChars(text, false));
            }

            @Override
            public void onFinalResult(final String text) {
                Log.i("onFinalResult");
                mListener.onFinalResult(text);
                setText(mTvMessage, lastChars(text, true));
            }

            @Override
            public void onFinish() {
                Log.i("onFinish");
                setGuiInitState(0);
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                setMicButtonVolumeLevel(mBImeStartStop, rmsdB);
            }
        };
    }

    private void setGuiInitState(int message) {
        mState = Constants.State.INIT;
        setMicButtonState(mBImeStartStop, mState);
        setText(mTvInstruction, R.string.buttonImeSpeak);
        if (message == 0) {
            setText(mTvMessage, "");
        } else {
            setText(mTvMessage, message);
        }
        setVisibility(mBImeKeyboard, View.VISIBLE);
        setVisibility(mBImeGo, View.VISIBLE);
    }

    private String lastChars(String str, boolean isFinal) {
        if (str == null) {
            str = "";
        } else {
            str = str.replaceAll("\\n", "↲");
        }
        if (isFinal) {
            return str + "■";
        }
        return str;
    }

    private void setText(final TextView textView, final CharSequence text) {
        if (textView != null) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                }
            });
        }
    }

    private void setText(final TextView textView, final int text) {
        if (textView != null) {
            textView.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(text);
                }
            });
        }
    }

    private void setMicButtonVolumeLevel(final MicButton button, final float rmsdB) {
        if (button != null) {
            button.post(new Runnable() {
                @Override
                public void run() {
                    button.setVolumeLevel(rmsdB);
                }
            });
        }
    }

    private void setMicButtonState(final MicButton button, final Constants.State state) {
        if (button != null) {
            button.post(new Runnable() {
                @Override
                public void run() {
                    button.setState(state);
                }
            });
        }
    }

    private void setVisibility(final View view, final int visibility) {
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(visibility);
                }
            });
        }
    }

    private String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof SpannableString) {
            SpannableString ss = (SpannableString) o;
            return ss.subSequence(0, ss.length()).toString();
        }
        return o.toString();
    }
}