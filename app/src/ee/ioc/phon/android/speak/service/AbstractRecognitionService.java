package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
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

public abstract class AbstractRecognitionService extends RecognitionService {

    private AudioCue mAudioCue;
    private AudioPauser mAudioPauser;
    private RecognitionService.Callback mListener;

    private RawAudioRecorder mRecorder;

    private Handler mVolumeHandler = new Handler();
    private Runnable mShowVolumeTask;

    abstract void onCancel0();

    abstract void connectToTheServer(Intent recognizerIntent) throws MalformedURLException;

    abstract void setUpHandler(Intent recognizerIntent, RecognitionService.Callback listener);

    abstract boolean queryPrefAudioCues(SharedPreferences prefs, Resources resources);

    abstract int getSampleRate();

    abstract void afterRecording(RawAudioRecorder recorder);

    protected RawAudioRecorder getRecorder() {
        return mRecorder;
    }


    public void onDestroy() {
        super.onDestroy();
        onCancel0();
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

        setListener(listener);

        setUpHandler(recognizerIntent, listener);

        try {
            onReadyForSpeech(new Bundle());
            startRecord(getSampleRate());
            onBeginningOfSpeech();
            connectToTheServer(recognizerIntent);
        } catch (MalformedURLException e) {
            onError(SpeechRecognizer.ERROR_CLIENT);
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_AUDIO);
        }
    }


    /**
     * Stops the recording and closes the socket.
     */
    @Override
    protected void onCancel(RecognitionService.Callback listener) {
        onCancel0();
        // Send empty results if recognition is cancelled
        // TEST: if it works with Google Translate and Slide IT
        onResults(new Bundle());
    }

    /**
     * Stops the recording and informs the socket that no more packages are coming.
     */
    @Override
    protected void onStopListening(RecognitionService.Callback listener) {
        stopRecording0();
        onEndOfSpeech();
    }

    public void setListener(RecognitionService.Callback listener) {
        mListener = listener;
    }

    public RecognitionService.Callback getListener() {
        return mListener;
    }

    public void setAudioCuesEnabled(boolean enabled) {
        if (enabled) {
            mAudioCue = new AudioCue(this);
        } else {
            mAudioCue = null;
        }
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
        onCancel0();
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

    void releaseRecorder() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
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
    }

    protected void stopRecording0() {
        if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);
        afterRecording(mRecorder);
        releaseRecorder();
        if (mAudioPauser != null) mAudioPauser.resume();
    }

}