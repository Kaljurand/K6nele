package ee.ioc.phon.android.speak;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.UnknownHostException;

import kaldi.speechkit.Recognizer;
import kaldi.speechkit.Result;
import kaldi.speechkit.SpeechKit;

public class VoiceImeView extends LinearLayout {

    interface VoiceImeViewListener {
        void onPartialResult(String text);
        void onFinalResult(String text);
    }

    private ImageButton mBImeStartStop;
    private TextView mTvInstruction;
    private TextView mTvErrorMessage;

    private VoiceImeViewListener mListener;
    private Recognizer mCurrentRecognizer;
    private SpeechKit mSpeechKit;

    private String addr = "bark.phon.ioc.ee";
    private int port = 82;

    public VoiceImeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSpeechKit = SpeechKit.initialize(context, "", "server", addr, port);
    }

    public void setListener(final VoiceImeViewListener listener) {
        mListener = listener;
        mBImeStartStop = (ImageButton) findViewById(R.id.bImeStartStop);
        mTvInstruction = (TextView) findViewById(R.id.tvInstruction);
        mTvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);

        mTvInstruction.post(new Runnable() {
            @Override
            public void run() {
                mTvInstruction.setText(R.string.buttonSpeak);
            }
        });
        mTvErrorMessage.post(new Runnable() {
            @Override
            public void run() {
                mTvErrorMessage.setText("");
            }
        });

        mBImeStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // TODO: clean this up
                if (mCurrentRecognizer != null && mCurrentRecognizer.isRecording()) {
                    mCurrentRecognizer.stopRecording();
                } else {
                    if (mSpeechKit != null) {
                        mSpeechKit.connect();
                    }

                    Recognizer.Listener recognizerListener = getRecognizerListener();
                    if (mCurrentRecognizer == null) {
                        // TODO: the language code is currently ignored
                        mCurrentRecognizer = mSpeechKit.createRecognizer("et_EE", recognizerListener);
                    } else {
                        mCurrentRecognizer.setListener(recognizerListener);
                    }

                    mCurrentRecognizer.start(); // Connect to server;
                }
            }
        });
    }

    void closing() {
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
                mTvInstruction.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvInstruction.setText(R.string.buttonStop);
                    }
                });
            }

            @Override
            public void onRecordingDone() {
                Log.i("onRecordingDone");
                mTvInstruction.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvInstruction.setText(R.string.statusTranscribing);
                    }
                });
            }

            @Override
            public void onError(final Exception error) {
                Log.i("onError");
                final String message;
                if (error instanceof UnknownHostException) {
                    message = "Server " + mSpeechKit.getHostAddr() + ":" + mSpeechKit.getPort() + " is not available";
                } else {
                    message = error.getMessage();
                }
                Log.e(error.getMessage());

                mTvInstruction.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvInstruction.setText(R.string.buttonSpeak);
                    }
                });

                mTvErrorMessage.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvErrorMessage.setText(lastChars(message, 30));
                    }
                });
            }

            @Override
            public void onPartialResult(Result text) {
                Log.i("onPartialResult");
                final String str = text.getText();
                mListener.onPartialResult(str);
                mTvErrorMessage.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvErrorMessage.setText(lastChars(str, 25));
                    }
                });
            }

            @Override
            public void onFinalResult(Result text) {
                Log.i("onFinalResult");
                final String str = text.getText();
                mListener.onFinalResult(str);
                mTvErrorMessage.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvErrorMessage.setText("[" + lastChars(str, 30) + "]");
                    }
                });
            }

            @Override
            public void onFinish(final String reason) {
                Log.i("onFinish");
                mTvInstruction.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvInstruction.setText(R.string.buttonSpeak);
                    }
                });

                mTvErrorMessage.post(new Runnable() {
                    @Override
                    public void run() {
                        mTvErrorMessage.setText(lastChars(reason, 30));
                    }
                });
            }
        };
    }

    private String lastChars(String str, int len) {
        if (str == null) {
            return "...";
        }
        return "..." + str.substring(str.length() - Math.min(len, str.length())).replaceAll("\\n", "<N>");
    }
}