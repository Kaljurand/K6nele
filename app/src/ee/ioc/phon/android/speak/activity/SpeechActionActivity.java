package ee.ioc.phon.android.speak.activity;

import android.Manifest;
import android.content.ComponentName;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speechutils.utils.PreferenceUtils;

/**
 * <p>This activity responds to the following intent types:</p>
 * <ul>
 * <li>android.speech.action.RECOGNIZE_SPEECH</li>
 * <li>android.speech.action.WEB_SEARCH</li>
 * </ul>
 * <p>We have tried to implement the complete interface of RecognizerIntent as of API level 7 (v2.1).</p>
 * <p/>
 * <p>It records audio, transcribes it using a speech-to-text server
 * and returns the result as a non-empty list of Strings.
 * In case of <code>android.intent.action.MAIN</code>,
 * it submits the recorded/transcribed audio to a web search.
 * It never returns an error code,
 * all the errors are processed within this activity.</p>
 * <p/>
 * <p>This activity rewrites the error codes which originally come from the
 * speech recognizer service
 * to the RecognizerIntent result error codes. The RecognizerIntent error codes are the
 * following (with my interpretation after the colon):</p>
 * <p/>
 * <ul>
 * <li>RESULT_AUDIO_ERROR: recording of the audio fails</li>
 * <li>RESULT_NO_MATCH: everything worked great just no transcription was produced</li>
 * <li>RESULT_NETWORK_ERROR: cannot reach the recognizer server
 * <ul>
 * <li>Network is switched off on the device</li>
 * <li>The recognizer webservice URL does not exist in the internet</li>
 * </ul>
 * </li>
 * <li>RESULT_SERVER_ERROR: server was reached but it denied service for some reason,
 * or produced results in a wrong format (i.e. maybe it provides a different service)</li>
 * <li>RESULT_CLIENT_ERROR: generic client error
 * <ul>
 * <li>The URLs of the recognizer webservice and/or the grammar were malformed</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Kaarel Kaljurand
 */
public class SpeechActionActivity extends AbstractRecognizerIntentActivity {

    private TextView mTvPrompt;
    private SpeechInputView mView;

    @Override
    void showError(String msg) {
        ((TextView) mView.findViewById(R.id.tvMessage)).setText(msg);
    }

    /**
     * <p>Only for developers, i.e. we are not going to localize these strings.</p>
     * TODO: fix
     */
    @Override
    String[] getDetails() {
        String callingActivityClassName = null;
        String callingActivityPackageName = null;
        String pendingIntentTargetPackage = null;
        ComponentName callingActivity = getCallingActivity();
        if (callingActivity != null) {
            callingActivityClassName = callingActivity.getClassName();
            callingActivityPackageName = callingActivity.getPackageName();
        }
        if (getExtraResultsPendingIntent() != null) {
            pendingIntentTargetPackage = getExtraResultsPendingIntent().getTargetPackage();
        }
        List<String> info = new ArrayList<>();
        info.add("ID: " + PreferenceUtils.getUniqueId(PreferenceManager.getDefaultSharedPreferences(this)));
        //info.add("User-Agent comment: " + getRecSessionBuilder().getUserAgentComment());
        info.add("Calling activity class name: " + callingActivityClassName);
        info.add("Calling activity package name: " + callingActivityPackageName);
        info.add("Pending intent target package: " + pendingIntentTargetPackage);
        //info.add("Selected grammar: " + getRecSessionBuilder().getGrammarUrl());
        //info.add("Selected target lang: " + getRecSessionBuilder().getGrammarTargetLang());
        //info.add("Selected server: " + getRecSessionBuilder().getServerUrl());
        info.add("Intent action: " + getIntent().getAction());
        info.addAll(Utils.ppBundle(getExtras()));
        return info.toArray(new String[info.size()]);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpActivity(R.layout.activity_recognizer);
        mTvPrompt = (TextView) findViewById(R.id.tvPrompt);
    }

    @Override
    public void onStart() {
        super.onStart();

        clearAudioBuffer();
        setUpExtras();
        registerPrompt(mTvPrompt);
        setTvPrompt();

        setUpSettingsButton();

        mView = (SpeechInputView) findViewById(R.id.vVoiceImeView);
        CallerInfo callerInfo = new CallerInfo(getExtras(), getCallingActivity());
        // TODO: do we need to send the ComponentName of the calling activity instead
        mView.init(R.array.keysActivity, callerInfo);
        mView.setListener(getSpeechInputViewListener());

        if (isAutoStart()) {
            Log.i("Auto-starting");
            mView.start();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // We stop the service unless a configuration change causes onStop(),
        // i.e. the service is not stopped because of rotation, but is
        // stopped if BACK or HOME is pressed, or the Settings-activity is launched.
        if (!isChangingConfigurations()) {
            mView.cancel();
        }
    }

    private SpeechInputView.SpeechInputViewListener getSpeechInputViewListener() {
        return new SpeechInputView.SpeechInputViewListener() {

            @Override
            public void onPartialResult(List<String> results) {
                // Ignore the partial results
            }

            @Override
            public void onFinalResult(List<String> results, Bundle bundle) {
                if (results != null && results.size() > 0) {
                    returnOrForwardMatches(results);
                }
            }

            @Override
            public void onSwitchIme(boolean isAskUser) {
                // Not applicable
            }

            @Override
            public void onGo() {
                // Not applicable
            }

            @Override
            public void onDeleteLastWord() {
                // Not applicable
            }

            @Override
            public void onAddNewline() {
                // Not applicable
            }

            @Override
            public void onAddSpace() {
                // Not applicable
            }

            @Override
            public void onSelectAll() {
                // Not applicable
            }

            @Override
            public void onReset() {
                // Not applicable
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.i("Activity: onBufferReceived: " + buffer.length);
                addToAudioBuffer(buffer);
            }

            @Override
            public void onError(int errorCode) {
                if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    ActivityCompat.requestPermissions(SpeechActionActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORD_AUDIO);
                } else {
                    setResultError(errorCode);
                }
            }
        };
    }
}
