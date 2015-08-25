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

package ee.ioc.phon.android.speak.service;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.android.speak.ChunkedWebRecSessionBuilder;
import ee.ioc.phon.android.speak.Constants;
import ee.ioc.phon.android.speak.Extras;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;
import ee.ioc.phon.android.speak.RawAudioRecorder;
import ee.ioc.phon.android.speak.Utils;
import ee.ioc.phon.android.speak.utils.PreferenceUtils;
import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;
import ee.ioc.phon.netspeechapi.recsession.Hypothesis;
import ee.ioc.phon.netspeechapi.recsession.Linearization;
import ee.ioc.phon.netspeechapi.recsession.NotAvailableException;
import ee.ioc.phon.netspeechapi.recsession.RecSession;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;

/**
 * Implements RecognitionService, connects to the server via HTTP.
 *
 * @author Kaarel Kaljurand
 */
public class SpeechRecognitionService extends AbstractRecognitionService {

    private SharedPreferences mPrefs;

    private Handler mStopHandler = new Handler();
    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;

    private Runnable mSendTask;
    private Runnable mStopTask;

    // Time (in milliseconds since the boot) when the recording is going to be stopped
    private long mTimeToFinish;

    private ChunkedWebRecSessionBuilder mRecSessionBuilder;
    private ChunkedWebRecSession mRecSession;

    private Bundle mExtras;


    @Override
    void onCancel0() {
        releaseResources();
    }

    @Override
    void connectToTheServer(Intent recognizerIntent) throws MalformedURLException {
        mSendHandler.postDelayed(mSendTask, Constants.TASK_DELAY_SEND);
        mStopHandler.postDelayed(mStopTask, Constants.TASK_DELAY_STOP);
    }

    @Override
    void setUpHandler(Intent recognizerIntent, RecognitionService.Callback callback) {
        boolean success = init(recognizerIntent);
        if (!success) {
            return;
        }
        mRecSessionBuilder.setContentType(getSampleRate());
        if (Log.DEBUG) Log.i(mRecSessionBuilder.toStringArrayList());
        mRecSession = mRecSessionBuilder.build();
        try {
            mRecSession.create();
        } catch (IOException e) {
            onError(SpeechRecognizer.ERROR_NETWORK);
            return;
        } catch (NotAvailableException e) {
            // This cannot happen in the current net-speech-api?
            onError(SpeechRecognizer.ERROR_SERVER);
            return;
        }
    }

    @Override
    boolean queryPrefAudioCues(SharedPreferences prefs, Resources resources) {
        return PreferenceUtils.getPrefBoolean(prefs, getResources(), R.string.keyAudioCues, R.bool.defaultAudioCues);
    }

    @Override
    int getSampleRate() {
        return PreferenceUtils.getPrefInt(mPrefs, getResources(), R.string.keyRecordingRate, R.string.defaultRecordingRate);
    }

    private void releaseResources() {
        stopTasks();
        if (mRecSession != null && !mRecSession.isFinished()) {
            mRecSession.cancel();
        }

        releaseRecorder();
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }
    }


    // TODO: temporary hack
    protected void afterRecording(RawAudioRecorder recorder) {
        stopTasks();
        transcribeAndFinishInBackground(recorder.consumeRecording());
    }

    /**
     * @param bytes  byte array representing the audio data
     * @param isLast indicates that this is the last chunk that is sent
     * @throws IOException
     */
    private void sendChunk(byte[] bytes, boolean isLast) throws IOException {
        if (mRecSession != null && !mRecSession.isFinished()) {
            mRecSession.sendChunk(bytes, isLast);
        }
    }


    private void stopTasks() {
        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendTask);
        if (mStopHandler != null) mStopHandler.removeCallbacks(mStopTask);
    }


    private boolean transcribeAndFinishInBackground(final byte[] bytes) {
        Thread t = new Thread() {
            public void run() {
                try {
                    try {
                        sendChunk(bytes, true);
                        getResult(mRecSession);
                    } catch (IOException e) {
                        onError(SpeechRecognizer.ERROR_NETWORK);
                    }
                } finally {
                    releaseResources();
                }
            }
        };
        t.start();
        return true;
    }


    /**
     * <p>If there are no results then returns {@code SpeechRecognizer.ERROR_NO_MATCH)}.
     * Otherwise packages the results in two different formats which both use an {@code ArrayList<String>}
     * and sends the results to the caller.</p>
     */
    private void getResult(RecSession recSession) throws IOException {
        RecSessionResult result = recSession.getResult();

        if (result == null) {
            Log.i("Callback: error: ERROR_NO_MATCH: RecSessionResult == null");
            onError(SpeechRecognizer.ERROR_NO_MATCH);
            return;
        }

        List<Hypothesis> hyps = result.getHypotheses();
        if (hyps.isEmpty()) {
            Log.i("Callback: error: ERROR_NO_MATCH: getHypotheses().isEmpty()");
            onError(SpeechRecognizer.ERROR_NO_MATCH);
            return;
        }

        int maxResults = mExtras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
        if (maxResults <= 0) {
            maxResults = hyps.size();
        }

        // Utterances OR linearizations
        ArrayList<String> lins = new ArrayList<>();

        // Utterances and their linearizations in a flat serialization
        ArrayList<String> everything = new ArrayList<>();
        ArrayList<Integer> counts = new ArrayList<>(hyps.size());
        int count = 0;
        for (Hypothesis hyp : hyps) {
            if (count++ >= maxResults) {
                break;
            }
            String utterance = hyp.getUtterance();
            // We assume that there is always an utterance. If the utterance is
            // missing then we consider the hypothesis not well-formed and take
            // the next hypothesis.
            if (utterance == null) {
                continue;
            }
            everything.add(utterance);
            List<Linearization> hypLins = hyp.getLinearizations();
            if (hypLins == null || hypLins.isEmpty()) {
                lins.add(hyp.getUtterance());
                counts.add(0);
            } else {
                counts.add(hypLins.size());
                for (Linearization lin : hypLins) {
                    String output = lin.getOutput();
                    everything.add(output);
                    everything.add(lin.getLang());
                    if (output == null || output.length() == 0) {
                        lins.add(utterance);
                    } else {
                        lins.add(output);
                    }
                }
            }
        }
        returnOrForwardMatches(everything, counts, lins);
    }


    /**
     * <p>Returns the transcription results to the caller,
     * or sends them to the pending intent.</p>
     *
     * @param everything recognition results (all the components)
     * @param counts     number of linearizations for each hyphothesis (needed to interpret {@code everything})
     * @param matches    recognition results (just linearizations)
     */
    private void returnOrForwardMatches(ArrayList<String> everything, ArrayList<Integer> counts, ArrayList<String> matches) {
        PendingIntent pendingIntent = Utils.getPendingIntent(mExtras);
        if (pendingIntent == null) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);
            bundle.putStringArrayList(Extras.RESULTS_RECOGNITION_LINEARIZATIONS, everything);
            bundle.putIntegerArrayList(Extras.RESULTS_RECOGNITION_LINEARIZATION_COUNTS, counts);
            Log.i("Callback: results: RESULTS_RECOGNITION: " + matches);
            Log.i("Callback: results: RESULTS_RECOGNITION_LINEARIZATIONS: " + everything);
            Log.i("Callback: results: RESULTS_RECOGNITION_LINEARIZATIONS_COUNTS: " + counts);
            onResults(bundle);
        } else {
            Log.i("EXTRA_RESULTS_PENDINGINTENT_BUNDLE was used with SpeechRecognizer (this is not tested)");
            // This probably never occurs...
            Bundle bundle = mExtras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE);
            if (bundle == null) {
                bundle = new Bundle();
            }
            String match = matches.get(0);
            //mExtraResultsPendingIntentBundle.putString(SearchManager.QUERY, match);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            // This is for Google Maps, YouTube, ...
            intent.putExtra(SearchManager.QUERY, match);
            // This is for SwiftKey X, ...
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
            intent.putStringArrayListExtra(Extras.RESULTS_RECOGNITION_LINEARIZATIONS, everything);
            intent.putIntegerArrayListExtra(Extras.RESULTS_RECOGNITION_LINEARIZATION_COUNTS, counts);
            try {
                // TODO: dummy number 1234
                pendingIntent.send(this, 1234, intent);
            } catch (CanceledException e) {
                // TODO
            }
        }
    }


    private boolean init(Intent recognizerIntent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mExtras = recognizerIntent.getExtras();
        if (mExtras == null) {
            // For some reason getExtras() can return null, we map it
            // to an empty Bundle if this occurs.
            mExtras = new Bundle();
        }

        final boolean isAutoStopAfterPause;

        // If the caller does not specify this extra, then we set it based on the settings.
        // TODO: in general, we could have 3-valued settings: true, false, use caller
        if (mExtras.containsKey(Extras.EXTRA_UNLIMITED_DURATION)) {
            isAutoStopAfterPause = !mExtras.getBoolean(Extras.EXTRA_UNLIMITED_DURATION);
        } else {
            isAutoStopAfterPause = PreferenceUtils.getPrefBoolean(mPrefs, getResources(), R.string.keyAutoStopAfterPause, R.bool.defaultAutoStopAfterPause);
        }

        try {
            mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, mExtras, null);
        } catch (MalformedURLException e) {
            // The user has managed to store a malformed URL in the configuration.
            Log.i("Callback: error: ERROR_CLIENT");
            onError(SpeechRecognizer.ERROR_CLIENT);
            return false;
        }

        mTimeToFinish = SystemClock.uptimeMillis() + 1000 * Integer.parseInt(
                mPrefs.getString(
                        getString(R.string.keyAutoStopAfterTime),
                        getString(R.string.defaultAutoStopAfterTime)));

        // Starting chunk sending in a separate thread so that slow Internet
        // would not block the UI.
        HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mSendLooper = thread.getLooper();
        mSendHandler = new Handler(mSendLooper);

        // Send chunks to the server
        mSendTask = new Runnable() {
            public void run() {
                if (getRecorder() != null) {
                    // TODO: Currently returns 16-bit LE
                    byte[] buffer = getRecorder().consumeRecording();
                    try {
                        sendChunk(buffer, false);
                        onBufferReceived(buffer);
                        mSendHandler.postDelayed(this, Constants.TASK_INTERVAL_SEND);
                    } catch (IOException e) {
                        onError(SpeechRecognizer.ERROR_NETWORK);
                    }
                }
            }
        };


        // Check if we should stop recording
        mStopTask = new Runnable() {
            public void run() {
                if (getRecorder() != null) {
                    if (mTimeToFinish < SystemClock.uptimeMillis() || isAutoStopAfterPause && getRecorder().isPausing()) {
                        //stopRecording(listener);
                        stopRecording0();
                    } else {
                        mStopHandler.postDelayed(this, Constants.TASK_INTERVAL_STOP);
                    }
                }
            }
        };

        return true;
    }


    /**
     * About RemoteException see
     * http://stackoverflow.com/questions/3156389/android-remoteexceptions-and-services
     */
    /*
    private static void handleRemoteException(RemoteException e) {
        Log.e(e.getMessage());
    }
    */
}