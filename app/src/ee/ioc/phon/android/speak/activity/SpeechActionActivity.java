package ee.ioc.phon.android.speak.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RawAudioRecorder;
import ee.ioc.phon.android.speak.Utils;
import ee.ioc.phon.android.speak.view.SpeechInputView;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

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
    private int mSampleRate;
    private byte[] mCompleteRecording;

    private SpeechInputView mView;

    void showError() {
        // TODO
    }

    Uri getAudioUri() {
        try {
            byte[] wav = RawAudioRecorder.getRecordingAsWav(mCompleteRecording, mSampleRate);
            FileOutputStream fos = openFileOutput(Constants.AUDIO_FILENAME, Context.MODE_PRIVATE);
            fos.write(wav);
            fos.close();

            return Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + Constants.AUDIO_FILENAME);
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("IOException: " + e.getMessage());
        }
        return null;
    }

    /**
     * <p>Only for developers, i.e. we are not going to localize these strings.</p>
     * TODO: fix
     */
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
    }

    @Override
    public void onStart() {
        super.onStart();

        setUpExtras();
        setTvPrompt((TextView) findViewById(R.id.tvPrompt));

        // Launch recognition immediately (if set so).
        // Auto-start only occurs is onCreate is called
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAutoStart =
                isAutoStartAction(getIntent().getAction())
                        || PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart);

        mSampleRate = PreferenceUtils.getPrefInt(prefs, getResources(), R.string.keyRecordingRate, R.string.defaultRecordingRate);

        setUpSettingsButton();

        mView = (SpeechInputView) findViewById(R.id.vVoiceImeView);
        CallerInfo callerInfo = new CallerInfo(getExtras(), getCallingActivity());
        // TODO: do we need to send the ComponentName of the calling activity instead
        mView.setListener(R.array.keysActivity, callerInfo, getVoiceImeViewListener());

        if (isAutoStart) {
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
            mView.closeSession();
        }
    }

    private SpeechInputView.VoiceImeViewListener getVoiceImeViewListener() {
        return new SpeechInputView.VoiceImeViewListener() {

            private List<byte[]> mBufferList = new ArrayList<>();

            @Override
            public void onPartialResult(List<String> results) {
                // Ignore the partial results
            }

            @Override
            public void onFinalResult(List<String> results) {
                if (results != null && results.size() > 0) {
                    int sum = 0;
                    for (byte[] ba : mBufferList) {
                        sum = sum + ba.length;
                    }
                    mCompleteRecording = new byte[sum];
                    int pos = 0;
                    for (byte[] ba : mBufferList) {
                        System.arraycopy(ba, 0, mCompleteRecording, pos, ba.length);
                        pos = pos + ba.length;
                    }
                    mBufferList = new ArrayList<>();

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
                mBufferList.add(buffer);
            }
        };
    }
}
