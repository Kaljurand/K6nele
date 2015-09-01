package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

import java.io.IOException;
import java.net.MalformedURLException;

import ee.ioc.phon.android.speak.AudioCue;
import ee.ioc.phon.android.speak.AudioPauser;
import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.RawAudioRecorder;

/**
 * About RemoteException see
 * http://stackoverflow.com/questions/3156389/android-remoteexceptions-and-services
 */
public abstract class AbstractRecognitionService extends RecognitionService {

    private AudioCue mAudioCue;
    private AudioPauser mAudioPauser;
    private RecognitionService.Callback mListener;

    private RawAudioRecorder mRecorder;

    private Handler mVolumeHandler = new Handler();
    private Runnable mShowVolumeTask;

    private Handler mStopHandler = new Handler();
    private Runnable mStopTask;

    /**
     * Stop sending audio to the server.
     */
    abstract void disconnectFromServer();

    /**
     * Start sending audio to the server.
     */
    abstract void connectToServer(Intent recognizerIntent) throws MalformedURLException;

    /**
     * Configures the service based on the given intent extras.
     */
    abstract void configureService(Intent recognizerIntent);

    /**
     * Queries the preferences to find out if audio cues are switched on.
     * Different services can have different preferences.
     */
    abstract boolean queryPrefAudioCues(SharedPreferences prefs, Resources resources);

    /**
     * Gets the sample rate used in the recorder.
     * Different services can use a different sample rate.
     */
    abstract int getSampleRate();

    /**
     * Gets the max number of milliseconds to record.
     */
    abstract int getAutoStopAfterTime();

    abstract boolean isAutoStopAfterPause();

    abstract void afterRecording(RawAudioRecorder recorder);

    protected RawAudioRecorder getRecorder() {
        return mRecorder;
    }

    public void onDestroy() {
        super.onDestroy();
        stopRecording0();
        disconnectFromServer();
        if (mAudioPauser != null) mAudioPauser.resume();
    }

    /**
     * Opens the socket and starts recording and sending the recorded packages.
     * // TODO: test this
     * if (checkCallingPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_DENIED) {
     * handleError(listener, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
     * return;
     * }
     */
    @Override
    protected void onStartListening(final Intent recognizerIntent, RecognitionService.Callback listener) {
        Log.i("onStartListening");
        mAudioPauser = new AudioPauser(this);
        mAudioPauser.pause();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setAudioCuesEnabled(queryPrefAudioCues(prefs, getResources()));

        mListener = listener;

        configureService(recognizerIntent);

        try {
            onReadyForSpeech(new Bundle());
            startRecord(getSampleRate());
            onBeginningOfSpeech();
            connectToServer(recognizerIntent);
        } catch (MalformedURLException e) {
            onError(SpeechRecognizer.ERROR_CLIENT);
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_AUDIO);
        }
    }

    /**
     * Stops the recording and informs the socket that no more packages are coming.
     */
    @Override
    protected void onStopListening(RecognitionService.Callback listener) {
        onEndOfSpeech();
    }

    /**
     * Stops the recording and closes the socket.
     */
    @Override
    protected void onCancel(RecognitionService.Callback listener) {
        stopRecording0();
        disconnectFromServer();
        // Send empty results if recognition is cancelled
        // TEST: if it works with Google Translate and Slide IT
        onResults(new Bundle());
    }

    // TODO: call onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT); if server initiates close
    // without having received EOS
    public void handleFinish() {
        onCancel(mListener);
    }

    public void onReadyForSpeech(Bundle bundle) {
        if (mAudioCue != null) mAudioCue.playStartSoundAndSleep();
        try {
            mListener.readyForSpeech(bundle);
        } catch (RemoteException e) {
        }
    }

    public void onRmsChanged(float rms) {
        try {
            mListener.rmsChanged(rms);
        } catch (RemoteException e) {
        }
    }

    public void onError(int errorCode) {
        // As soon as there is an error we shut down the socket and the recorder
        stopRecording0();
        disconnectFromServer();
        if (mAudioCue != null) mAudioCue.playErrorSound();
        if (mAudioPauser != null) mAudioPauser.resume();
        try {
            mListener.error(errorCode);
        } catch (RemoteException e) {
        }
    }

    public void onResults(Bundle bundle) {
        try {
            mListener.results(bundle);
        } catch (RemoteException e) {
        }
    }

    public void onPartialResults(Bundle bundle) {
        try {
            mListener.partialResults(bundle);
        } catch (RemoteException e) {
        }
    }

    public void onBeginningOfSpeech() {
        try {
            mListener.beginningOfSpeech();
        } catch (RemoteException e) {
        }
    }

    public void onEndOfSpeech() {
        afterRecording(mRecorder);
        stopRecording0();
        if (mAudioCue != null) mAudioCue.playStopSound();
        try {
            mListener.endOfSpeech();
        } catch (RemoteException e) {
        }
    }

    /**
     * TODO: Expects 16-bit BE?
     *
     * @param buffer
     */
    public void onBufferReceived(byte[] buffer) {
        try {
            mListener.bufferReceived(buffer);
        } catch (RemoteException e) {
        }
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
                    mVolumeHandler.postDelayed(this, Constants.TASK_INTERVAL_VOL);
                }
            }
        };

        mVolumeHandler.postDelayed(mShowVolumeTask, Constants.TASK_DELAY_VOL);


        // Time (in milliseconds since the boot) when the recording is going to be stopped
        final long timeToFinish = SystemClock.uptimeMillis() + getAutoStopAfterTime();
        final boolean isAutoStopAfterPause = isAutoStopAfterPause();

        // Check if we should stop recording
        mStopTask = new Runnable() {
            public void run() {
                if (mRecorder != null) {
                    if (timeToFinish < SystemClock.uptimeMillis() || isAutoStopAfterPause && mRecorder.isPausing()) {
                        onEndOfSpeech();
                    } else {
                        mStopHandler.postDelayed(this, Constants.TASK_INTERVAL_STOP);
                    }
                }
            }
        };

        mStopHandler.postDelayed(mStopTask, Constants.TASK_DELAY_STOP);
    }


    private void stopRecording0() {
        if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);
        if (mStopHandler != null) mStopHandler.removeCallbacks(mStopTask);
        releaseRecorder();
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