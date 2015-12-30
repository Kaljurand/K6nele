package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

import java.io.IOException;
import java.util.ArrayList;

import ee.ioc.phon.android.speak.AudioPauser;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.android.speechutils.AudioCue;
import ee.ioc.phon.android.speechutils.Extras;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;

/**
 * About RemoteException see
 * http://stackoverflow.com/questions/3156389/android-remoteexceptions-and-services
 */
public abstract class AbstractRecognitionService extends RecognitionService {

    // When does the chunk sending start and what is its interval
    static final int TASK_INTERVAL_SEND = 300;
    static final int TASK_DELAY_SEND = 100;

    // We send more frequently in the IME
    static final int TASK_INTERVAL_IME_SEND = 200;
    static final int TASK_DELAY_IME_SEND = 100;

    // Check the volume 10 times a second
    static final int TASK_INTERVAL_VOL = 100;
    // Wait for 1/2 sec before starting to measure the volume
    static final int TASK_DELAY_VOL = 500;

    static final int TASK_INTERVAL_STOP = 1000;
    static final int TASK_DELAY_STOP = 1000;

    private AudioCue mAudioCue;
    private AudioPauser mAudioPauser;
    private RecognitionService.Callback mListener;

    private RawAudioRecorder mRecorder;

    private Handler mVolumeHandler = new Handler();
    private Runnable mShowVolumeTask;

    private Handler mStopHandler = new Handler();
    private Runnable mStopTask;

    private Bundle mExtras;

    static Bundle toBundle(String hypothesis) {
        ArrayList<String> hypotheses = new ArrayList<>();
        hypotheses.add(hypothesis);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, hypotheses);
        return bundle;
    }

    static Bundle toBundle(ArrayList<String> hypotheses, boolean isFinal) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, hypotheses);
        bundle.putBoolean(Extras.EXTRA_SEMI_FINAL, isFinal);
        return bundle;
    }

    /**
     * Configures the service based on the given intent extras.
     * Can result in an IOException, e.g. if building the server URL fails
     * (UnsupportedEncodingException, MalformedURLException).
     * TODO: generalize the exception
     */
    abstract void configure(Intent recognizerIntent) throws IOException;

    /**
     * Start sending audio to the server.
     */
    abstract void connect();

    /**
     * Stop sending audio to the server.
     */
    abstract void disconnect();

    /**
     * Queries the preferences to find out if audio cues are switched on.
     * Different services can have different preferences.
     */
    boolean isAudioCues() {
        return false;
    }

    /**
     * Gets the sample rate used in the recorder.
     * Different services can use a different sample rate.
     */
    int getSampleRate() {
        return 16000;
    }

    /**
     * Gets the max number of milliseconds to record.
     */
    int getAutoStopAfterMillis() {
        return 1000 * 10000; // We record as long as the server allows
    }

    /**
     * Stop after a pause is detected.
     * This can be implemented either in the server or in the app.
     */
    boolean isAutoStopAfterPause() {
        return false;
    }

    /**
     * Tasks done after the recording has finished and the audio has been obtained.
     */
    void afterRecording(byte[] recording) {
        // Nothing to do, e.g. if the audio has already been sent to the server during recording
    }

    RawAudioRecorder getRecorder() {
        return mRecorder;
    }

    SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onDestroy() {
        super.onDestroy();
        stopRecording0();
        disconnect();
    }

    /**
     * Starts recording and opens the connection to the server to start sending the recorded packages.
     */
    @Override
    protected void onStartListening(final Intent recognizerIntent, RecognitionService.Callback listener) {
        mListener = listener;
        // TODO: do we need to check the permissions somewhere?
        //if (checkCallingPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_DENIED) {
        //    onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
        //    return;
        //}
        Log.i("onStartListening");

        setAudioCuesEnabled(isAudioCues());

        mExtras = recognizerIntent.getExtras();
        if (mExtras == null) {
            mExtras = new Bundle();
        }

        try {
            configure(recognizerIntent);
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_CLIENT);
            return;
        }

        mAudioPauser = new AudioPauser(this);
        mAudioPauser.pause();

        try {
            onReadyForSpeech(new Bundle());
            startRecord(getSampleRate());
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_AUDIO);
            return;
        }

        onBeginningOfSpeech();
        connect();
    }

    /**
     * Stops the recording and informs the server that no more packages are coming.
     */
    @Override
    protected void onStopListening(RecognitionService.Callback listener) {
        Log.i("onStopListening");
        onEndOfSpeech();
    }

    /**
     * Stops the recording and closes the connection to the server.
     */
    @Override
    protected void onCancel(RecognitionService.Callback listener) {
        Log.i("onCancel");
        stopRecording0();
        disconnect();
        // Send empty results if recognition is cancelled
        // TEST: if it works with Google Translate and Slide IT
        onResults(new Bundle());
    }


    /**
     * Calls onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT) if server initiates close
     * without having received EOS. Otherwise simply shuts down the recorder and recognizer service.
     *
     * @param isEosSent true iff EOS was sent
     */
    public void handleFinish(boolean isEosSent) {
        if (isEosSent) {
            onCancel(mListener);
        } else {
            onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
        }
    }

    Bundle getExtras() {
        return mExtras;
    }

    protected void onReadyForSpeech(Bundle bundle) {
        if (mAudioCue != null) mAudioCue.playStartSoundAndSleep();
        try {
            mListener.readyForSpeech(bundle);
        } catch (RemoteException e) {
        }
    }

    protected void onRmsChanged(float rms) {
        try {
            mListener.rmsChanged(rms);
        } catch (RemoteException e) {
        }
    }

    protected void onError(int errorCode) {
        stopRecording0();
        disconnect();
        if (mAudioCue != null) mAudioCue.playErrorSound();
        try {
            mListener.error(errorCode);
        } catch (RemoteException e) {
        }
    }

    protected void onResults(Bundle bundle) {
        stopRecording0();
        disconnect();
        try {
            mListener.results(bundle);
        } catch (RemoteException e) {
        }
    }

    protected void onPartialResults(Bundle bundle) {
        try {
            mListener.partialResults(bundle);
        } catch (RemoteException e) {
        }
    }

    protected void onBeginningOfSpeech() {
        try {
            mListener.beginningOfSpeech();
        } catch (RemoteException e) {
        }
    }

    protected void onEndOfSpeech() {
        byte[] recording = null;

        if (mRecorder != null) {
            // TODO: make sure this call does not do too much work in the case of the
            // WebSocket-service which does not use the bytes in the end
            recording = mRecorder.consumeRecording();
        }
        stopRecording0();
        if (mAudioCue != null) mAudioCue.playStopSound();
        try {
            mListener.endOfSpeech();
        } catch (RemoteException e) {
        }
        afterRecording(recording);
    }

    /**
     * TODO: Expects 16-bit BE?
     *
     * @param buffer
     */
    protected void onBufferReceived(byte[] buffer) {
        try {
            mListener.bufferReceived(buffer);
        } catch (RemoteException e) {
        }
    }

    /**
     * Return the server URL specified by the caller, or if this is missing then the URL
     * stored in the preferences, or if this is missing then the default URL.
     *
     * @param key          preference key to the server URL
     * @param defaultValue default URL to use if no URL is stored at the given key
     * @return server URL as string
     */
    String getServerUrl(int key, int defaultValue) {
        String url = getExtras().getString(Extras.EXTRA_SERVER_URL);
        if (url == null) {
            return PreferenceUtils.getPrefString(
                    getSharedPreferences(),
                    getResources(),
                    key,
                    defaultValue);
        }
        return url;
    }


    /**
     * Starts recording.
     *
     * @throws IOException if there was an error, e.g. another app is currently recording
     */
    private void startRecord(int sampleRate) throws IOException {
        mRecorder = new RawAudioRecorder(sampleRate);
        if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
            throw new IOException();
        }

        if (mRecorder.getState() != RawAudioRecorder.State.READY) {
            throw new IOException();
        }

        mRecorder.start();

        if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
            throw new IOException();
        }

        // Monitor the volume level
        mShowVolumeTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    onRmsChanged(mRecorder.getRmsdb());
                    mVolumeHandler.postDelayed(this, TASK_INTERVAL_VOL);
                }
            }
        };

        mVolumeHandler.postDelayed(mShowVolumeTask, TASK_DELAY_VOL);


        // Time (in milliseconds since the boot) when the recording is going to be stopped
        final long timeToFinish = SystemClock.uptimeMillis() + getAutoStopAfterMillis();
        final boolean isAutoStopAfterPause = isAutoStopAfterPause();

        // Check if we should stop recording
        mStopTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    if (timeToFinish < SystemClock.uptimeMillis() || isAutoStopAfterPause && mRecorder.isPausing()) {
                        onEndOfSpeech();
                    } else {
                        mStopHandler.postDelayed(this, TASK_INTERVAL_STOP);
                    }
                }
            }
        };

        mStopHandler.postDelayed(mStopTask, TASK_DELAY_STOP);
    }


    private void stopRecording0() {
        releaseRecorder();
        if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);
        if (mStopHandler != null) mStopHandler.removeCallbacks(mStopTask);
        if (mAudioPauser != null) mAudioPauser.resume();
    }


    private void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }


    private void setAudioCuesEnabled(boolean enabled) {
        if (enabled) {
            mAudioCue = new AudioCue(this);
        } else {
            mAudioCue = null;
        }
    }
}
