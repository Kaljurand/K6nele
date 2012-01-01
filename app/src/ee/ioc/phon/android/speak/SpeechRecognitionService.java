/*
 * Copyright 2011, Institute of Cybernetics at Tallinn University of Technology
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
import java.util.UUID;

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

// TODO: max recording time, maybe calculate the amount of sends
// delay + x * interval = max_recording_time --> (max-delay)/interval

// TODO: keep track of callers

// TODO: send correct dB

// TODO: send correct waveform data
public class SpeechRecognitionService extends RecognitionService {

	private static final String LOG_TAG = SpeechRecognitionService.class.getName();

	// Send the chunk every 2 seconds
	private static final int TASK_INTERVAL_SEND = 2000;
	private static final int TASK_DELAY_SEND = TASK_INTERVAL_SEND;

	// Check the volume / pauses 10 times a second
	private static final int TASK_INTERVAL_VOL = 100;
	// Wait for 1 sec before starting to measure the volume
	private static final int TASK_DELAY_VOL = 1000;


	private String mUniqueId;

	private SharedPreferences mPrefs;

	private Handler mVolumeHandler = new Handler();
	private volatile Looper mSendLooper;
	private volatile Handler mSendHandler;

	private Runnable mSendTask;
	private Runnable mShowVolumeTask;

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

	@Override
	protected void onCancel(Callback listener) {
		releaseResources();
	}

	@Override
	protected void onStartListening(Intent recognizerIntent, final Callback listener) {
		try {
			myCreate(recognizerIntent, listener);
			myStart(listener);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}


		// TODO: test this
		/*
		if (checkCallingPermission("android.permission.RECORD_AUDIO") == PackageManager.PERMISSION_DENIED) {
			try {
				listener.error(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		 */

		int sampleRate = Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyRecordingRate),
						getString(R.string.defaultRecordingRate)));

		try {
			createRecSession(Utils.getContentType(sampleRate), listener);
		} catch (RemoteException e2) {
			e2.printStackTrace();
		}

		try {
			startRecording(sampleRate);
			listener.readyForSpeech(new Bundle());
			// TODO: send it when the user actually started speaking
			listener.beginningOfSpeech();
			startTasks();
		} catch (IOException e) {
			try {
				listener.error(SpeechRecognizer.ERROR_AUDIO);
			} catch (RemoteException e1) {
				e1.printStackTrace();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
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
		mSendLooper.quit();
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

		mRecorder.prepare();

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
		try {
			listener.endOfSpeech();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		mRecorder.stop();
		stopTasks();
		transcribeAndFinishInBackground(mRecorder.consumeRecording(), listener);
	}


	/**
	 * @param bytes byte array representing the audio data
	 * @param isLast indicates that this is the last chunk that is sent
	 * @throws RemoteException 
	 */
	private void sendChunk(byte[] bytes, boolean isLast, final Callback listener) throws RemoteException {
		if (bytes != null && bytes.length > 0 && ! mRecSession.isFinished()) {
			try {
				mRecSession.sendChunk(bytes, isLast);
			} catch (IOException e) {
				listener.error(SpeechRecognizer.ERROR_NETWORK);
			}
		}
	}


	private void startTasks() {
		mSendHandler.postDelayed(mSendTask, TASK_DELAY_SEND);
		mVolumeHandler.postDelayed(mShowVolumeTask, TASK_DELAY_VOL);
	}


	private void stopTasks() {
		mSendHandler.removeCallbacks(mSendTask);
		mVolumeHandler.removeCallbacks(mShowVolumeTask);
	}


	private boolean transcribeAndFinishInBackground(final byte[] bytes, final Callback listener) {
		Thread t = new Thread() {
			public void run() {
				try {
					sendChunk(bytes, true, listener);
					getResult(mRecSession, listener);
					// At this point we don't need the resources anymore
					releaseResources();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		return true;
	}


	private boolean getResult(RecSession recSession, Callback listener) throws RemoteException {
		RecSessionResult result = null;
		try {
			result = recSession.getResult();
		} catch (IOException e) {
			listener.error(SpeechRecognizer.ERROR_NETWORK);
			return false;
		}

		if (result == null || result.getLinearizations().isEmpty()) {
			listener.error(SpeechRecognizer.ERROR_NO_MATCH);
			return false;
		} else {
			ArrayList<String> matches = new ArrayList<String>();
			matches.addAll(result.getLinearizations());
			returnOrForwardMatches(matches, listener);
			return true;
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
	 * @param handler message handler
	 * @param matches transcription results (one or more)
	 */
	private void returnOrForwardMatches(ArrayList<String> matches, Callback listener) {
		// Throw away matches that the user is not interested in
		if (mExtraMaxResults > 0 && matches.size() > mExtraMaxResults) {
			matches.subList(mExtraMaxResults, matches.size()).clear();
		}

		if (mExtraResultsPendingIntent == null) {
			Bundle bundle = new Bundle();
			bundle.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);

			try {
				listener.results(bundle);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
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


	private void myCreate(Intent recognizerIntent, final Callback listener) throws RemoteException {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		SharedPreferences settings = getSharedPreferences(getString(R.string.filePreferences), 0);
		mUniqueId = settings.getString("id", null);
		if (mUniqueId == null) {
			mUniqueId = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("id", mUniqueId);
			editor.commit();	
		}

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

		PackageNameRegistry wrapper = new PackageNameRegistry(this, getCaller());

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
	}


	private void myStart(final Callback listener) throws RemoteException {

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
		mRecSession.setUserAgentComment(makeUserAgentComment());

		// This task talks to the internet. It is therefore potentially slow and
		// should not be run in the UI thread.
		mSendTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						// TODO: Currently returns 16-bit LE
						byte[] buffer = mRecorder.consumeRecording();
						sendChunk(buffer, false, listener);
						// TODO: Expects 16-bit BE
						listener.bufferReceived(buffer);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					Log.i(LOG_TAG, "Send chunk completed");
					mSendHandler.postDelayed(this, TASK_INTERVAL_SEND);
				}
			}
		};


		// Monitor the volume level
		mShowVolumeTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						float rmsdb = mRecorder.getRmsdb();
						Log.i(LOG_TAG, "RMS DB: " + rmsdb);
						listener.rmsChanged(rmsdb);
					} catch (RemoteException e) {
						e.printStackTrace();
					}

					if (
							mTimeToFinish < SystemClock.uptimeMillis() ||
							mPrefs.getBoolean("keyAutoStopAfterPause", true) && mRecorder.isPausing()
					) {
						stopRecording(listener);
					} else {
						mVolumeHandler.postDelayed(this, TASK_INTERVAL_VOL);
					}
				}
			}
		};
	}


	// TODO: maybe replace by "SpeechRecognitionService/"
	private String makeUserAgentComment() {
		return 	"RecognizerIntentActivity/" + Utils.getVersionName(this) + "; " + mUniqueId + "; " + getCaller();
	}


	/**
	 * <p>Returns the package name of the app that receives the transcription,
	 * or <code>null</code> if the package name could not be resolved.</p>
	 *
	 * <p>We use EXTRA_CALLING_PACKAGE because there does not seem to be a way to
	 * find out which Activity called us, i.e. this does not work:</p>
	 *
	 * <pre>
	 * ComponentName callingActivity = getCallingActivity();
	 * if (callingActivity != null) {
	 *     return callingActivity.getPackageName();
	 * }
	 * </pre>
	 */
	private String getCaller() {
		if (mExtraResultsPendingIntent == null) {
			String caller = mExtras.getString(RecognizerIntent.EXTRA_CALLING_PACKAGE);
			if (caller == null) {
				return "unknownkeyboard";
			}
			return caller;
		}
		return mExtraResultsPendingIntent.getTargetPackage();
	}


	/**
	 * <p>Tries to create a speech recognition session and returns <code>true</code>
	 * if succeeds. Otherwise sends out an error message and returns <code>false</code>.
	 * Set the content-type to <code>null</code> if you want to use the default
	 * net-speech-api content type (raw audio).</p>
	 * 
	 * @param contentType content type of the audio (e.g. "audio/x-flac;rate=16000")
	 * @return <code>true</code> iff success
	 * @throws RemoteException 
	 */
	private boolean createRecSession(String contentType, Callback listener) throws RemoteException {
		try {
			if (contentType != null) {
				mRecSession.setContentType(contentType);
			}
			mRecSession.create();
			return true;
		} catch (IOException e) {
			listener.error(SpeechRecognizer.ERROR_NETWORK);
		} catch (NotAvailableException e) {
			// This cannot happen in the current net-speech-api?
			listener.error(SpeechRecognizer.ERROR_SERVER);
		}
		return false;
	}
}