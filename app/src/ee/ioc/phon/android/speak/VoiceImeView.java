package ee.ioc.phon.android.speak;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.message.BasicNameValuePair;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import kaldi.speechkit.Recognizer;
import kaldi.speechkit.Result;
import kaldi.speechkit.SpeechKit;

public class VoiceImeView extends LinearLayout {

    interface VoiceImeViewListener {
        void onPartialResult(String text);

        void onFinalResult(String text);

        void onKeyboard();

        void onGo();
    }

    private MicButton mBImeStartStop;
    private ImageButton mBImeKeyboard;
    private ImageButton mBImeGo;
    private TextView mTvInstruction;
    private TextView mTvErrorMessage;

    private VoiceImeViewListener mListener;
    private Recognizer mCurrentRecognizer;
    private SpeechKit mSpeechKit;

    private Constants.State mState = Constants.State.INIT;

    public VoiceImeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(EditorInfo attribute, final VoiceImeViewListener listener) {
        mListener = listener;
        mBImeStartStop = (MicButton) findViewById(R.id.bImeStartStop);
        mBImeKeyboard = (ImageButton) findViewById(R.id.bImeKeyboard);
        mBImeGo = (ImageButton) findViewById(R.id.bImeGo);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);

        setText(mTvInstruction, R.string.buttonImeSpeak);
        setVisibility(mBImeKeyboard, View.VISIBLE);
        setVisibility(mBImeGo, View.VISIBLE);
        setText(mTvErrorMessage, "");
        List<BasicNameValuePair> editorInfo = setEditorInfo(attribute);
        mSpeechKit = SpeechKit.initialize(getResources().getString(R.string.defaultWsService), editorInfo);
        //setText(mTvErrorMessage, editorInfo.toString());

        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mState == Constants.State.INIT) {
                    startSession();
                } else if (mState == Constants.State.RECORDING) {
                    mCurrentRecognizer.stopRecording();
                } else if (mState == Constants.State.ERROR) {
                    startSession();
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

        // TODO: launch recognition immediately
    }

    private void startSession() {
        if (mSpeechKit != null) {
            Recognizer.Listener recognizerListener = getRecognizerListener();
            if (mCurrentRecognizer == null) {
                // TODO: the language code is currently ignored
                mCurrentRecognizer = mSpeechKit.createRecognizer("et_EE", recognizerListener);
            } else {
                mCurrentRecognizer.setListener(recognizerListener);
            }

            mCurrentRecognizer.start();
        }
    }

    private List<BasicNameValuePair> setEditorInfo(EditorInfo attribute) {
        return Arrays.asList(
                new BasicNameValuePair("actionLabel", asString(attribute.actionLabel)),
                new BasicNameValuePair("fieldName", asString(attribute.fieldName)),
                new BasicNameValuePair("hintText", asString(attribute.hintText)),
                new BasicNameValuePair("inputType", String.valueOf(attribute.inputType)),
                new BasicNameValuePair("label", asString(attribute.label)),
                new BasicNameValuePair("packageName", asString(attribute.packageName))
        );
    }

    void closeSession() {
        if (mCurrentRecognizer != null) {
            mCurrentRecognizer.cancel();
            mCurrentRecognizer = null;
        }
    }


    Recognizer.Listener getRecognizerListener() {
        return new Recognizer.Listener() {

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

            /**
             * TODO: review the error conditions
             */
            @Override
            public void onError(final Exception error) {
                Log.i("onError");
                mState = Constants.State.ERROR;
                Log.e(error.getMessage());
                if (error instanceof UnknownHostException) {
                    setMicButtonState(mBImeStartStop, mState);
                    setText(mTvInstruction, R.string.errorImeResultNetworkError);
                } else {
                    setText(mTvErrorMessage, lastChars(error.getMessage(), false));
                }
            }

            @Override
            public void onPartialResult(Result text) {
                Log.i("onPartialResult");
                final String str = text.getText();
                mListener.onPartialResult(str);
                setText(mTvErrorMessage, lastChars(str, false));
            }

            @Override
            public void onFinalResult(Result text) {
                Log.i("onFinalResult");
                final String str = text.getText();
                mListener.onFinalResult(str);
                setText(mTvErrorMessage, lastChars(str, true));
            }

            @Override
            public void onFinish(final String reason) {
                Log.i("onFinish");
                mState = Constants.State.INIT;
                setMicButtonState(mBImeStartStop, mState);
                setText(mTvInstruction, R.string.buttonImeSpeak);
                setText(mTvErrorMessage, lastChars(reason, false));
                setVisibility(mBImeKeyboard, View.VISIBLE);
                setVisibility(mBImeGo, View.VISIBLE);
            }
        };
    }

    private String lastChars(String str, boolean isFinal) {
        if (str == null) {
            str = "";
        } else {
            str = str.replaceAll("\\n", "↲");
        }
        if (isFinal) {
            return "[" + str + "]";
        }
        return str;
    }

    private void setText(final TextView textView, final CharSequence text) {
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void setText(final TextView textView, final int text) {
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }

    private void setMicButtonState(final MicButton button, final Constants.State state) {
        button.post(new Runnable() {
            @Override
            public void run() {
                button.setState(state);
            }
        });
    }

    private void setVisibility(final View view, final int visibility) {
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
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