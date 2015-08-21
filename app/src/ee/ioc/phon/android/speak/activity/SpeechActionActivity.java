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
import ee.ioc.phon.android.speak.VoiceImeView;
import ee.ioc.phon.android.speak.model.CallerInfo;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;

public class SpeechActionActivity extends AbstractRecognizerIntentActivity {

    private boolean mAutoStart;
    private int mSampleRate;
    private byte[] mCompleteRecording;

    private VoiceImeView mView;

    void showError() {
        // TODO
    }

    /**
     * TODO: get the audio
     */
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
        setUpExtras();
        setTvPrompt((TextView) findViewById(R.id.tvPrompt));

        // Launch recognition immediately (if set so).
        // Auto-start only occurs is onCreate is called
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mAutoStart = PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart);
        mSampleRate = PreferenceUtils.getPrefInt(prefs, getResources(), R.string.keyRecordingRate, R.string.defaultRecordingRate);
    }

    @Override
    public void onStart() {
        super.onStart();

        setUpSettingsButton();

        mView = (VoiceImeView) findViewById(R.id.vVoiceImeView);
        CallerInfo callerInfo = new CallerInfo(getExtras(), getCallingActivity());
        // TODO: do we need to send the ComponentName of the calling activity instead
        mView.setListener(R.array.keysActivity, callerInfo, getVoiceImeViewListener());

        if (mAutoStart) {
            mAutoStart = false;
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

    private VoiceImeView.VoiceImeViewListener getVoiceImeViewListener() {
        return new VoiceImeView.VoiceImeViewListener() {

            private List<byte[]> mBufferList = new ArrayList<>();

            @Override
            public void onPartialResult(ArrayList<String> results) {
                // Ignore the partial results
            }

            @Override
            public void onFinalResult(ArrayList<String> results) {
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
            public void onBufferReceived(byte[] buffer) {
                Log.i("Activity: onBufferReceived: " + buffer.length);
                mBufferList.add(buffer);
            }
        };
    }
}