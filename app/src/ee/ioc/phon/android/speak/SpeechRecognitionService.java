/*
 * Copyright 2011-2012, Institute of Cybernetics at Tallinn University of Technology
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

import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;
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


	@Override
	protected void onCancel(Callback listener) {
		releaseResources();
	}

	@Override
	protected void onStartListening(Intent recognizerIntent, final Callback listener) {
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
			listener.readyForSpeech(new Bundle());
			// TODO: send it when the user actually started speaking
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
		stopRecording(listener);
	}


	@Override
	public void onDestroy() {
		releaseResources();
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
		transcribeAndFinishInBackground(mRecorder.consumeRecording(), listener);
		try {
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
		mSendHandler.removeCallbacks(mSendTask);
		mVolumeHandler.removeCallbacks(mShowVolumeTask);
		mStopHandler.removeCallbacks(mStopTask);
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


	private void getResult(RecSession recSession, Callback listener) throws RemoteException, IOException {
		RecSessionResult result = recSession.getResult();

		if (result == null || result.getLinearizations().isEmpty()) {
			listener.error(SpeechRecognizer.ERROR_NO_MATCH);
		} else {
			ArrayList<String> matches = new ArrayList<String>();
			matches.addAll(result.getLinearizations());
			returnOrForwardMatches(matches, listener);
		}
	}


	/**
	 * <p>Returns the transcription results (matches) to the caller,
	 * or sends them to the pending intent. In the latter case we also display
	 * a toast-message with the transcription.
	 * Note that we assume that the given list of matches contains at least one
	 * element.</p>
	 *
	 * TODO: the pending intent result code is currently set to 1234 (don't know what this means)
	 *
	 * @param matches transcription results (one or more)
	 * @param listener listener that receives the matches
	 * @throws RemoteException 
	 */
	private void returnOrForwardMatches(ArrayList<String> matches, Callback listener) throws RemoteException {
		// Throw away matches that the user is not interested in
		int maxResults = mExtras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
		if (maxResults > 0 && matches.size() > maxResults) {
			matches.subList(maxResults, matches.size()).clear();
		}

		PendingIntent pendingIntent = Utils.getPendingIntent(mExtras);
		if (pendingIntent == null) {
			Bundle bundle = new Bundle();
			bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);
			listener.results(bundle);
		} else {
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
							// TODO: Expects 16-bit BE
							listener.bufferReceived(buffer);
							Log.i("Send chunk completed");
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
