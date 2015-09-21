/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.AudioCue;
import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RecognizerIntentService;
import ee.ioc.phon.android.speak.RecognizerIntentService.RecognizerBinder;
import ee.ioc.phon.android.speak.RecognizerIntentService.State;
import ee.ioc.phon.android.speak.Utils;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;


/**
 * @deprecated
 *
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
 * speech recognizer webservice (and which are then rewritten by the net-speech-api)
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
public class RecognizerIntentActivity extends AbstractRecognizerIntentActivity {

    private static final int TASK_CHUNKS_INTERVAL = 1500;
    private static final int TASK_CHUNKS_DELAY = 100;

    // Update the byte count every second
    private static final int TASK_BYTES_INTERVAL = 1000;
    // Start the task almost immediately
    private static final int TASK_BYTES_DELAY = 100;

    // Check for pause / max time limit twice a second
    private static final int TASK_STOP_INTERVAL = 500;
    private static final int TASK_STOP_DELAY = 1000;

    // Check the volume 10 times a second
    private static final int TASK_VOLUME_INTERVAL = 100;
    private static final int TASK_VOLUME_DELAY = 500;

    private static final String DOTS = "............";

    private SharedPreferences mPrefs;

    private ChunkedWebRecSessionBuilder mRecSessionBuilder;

    private TextView mTvPrompt;
    private Button mBStartStop;
    private LinearLayout mLlTranscribing;
    private LinearLayout mLlProgress;
    private LinearLayout mLlError;
    private TextView mTvBytes;
    private Chronometer mChronometer;
    private ImageView mIvVolume;
    private ImageView mIvWaveform;
    private TextView mTvChunks;
    private TextView mTvErrorMessage;
    private List<Drawable> mVolumeLevels;

    private Handler mHandlerBytes = new Handler();
    private Handler mHandlerStop = new Handler();
    private Handler mHandlerVolume = new Handler();
    private Handler mHandlerChunks = new Handler();

    private Runnable mRunnableBytes;
    private Runnable mRunnableStop;
    private Runnable mRunnableVolume;
    private Runnable mRunnableChunks;

    private Resources mRes;
    private MediaPlayer mMediaPlayer;

    private AudioCue mAudioCue;
    private RecognizerIntentService mService;
    private boolean mIsBound = false;
    private boolean mStartRecording = false;
    private int mLevel = 0;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i("Service connected");
            mService = ((RecognizerBinder) service).getService();

            mService.setOnResultListener(new RecognizerIntentService.OnResultListener() {
                public boolean onResult(RecSessionResult result) {
                    // We trust that getLinearizations() returns a non-null non-empty list.
                    ArrayList<String> matches = new ArrayList<>();
                    matches.addAll(result.getLinearizations());
                    returnOrForwardMatches(matches);
                    return true;
                }
            });

            mService.setOnErrorListener(new RecognizerIntentService.OnErrorListener() {
                public boolean onError(int errorCode, Exception e) {
                    handleResultError(errorCode, "onError", e);
                    return true;
                }
            });


            if (mStartRecording && !mService.isWorking()) {
                startRecording();
                mStartRecording = false;
            } else {
                setGui();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mService = null;
            Log.i("Service disconnected");
        }
    };

    Uri getAudioUri() {
        try {
            FileOutputStream fos = openFileOutput(Constants.AUDIO_FILENAME, Context.MODE_PRIVATE);
            fos.write(mService.getCompleteRecordingAsWav());
            fos.close();

            return Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + Constants.AUDIO_FILENAME);
        } catch (FileNotFoundException e) {
            Log.e("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e("IOException: " + e.getMessage());
        }
        return null;
    }

    void showError() {
        playErrorSound();
        stopAllTasks();
        setGuiError();
    }


    /**
     * <p>Only for developers, i.e. we are not going to localize these strings.</p>
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
        info.add("User-Agent comment: " + getRecSessionBuilder().getUserAgentComment());
        info.add("Calling activity class name: " + callingActivityClassName);
        info.add("Calling activity package name: " + callingActivityPackageName);
        info.add("Pending intent target package: " + pendingIntentTargetPackage);
        info.add("Selected grammar: " + getRecSessionBuilder().getGrammarUrl());
        info.add("Selected target lang: " + getRecSessionBuilder().getGrammarTargetLang());
        info.add("Selected server: " + getRecSessionBuilder().getServerUrl());
        info.add("Intent action: " + getIntent().getAction());
        info.addAll(Utils.ppBundle(getExtras()));
        return info.toArray(new String[info.size()]);
    }

    // TODO: remove
    protected ChunkedWebRecSessionBuilder getRecSessionBuilder() {
        return mRecSessionBuilder;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpActivity(R.layout.recognizer);
        setUpExtras();

        try {
            mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, getExtras(), getCallingActivity());
        } catch (MalformedURLException e) {
            // The user has managed to store a malformed URL in the configuration.
            handleResultError(RecognizerIntent.RESULT_CLIENT_ERROR, "", e);
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // For the change in the autostart-setting to take effect,
        // the user must restart the app. This seems more natural.
        mStartRecording = PreferenceUtils.getPrefBoolean(mPrefs, getResources(), R.string.keyAutoStart, R.bool.defaultAutoStart);

        mTvPrompt = (TextView) findViewById(R.id.tvPrompt);
        mBStartStop = (Button) findViewById(R.id.bStartStop);
        mLlTranscribing = (LinearLayout) findViewById(R.id.llTranscribing);
        mLlProgress = (LinearLayout) findViewById(R.id.llProgress);
        mLlError = (LinearLayout) findViewById(R.id.llError);
        mTvBytes = (TextView) findViewById(R.id.tvBytes);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mIvVolume = (ImageView) findViewById(R.id.ivVolume);
        mIvWaveform = (ImageView) findViewById(R.id.ivWaveform);
        mTvChunks = (TextView) findViewById(R.id.tvChunks);
        mTvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);

        mRes = getResources();
        mVolumeLevels = new ArrayList<>();
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level0));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level1));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level2));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level3));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level4));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level5));
        mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level6));

        mAudioCue = new AudioCue(this);
    }


    @Override
    public void onStart() {
        super.onStart();

        // Show the length of the current recording in bytes
        mRunnableBytes = new Runnable() {
            public void run() {
                if (mService != null) {
                    mTvBytes.setText(Utils.getSizeAsString(mService.getLength()));
                }
                mHandlerBytes.postDelayed(this, TASK_BYTES_INTERVAL);
            }
        };

        // Show the number of audio chunks that have been sent to the server
        mRunnableChunks = new Runnable() {
            public void run() {
                if (mService != null) {
                    mTvChunks.setText(makeBar(DOTS, mService.getChunkCount()));
                }
                mHandlerChunks.postDelayed(this, TASK_CHUNKS_INTERVAL);
            }
        };

        // Decide if we should stop recording
        // 1. Max recording time (in milliseconds) has passed
        // 2. Speaker stopped speaking
        final int maxRecordingTime = 1000 * PreferenceUtils.getPrefInt(mPrefs, mRes, R.string.keyAutoStopAfterTime, R.string.defaultAutoStopAfterTime);

        mRunnableStop = new Runnable() {
            public void run() {
                if (mService != null) {
                    if (maxRecordingTime < (SystemClock.elapsedRealtime() - mService.getStartTime())) {
                        Log.i("Max recording time exceeded");
                        stopRecording();
                    } else if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyAutoStopAfterPause, R.bool.defaultAutoStopAfterPause) && mService.isPausing()) {
                        Log.i("Speaker finished speaking");
                        stopRecording();
                    } else {
                        mHandlerStop.postDelayed(this, TASK_STOP_INTERVAL);
                    }
                }
            }
        };


        mRunnableVolume = new Runnable() {
            public void run() {
                if (mService != null) {

                    float db = mService.getRmsdb();
                    final int maxLevel = mVolumeLevels.size() - 1;

                    int index = (int) ((db - Constants.DB_MIN) / (Constants.DB_MAX - Constants.DB_MIN) * maxLevel);
                    final int level = Math.min(Math.max(0, index), maxLevel);

                    if (level != mLevel) {
                        mIvVolume.setImageDrawable(mVolumeLevels.get(level));
                        mLevel = level;
                    }

                    mHandlerVolume.postDelayed(this, TASK_VOLUME_INTERVAL);
                }
            }
        };


        mBStartStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mIsBound) {
                    if (mService.getState() == State.RECORDING) {
                        stopRecording();
                    } else {
                        startRecording();
                    }
                } else {
                    mStartRecording = true;
                    doBindService();
                }
            }
        });

        setUpSettingsButton();
        doBindService();
    }


    @Override
    public void onResume() {
        super.onResume();
        setGui();
    }


    @SuppressLint("NewApi")
    @Override
    public void onStop() {
        super.onStop();
        Log.i("onStop");
        if (mService != null) {
            mService.setOnResultListener(null);
            mService.setOnErrorListener(null);
        }
        stopAllTasks();
        doUnbindService();

        // We stop the service unless a configuration change causes onStop(),
        // i.e. the service is not stopped because of rotation, but is
        // stopped if BACK or HOME is pressed, or the Settings-activity is launched.
        if (!isChangingConfigurations()) {
            stopService(new Intent(this, RecognizerIntentService.class));
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


    void doBindService() {
        // This can be called also on an already running service
        startService(new Intent(this, RecognizerIntentService.class));

        bindService(new Intent(this, RecognizerIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.i("Service is bound");
    }


    void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
            mService = null;
            Log.i("Service is UNBOUND");
        }
    }


    private void setGui() {
        if (mService == null) {
            // in onResume() the service might not be ready yet
            return;
        }
        switch (mService.getState()) {
            case IDLE:
                setGuiInit();
                break;
            case INITIALIZED:
                setGuiInit();
                break;
            case RECORDING:
                setGuiRecording();
                break;
            case PROCESSING:
                setGuiTranscribing(mService.getCompleteRecording());
                break;
            case ERROR:
                setGuiError(mService.getErrorCode());
                break;
        }
    }


    private void setRecorderStyle(int color) {
        mTvBytes.setTextColor(color);
        mChronometer.setTextColor(color);
    }


    private void stopRecording() {
        mService.stop();
        playStopSound();
        setGui();
    }


    private void startAllTasks() {
        mHandlerBytes.postDelayed(mRunnableBytes, TASK_BYTES_DELAY);
        mHandlerStop.postDelayed(mRunnableStop, TASK_STOP_DELAY);
        mHandlerVolume.postDelayed(mRunnableVolume, TASK_VOLUME_DELAY);
        mHandlerChunks.postDelayed(mRunnableChunks, TASK_CHUNKS_DELAY);
    }


    private void stopAllTasks() {
        mHandlerBytes.removeCallbacks(mRunnableBytes);
        mHandlerStop.removeCallbacks(mRunnableStop);
        mHandlerVolume.removeCallbacks(mRunnableVolume);
        mHandlerChunks.removeCallbacks(mRunnableChunks);
        stopChronometer();
    }


    private void setGuiInit() {
        mLlTranscribing.setVisibility(View.GONE);
        mIvWaveform.setVisibility(View.GONE);
        // includes: bytes, chronometer, chunks
        mLlProgress.setVisibility(View.INVISIBLE);
        mTvChunks.setText("");
        setTvPrompt(mTvPrompt);
        if (mStartRecording) {
            mBStartStop.setVisibility(View.GONE);
            mIvVolume.setVisibility(View.VISIBLE);
        } else {
            mIvVolume.setVisibility(View.GONE);
            mBStartStop.setText(getString(R.string.buttonSpeak));
            mBStartStop.setVisibility(View.VISIBLE);
        }
        mLlError.setVisibility(View.GONE);
    }


    private void setGuiError() {
        if (mService == null) {
            setGuiError(RecognizerIntent.RESULT_CLIENT_ERROR);
        } else {
            setGuiError(mService.getErrorCode());
        }
    }


    private void setGuiError(int errorCode) {
        mLlTranscribing.setVisibility(View.GONE);
        mIvVolume.setVisibility(View.GONE);
        mIvWaveform.setVisibility(View.GONE);
        // includes: bytes, chronometer, chunks
        mLlProgress.setVisibility(View.GONE);
        setTvPrompt(mTvPrompt);
        mBStartStop.setText(getString(R.string.buttonSpeak));
        mBStartStop.setVisibility(View.VISIBLE);
        mLlError.setVisibility(View.VISIBLE);
        mTvErrorMessage.setText(getErrorMessages().get(errorCode));
    }


    private void setGuiRecording() {
        mChronometer.setBase(mService.getStartTime());
        startChronometer();
        startAllTasks();
        setTvPrompt(mTvPrompt);
        mLlProgress.setVisibility(View.VISIBLE);
        mLlError.setVisibility(View.GONE);
        setRecorderStyle(mRes.getColor(R.color.red));
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyAutoStopAfterPause, R.bool.defaultAutoStopAfterPause)) {
            mBStartStop.setVisibility(View.GONE);
            mIvVolume.setVisibility(View.VISIBLE);
        } else {
            mIvVolume.setVisibility(View.GONE);
            mBStartStop.setText(getString(R.string.buttonStop));
            mBStartStop.setVisibility(View.VISIBLE);
        }
    }


    private void setGuiTranscribing(byte[] bytes) {
        mChronometer.setBase(mService.getStartTime());
        stopChronometer();
        mHandlerBytes.removeCallbacks(mRunnableBytes);
        mHandlerStop.removeCallbacks(mRunnableStop);
        mHandlerVolume.removeCallbacks(mRunnableVolume);
        // Chunk checking keeps running
        mTvBytes.setText(Utils.getSizeAsString(bytes.length));
        setRecorderStyle(mRes.getColor(R.color.grey2));
        mBStartStop.setVisibility(View.GONE);
        mTvPrompt.setVisibility(View.GONE);
        mIvVolume.setVisibility(View.GONE);
        mLlProgress.setVisibility(View.VISIBLE);
        mLlTranscribing.setVisibility(View.VISIBLE);

        // http://stackoverflow.com/questions/5012840/android-specifying-pixel-units-like-sp-px-dp-without-using-xml
        DisplayMetrics metrics = mRes.getDisplayMetrics();
        // This must match the layout_width of the top layout in recognizer.xml
        float dp = 250f;
        int waveformWidth = (int) (metrics.density * dp + 0.5f);
        int waveformHeight = (int) (waveformWidth / 2.5);
        mIvWaveform.setVisibility(View.VISIBLE);
        mIvWaveform.setImageBitmap(Utils.drawWaveform(bytes, waveformWidth, waveformHeight, 0, bytes.length));
    }


    private void stopChronometer() {
        mChronometer.stop();
    }


    private void startChronometer() {
        mChronometer.start();
    }


    private void startRecording() {
        int sampleRate = PreferenceUtils.getPrefInt(mPrefs, mRes, R.string.keyRecordingRate, R.string.defaultRecordingRate);
        getRecSessionBuilder().setContentType(sampleRate);
        if (mService.init(getRecSessionBuilder().build())) {
            playStartSound();
            mService.start(sampleRate);
            setGui();
        }
    }


    private void playStartSound() {
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyAudioCues, R.bool.defaultAudioCues)) {
            mAudioCue.playStartSoundAndSleep();
        }
    }

    private void playStopSound() {
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyAudioCues, R.bool.defaultAudioCues)) {
            mAudioCue.playStopSound();
        }
    }

    private void playErrorSound() {
        if (PreferenceUtils.getPrefBoolean(mPrefs, mRes, R.string.keyAudioCues, R.bool.defaultAudioCues)) {
            mAudioCue.playErrorSound();
        }
    }

    private static String makeBar(String bar, int len) {
        if (len <= 0) return "";
        if (len >= bar.length()) return Integer.toString(len);
        return bar.substring(0, len);
    }
}