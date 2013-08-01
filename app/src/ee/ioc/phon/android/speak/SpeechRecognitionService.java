/*
 * Copyright 2011-2013, Institute of Cybernetics at Tallinn University of Technology
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

package ee.ioc.phon.android.speak;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;
import ee.ioc.phon.netspeechapi.recsession.Hypothesis;
import ee.ioc.phon.netspeechapi.recsession.Linearization;
import ee.ioc.phon.netspeechapi.recsession.NotAvailableException;
import ee.ioc.phon.netspeechapi.recsession.RecSession;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionService;

/**
 * TODO: send correct waveform data
 *
 * @author Kaarel Kaljurand
 */
public class SpeechRecognitionService extends RecognitionService {

	// Check the volume 10 times a second
	private static final int TASK_INTERVAL_VOL = 100;
	// Wait for 1/2 sec before starting to measure the volume
	private static final int TASK_DELAY_VOL = 500;

	private static final int TASK_INTERVAL_STOP = 1000;
	private static final int TASK_DELAY_STOP = 1000;

	private SharedPreferences mPrefs;

	private Handler mVolumeHandler = new Handler();
	private Handler mStopHandler = new Handler();
	private volatile Looper mSendLooper;
	private volatile Handler mSendHandler;

	private Runnable mSendTask;
	private Runnable mShowVolumeTask;
	private Runnable mStopTask;

	// Time (in milliseconds since the boot) when the recording is going to be stopped
	private long mTimeToFinish;

	private ChunkedWebRecSessionBuilder mRecSessionBuilder;
	private ChunkedWebRecSession mRecSession;

	private RawAudioRecorder mRecorder;

	private Bundle mExtras;
	private AudioPauser mAudioPauser;


	@Override
	protected void onStartListening(Intent recognizerIntent, final Callback listener) {
		Log.i("onStartListening");
		mAudioPauser = new AudioPauser(this);
		mAudioPauser.pause();
		try {
			init(recognizerIntent, listener);
		} catch (RemoteException e) {
			handleRemoteException(e);
			return;
		}


		// TODO: test this
		/*
		if (checkCallingPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_DENIED) {
			handleError(listener, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
			return;
		}
		 */

		int sampleRate = Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyRecordingRate),
						getString(R.string.defaultRecordingRate)));


		mRecSessionBuilder.setContentType(sampleRate);
		if (Log.DEBUG) Log.i(mRecSessionBuilder.toStringArrayList());
		mRecSession = mRecSessionBuilder.build();
		try {
			mRecSession.create();
		} catch (IOException e) {
			handleError(listener, SpeechRecognizer.ERROR_NETWORK);
			return;
		} catch (NotAvailableException e) {
			// This cannot happen in the current net-speech-api?
			handleError(listener, SpeechRecognizer.ERROR_SERVER);
			return;
		}


		try {
			startRecording(sampleRate);
			Log.i("Callback: readyForSpeech: " + sampleRate);
			listener.readyForSpeech(new Bundle());
			// TODO: send it when the user actually started speaking
			Log.i("Callback: beginningOfSpeech");
			listener.beginningOfSpeech();
			startTasks();
		} catch (IOException e) {
			handleError(listener, SpeechRecognizer.ERROR_AUDIO);
		} catch (RemoteException e) {
			handleRemoteException(e);
		}
	}


	@Override
	protected void onStopListening(Callback listener) {
		Log.i("onStopListening");
		stopRecording(listener);
	}


	/**
	 * Note that we ignore the callback, it doesn't seem to make
	 * sense to call anything on cancel.
	 */
	@Override
	protected void onCancel(Callback listener) {
		Log.i("onCancel");
		releaseResources();
		mAudioPauser.resume();
	}


	@Override
	public void onDestroy() {
		Log.i("onDestroy");
		releaseResources();
		if (mAudioPauser != null) mAudioPauser.resume();
		super.onDestroy();
	}


	private void releaseResources() {
		stopTasks();
		if (mRecSession != null && ! mRecSession.isFinished()) {
			mRecSession.cancel();
		}
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
		if (mSendLooper != null) {
			mSendLooper.quit();
			mSendLooper = null;
		}
	}


	/**
	 * <p>Starts recording from the microphone with 16kHz sample rate.</p>
	 *
	 * @throws IOException if recorder could not be created
	 */
	private void startRecording(int sampleRate) throws IOException {
		mRecorder = new RawAudioRecorder(sampleRate);
		if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
			mRecorder = null;
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		if (mRecorder.getState() != RawAudioRecorder.State.READY) {
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		mRecorder.start();

		if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}
	}


	// TODO: maybe change the order of these calls
	private void stopRecording(Callback listener) {
		mRecorder.stop();
		stopTasks();
		mAudioPauser.resume();
		transcribeAndFinishInBackground(mRecorder.consumeRecording(), listener);
		try {
			Log.i("Callback: endOfSpeech");
			listener.endOfSpeech();
		} catch (RemoteException e) {
			handleRemoteException(e);
		}
	}


	/**
	 * @param bytes byte array representing the audio data
	 * @param isLast indicates that this is the last chunk that is sent
	 * @throws IOException
	 */
	private void sendChunk(byte[] bytes, boolean isLast) throws IOException {
		if (mRecSession != null && ! mRecSession.isFinished()) {
			mRecSession.sendChunk(bytes, isLast);
		}
	}


	private void startTasks() {
		mSendHandler.postDelayed(mSendTask, Constants.TASK_DELAY_SEND);
		mVolumeHandler.postDelayed(mShowVolumeTask, TASK_DELAY_VOL);
		mStopHandler.postDelayed(mStopTask, TASK_DELAY_STOP);
	}


	private void stopTasks() {
		if (mSendHandler != null) mSendHandler.removeCallbacks(mSendTask);
		if (mVolumeHandler != null) mVolumeHandler.removeCallbacks(mShowVolumeTask);
		if (mStopHandler != null) mStopHandler.removeCallbacks(mStopTask);
	}


	private boolean transcribeAndFinishInBackground(final byte[] bytes, final Callback listener) {
		Thread t = new Thread() {
			public void run() {
				try {
					try {
						sendChunk(bytes, true);
						getResult(mRecSession, listener);
					} catch (IOException e) {
						handleError(listener, SpeechRecognizer.ERROR_NETWORK);
					}
				} catch (RemoteException e) {
					handleRemoteException(e);
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
	private void getResult(RecSession recSession, Callback listener) throws RemoteException, IOException {
		RecSessionResult result = recSession.getResult();

		if (result == null) {
			Log.i("Callback: error: ERROR_NO_MATCH: RecSessionResult == null");
			listener.error(SpeechRecognizer.ERROR_NO_MATCH);
			return;
		}

		List<Hypothesis> hyps = result.getHypotheses();
		if (hyps.isEmpty()) {
			Log.i("Callback: error: ERROR_NO_MATCH: getHypotheses().isEmpty()");
			listener.error(SpeechRecognizer.ERROR_NO_MATCH);
			return;
		}

		int maxResults = mExtras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
		if (maxResults <= 0) {
			maxResults = hyps.size();
		}

		// Utterances OR linearizations
		ArrayList<String> lins = new ArrayList<String>();

		// Utterances and their linearizations in a flat serialization
		ArrayList<String> everything = new ArrayList<String>();
		ArrayList<Integer> counts = new ArrayList<Integer>(hyps.size());
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
		returnOrForwardMatches(everything, counts, lins, listener);
	}


	/**
	 * <p>Returns the transcription results to the caller,
	 * or sends them to the pending intent.</p>
	 *
	 * @param everything recognition results (all the components)
	 * @param counts number of linearizations for each hyphothesis (needed to interpret {@code everything})
	 * @param matches recognition results (just linearizations)
	 * @param listener listener that receives the matches
	 * @throws RemoteException 
	 */
	private void returnOrForwardMatches(ArrayList<String> everything, ArrayList<Integer> counts, ArrayList<String> matches, Callback listener) throws RemoteException {
		PendingIntent pendingIntent = Utils.getPendingIntent(mExtras);
		if (pendingIntent == null) {
			Bundle bundle = new Bundle();
			bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);
			bundle.putStringArrayList(Extras.RESULTS_RECOGNITION_LINEARIZATIONS, everything);
			bundle.putIntegerArrayList(Extras.RESULTS_RECOGNITION_LINEARIZATION_COUNTS, counts);
			Log.i("Callback: results: RESULTS_RECOGNITION: " + matches);
			Log.i("Callback: results: RESULTS_RECOGNITION_LINEARIZATIONS: " + everything);
			Log.i("Callback: results: RESULTS_RECOGNITION_LINEARIZATIONS_COUNTS: " + counts);
			listener.results(bundle);
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


	private void init(Intent recognizerIntent, final Callback listener) throws RemoteException {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		mExtras = recognizerIntent.getExtras();
		if (mExtras == null) {
			// For some reason getExtras() can return null, we map it
			// to an empty Bundle if this occurs.
			mExtras = new Bundle();
		}

		try {
			mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, mExtras, null);
		} catch (MalformedURLException e) {
			// The user has managed to store a malformed URL in the configuration.
			Log.i("Callback: error: ERROR_CLIENT");
			listener.error(SpeechRecognizer.ERROR_CLIENT);
			return;
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
				if (mRecorder != null) {
					try {
						// TODO: Currently returns 16-bit LE
						byte[] buffer = mRecorder.consumeRecording();
						try {
							sendChunk(buffer, false);
							Log.i("Callback: bufferReceived: length = " + buffer.length);
							// TODO: Expects 16-bit BE
							listener.bufferReceived(buffer);
							mSendHandler.postDelayed(this, Constants.TASK_INTERVAL_SEND);
						} catch (IOException e) {
							handleError(listener, SpeechRecognizer.ERROR_NETWORK);
						}
					} catch (RemoteException e) {
						handleRemoteException(e);
					}
				}
			}
		};


		// Monitor the volume level
		mShowVolumeTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						float rmsdb = mRecorder.getRmsdb();
						Log.i("Callback: rmsChanged: " + rmsdb);
						listener.rmsChanged(rmsdb);
						mVolumeHandler.postDelayed(this, TASK_INTERVAL_VOL);
					} catch (RemoteException e) {
						handleRemoteException(e);
					}
				}
			}
		};


		// Check if we should stop recording
		mStopTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					if (
							mTimeToFinish < SystemClock.uptimeMillis() ||
							mPrefs.getBoolean("keyAutoStopAfterPause", true) && mRecorder.isPausing()
							) {
						stopRecording(listener);
					} else {
						mStopHandler.postDelayed(this, TASK_INTERVAL_STOP);
					}
				}
			}
		};
	}


	private void handleError(final Callback listener, int errorCode) {
		releaseResources();
		try {
			Log.i("Callback: error: " + errorCode);
			if (mAudioPauser != null) mAudioPauser.resume();
			listener.error(errorCode);
		} catch (RemoteException e) {
			handleRemoteException(e);
		}
	}


	/**
	 * About RemoteException see
	 * http://stackoverflow.com/questions/3156389/android-remoteexceptions-and-services
	 */
	private void handleRemoteException(RemoteException e) {
		Log.e(e.getMessage());
	}
}
