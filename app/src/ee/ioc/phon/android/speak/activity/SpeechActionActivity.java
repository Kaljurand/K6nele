package ee.ioc.phon.android.speak.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.utils.Utils;
import ee.ioc.phon.android.speak.view.AbstractSpeechInputViewListener;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.TtsProvider;
import ee.ioc.phon.android.speechutils.editor.UtteranceRewriter;
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

    public static String[] HEADER_REWRITES_COL2 = {"Utterance", "Replacement"};

    private UtteranceRewriter mUtteranceRewriter;
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
        // TODO: do not use the default dialog style when in multi window mode
        /*
        if (isInMultiWindowMode()) {
            setTheme(R.style.Theme_K6nele_NoActionBar);
        }
        */
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

        String[] results = getExtras().getStringArray(Extras.EXTRA_RESULT_RESULTS);
        if (results == null) {
            if (hasVoicePrompt()) {
                sayVoicePrompt(new TtsProvider.Listener() {
                    @Override
                    public void onDone() {
                        start();
                    }
                });
            } else {
                start();
            }
        } else {
            handleResults(Arrays.asList(results));
        }
    }


    private void start() {
        if (isAutoStart()) {
            // TODO: test what happens if the view is started while TTS is running
            // and then started again when the TTS stops and calls onDone
            mView.post(new Runnable() {
                @Override
                public void run() {
                    mView.start();
                }
            });
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

        stopTts();
    }

    /**
     * Rewrite results based on EXTRAs.
     * First, the utterance-replacement pair (if exists) is applied to the results.
     * Then, the complete rewrite table (with a header) (if exists) is applied to the results.
     */
    private List<String> rewriteResultsWithExtras(List<String> results) {
        Bundle extras = getExtras();
        String rewritesAsStr = extras.getString(Extras.EXTRA_RESULT_REWRITES_AS_STR, null);
        String utterance = extras.getString(Extras.EXTRA_RESULT_UTTERANCE, null);
        String replacement = extras.getString(Extras.EXTRA_RESULT_REPLACEMENT, null);
        if (utterance != null && replacement != null) {
            toast(utterance + "->" + replacement);
            UtteranceRewriter utteranceRewriter = new UtteranceRewriter(utterance + "\t" + replacement, HEADER_REWRITES_COL2);
            results = utteranceRewriter.rewrite(results);
        }
        if (rewritesAsStr != null) {
            UtteranceRewriter utteranceRewriter = new UtteranceRewriter(rewritesAsStr);
            results = utteranceRewriter.rewrite(results);
        }
        return results;
    }

    public void handleResults(List<String> results) {
        if (results != null && results.size() > 0) {
            List<String> newResults = rewriteResultsWithExtras(results);
            if (mUtteranceRewriter == null) {
                returnOrForwardMatches(newResults);
            } else {
                returnOrForwardMatches(mUtteranceRewriter.rewrite(newResults));
            }
        }
    }

    private SpeechInputView.SpeechInputViewListener getSpeechInputViewListener() {
        return new AbstractSpeechInputViewListener() {

            @Override
            public void onComboChange(String language, ComponentName service) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SpeechActionActivity.this);
                String[] rewrites = getExtras().getStringArray(Extras.EXTRA_RESULT_REWRITES);
                mUtteranceRewriter = Utils.getUtteranceRewriter(prefs, getResources(), rewrites, language, service, getCallingActivity());
            }

            @Override
            public void onFinalResult(List<String> results, Bundle bundle) {
                handleResults(results);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
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

            @Override
            public void onStartListening() {
                stopTts();
            }
        };
    }
}
