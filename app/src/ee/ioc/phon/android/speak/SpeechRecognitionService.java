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
import java.net.URL;
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
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionService;
import android.util.Log;

/**
 * TODO: send correct waveform data
 *
 * @author Kaarel Kaljurand
 */
public class SpeechRecognitionService extends RecognitionService {

	private static final String LOG_TAG = SpeechRecognitionService.class.getName();

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

	private String mGrammarUrl;
	private String mGrammarTargetLang;
	private String mServerUrl;

	private int mExtraMaxResults = 0;
	private PendingIntent mExtraResultsPendingIntent;
	private Bundle mExtraResultsPendingIntentBundle;
	private ChunkedWebRecSession mRecSession;

	private RawAudioRecorder mRecorder;

	private Bundle mExtras;

	private Caller mCaller;

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


		try {
			mRecSession.setContentType(Utils.getContentType(sampleRate));
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
		Log.i(LOG_TAG, "Releasing resources");
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
		if (mExtraMaxResults > 0 && matches.size() > mExtraMaxResults) {
			matches.subList(mExtraMaxResults, matches.size()).clear();
		}

		if (mExtraResultsPendingIntent == null) {
			Bundle bundle = new Bundle();
			bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);
			listener.results(bundle);
		} else {
			if (mExtraResultsPendingIntentBundle == null) {
				mExtraResultsPendingIntentBundle = new Bundle();
			}
			String match = matches.get(0);
			//mExtraResultsPendingIntentBundle.putString(SearchManager.QUERY, match);
			Intent intent = new Intent();
			intent.putExtras(mExtraResultsPendingIntentBundle);
			// This is for Google Maps, YouTube, ...
			intent.putExtra(SearchManager.QUERY, match);
			// This is for SwiftKey X, ...
			intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
			try {
				// TODO: dummy number 1234
				mExtraResultsPendingIntent.send(this, 1234, intent);
			} catch (CanceledException e) {
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
		} else {
			mExtraMaxResults = mExtras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
			mExtraResultsPendingIntentBundle = mExtras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE);

			Parcelable extraResultsPendingIntentAsParceable = mExtras.getParcelable(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT);
			if (extraResultsPendingIntentAsParceable != null) {
				//PendingIntent.readPendingIntentOrNullFromParcel(mExtraResultsPendingIntent);
				if (extraResultsPendingIntentAsParceable instanceof PendingIntent) {
					mExtraResultsPendingIntent = (PendingIntent) extraResultsPendingIntentAsParceable;
				} else {
					listener.error(SpeechRecognizer.ERROR_CLIENT);
					return;
				}
			}
		}

		mTimeToFinish = SystemClock.uptimeMillis() + 1000 * Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyAutoStopAfterTime),
						getString(R.string.defaultAutoStopAfterTime)));

		mCaller = new Caller(mExtraResultsPendingIntent, mExtras);
		PackageNameRegistry wrapper = new PackageNameRegistry(this, mCaller.getActualCaller());

		// If the user has not overridden the grammar then use the app's EXTRA.
		mGrammarUrl = Utils.chooseValue(wrapper.getGrammarUrl(), mExtras.getString(Extras.EXTRA_GRAMMAR_URL));
		mGrammarTargetLang = Utils.chooseValue(wrapper.getGrammarLang(), mExtras.getString(Extras.EXTRA_GRAMMAR_TARGET_LANG));
		// The server URL should never be null
		mServerUrl = Utils.chooseValue(
				wrapper.getServerUrl(),
				mExtras.getString(Extras.EXTRA_SERVER_URL),
				mPrefs.getString(getString(R.string.keyService), getString(R.string.defaultService))
				);

		// Starting chunk sending in a separate thread so that slow internet
		// would not block the UI.
		HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mSendLooper = thread.getLooper();
		mSendHandler = new Handler(mSendLooper);

		URL wsUrl = null;
		URL lmUrl = null;

		try {
			wsUrl = new URL(mServerUrl);
			if (mGrammarUrl != null && mGrammarUrl.length() > 0) {
				lmUrl = new URL(mGrammarUrl);
			}
		} catch (MalformedURLException e) {
			// The user has managed to store a malformed URL in the configuration.
			listener.error(SpeechRecognizer.ERROR_CLIENT);
			return;
		}

		int nbest = (mExtraMaxResults > 1) ? mExtraMaxResults : 1;
		mRecSession = new ChunkedWebRecSession(wsUrl, lmUrl, mGrammarTargetLang, nbest);
		Log.i(LOG_TAG, "Created ChunkedWebRecSession: " + wsUrl + ": lm=" + lmUrl + ": lang=" + mGrammarTargetLang + ": nbest=" + nbest);
		String userAgentComment = Utils.makeUserAgentComment(this, mCaller.toString());
		mRecSession.setUserAgentComment(userAgentComment);
		Log.i(LOG_TAG, "User Agent comment: " + userAgentComment);
		mRecSession.setDeviceId(Utils.getUniqueId(getSharedPreferences(getString(R.string.filePreferences), 0)));
		String phrase = mExtras.getString(Extras.EXTRA_PHRASE);
		if (phrase != null) {
			mRecSession.setPhrase(phrase);
		}

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
							Log.i(LOG_TAG, "Send chunk completed");
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
		Log.e(LOG_TAG, e.getMessage());
	}
}
