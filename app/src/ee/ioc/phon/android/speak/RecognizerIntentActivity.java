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

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.PendingIntent.CanceledException;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;

import android.preference.PreferenceManager;
import android.util.Log;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;
import android.widget.Chronometer.OnChronometerTickListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;
import ee.ioc.phon.netspeechapi.recsession.NotAvailableException;
import ee.ioc.phon.netspeechapi.recsession.RecSession;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;


/**
 * <p>This activity responds to the RecognizerIntent.ACTION_RECOGNIZE_SPEECH intent.
 * We have tried to implement the complete interface of RecognizerIntent as of API level 7 (v2.1).</p>
 * 
 * <p>It records audio, transcribes it using a speech-to-text server
 * and returns the result as a list of Strings, or an error code.</p>
 * 
 * <p>This activity rewrites the error codes which originally come from the
 * speech recognizer webservice (and which are then rewritten by the net-speech-api)
 * to the RecognizerIntent result error codes, which the eventual app will react to
 * by displaying them somehow to the user. The RecognizerIntent error codes are the
 * following (with my interpretation after the colon):</p>
 * 
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
public class RecognizerIntentActivity extends Activity {

	private static final String LOG_TAG = RecognizerIntentActivity.class.getName();

	// Send the chunk every 2 seconds
	private static final int TASK_INTERVAL_SEND = 2000;
	private static final int TASK_DELAY_SEND = TASK_INTERVAL_SEND;

	// Update the byte count every second
	private static final int TASK_INTERVAL_STAT = 1000;
	// Start the task almost immediately
	private static final int TASK_DELAY_STAT = 100;

	// Check the volume / pauses 10 times a second
	private static final int TASK_INTERVAL_VOL = 100;
	// Wait for 1 sec before starting to measure the volume
	private static final int TASK_DELAY_VOL = 1000;

	private static final int DELAY_AFTER_START_BEEP = 200;

	private static final String MSG = "MSG";
	private static final int MSG_TOAST = 1;
	private static final int MSG_CHUNKS = 2;
	private static final int MSG_RESULT_ERROR = 3;

	// The amplitude is either short (16-bit) or byte (8-bit)
	private static final double LOG_OF_MAX_VOLUME = Math.log10((double) Short.MAX_VALUE);
	private static final String MAX_BAR = "||||||||||||||||||||";

	private String mUniqueId;

	private SharedPreferences mPrefs;

	private TextView mTvPrompt;
	private Button mBStartStop;
	private LinearLayout mLlTranscribing;
	private LinearLayout mLlProgress;
	private LinearLayout mLlError;
	private TextView mTvBytes;
	private Chronometer mChronometer;
	private ImageView mIvWaveform;
	private TextView mTvChunks;
	private TextView mTvErrorMessage;

	private SimpleMessageHandler mMessageHandler = new SimpleMessageHandler();
	private Handler mStatusHandler = new Handler();
	private Handler mVolumeHandler = new Handler();
	private volatile Looper mSendLooper;
	private volatile Handler mSendHandler;

	private Runnable mSendTask;
	private Runnable mShowStatusTask;
	private Runnable mShowVolumeTask;

	// Max recording time in milliseconds
	private int mMaxRecordingTime;

	private URL mServerUrl;
	private URL mGrammarUrl;
	private String mGrammarTargetLang;

	private Resources mRes;

	private int mExtraMaxResults = 0;
	private PendingIntent mExtraResultsPendingIntent;
	private Bundle mExtraResultsPendingIntentBundle;
	private ChunkedWebRecSession mRecSession;

	private RawAudioRecorder mRecorder;

	private Bundle mExtras;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.recognizer);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		SharedPreferences settings = getSharedPreferences(getString(R.string.filePreferences), 0);
		mUniqueId = settings.getString("id", null);
		if (mUniqueId == null) {
			//TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
			//mUniqueId = tm.getDeviceId();
			mUniqueId = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("id", mUniqueId);
			editor.commit();	
		}

		// Don't shut down the screen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mTvPrompt = (TextView) findViewById(R.id.tvPrompt);
		mBStartStop = (Button) findViewById(R.id.bStartStop);
		mLlTranscribing = (LinearLayout) findViewById(R.id.llTranscribing);
		mLlProgress = (LinearLayout) findViewById(R.id.llProgress);
		mLlError = (LinearLayout) findViewById(R.id.llError);
		mTvBytes = (TextView) findViewById(R.id.tvBytes);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);
		mIvWaveform = (ImageView) findViewById(R.id.ivWaveform);
		mTvChunks = (TextView) findViewById(R.id.tvChunks);
		mTvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);

		mExtras = getIntent().getExtras();
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
					handleResultError(mMessageHandler, RecognizerIntent.RESULT_CLIENT_ERROR, getString(R.string.errorBadPendingIntent), null);
					return;
				}
			}
		}

		setPrompt();

		mMaxRecordingTime = 1000 * Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyAutoStopAfterTime),
						getString(R.string.defaultAutoStopAfterTime)));

		mRes = getResources();

		PackageNameRegistry wrapper = new PackageNameRegistry(this, getCaller());

		try {
			setUrls(wrapper);
		} catch (MalformedURLException e) {
			// The user has managed to store a malformed URL in the configuration.
			handleResultError(mMessageHandler, RecognizerIntent.RESULT_CLIENT_ERROR, "", e);
		}

		mGrammarTargetLang = Utils.chooseValue(wrapper.getGrammarLang(), mExtras.getString(Extras.EXTRA_GRAMMAR_TARGET_LANG));

		// Starting chunk sending in a separate thread so that slow internet
		// would not block the UI.
		HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mSendLooper = thread.getLooper();
		mSendHandler = new Handler(mSendLooper);
	}


	@Override
	public void onStart() {
		super.onStart();

		// This task talks to the internet. It is therefore potentially slow and
		// should not be run in the UI thread.
		mSendTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					sendChunk(mMessageHandler, mRecorder.consumeRecording(), false);
					mSendHandler.postDelayed(this, TASK_INTERVAL_SEND);
				}
			}
		};

		// Show the file size
		mShowStatusTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					mTvBytes.setText(Utils.getSizeAsString(mRecorder.getLength()));
					mStatusHandler.postDelayed(this, TASK_INTERVAL_STAT);
				}
			}
		};

		// Show the max volume
		mShowVolumeTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					//int maxAmplitude = mRecorder.getMaxAmplitude();
					//mVolume.setText(makeBar(scaleVolume(maxAmplitude)));
					//byte[] bytes = mRecorder.getCurrentRecording();
					//int end = bytes.length;
					//int start = end - 32 * TASK_INTERVAL_VOL;
					//mIvWaveform.setImageBitmap(Utils.drawWaveform(bytes, 150, 50, start, end));
					if (mPrefs.getBoolean("keyAutoStopAfterPause", true) && mRecorder.isPausing()) {
						stopRecording();
					} else {
						mVolumeHandler.postDelayed(this, TASK_INTERVAL_VOL);
					}
				}
			}
		};


		mBStartStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mRecorder == null) {
					startRecordingOrFinish();
				} else {
					stopRecording();
				}
			}
		});


		mChronometer.setOnChronometerTickListener(new OnChronometerTickListener() {                      
			@Override
			public void onChronometerTick(Chronometer chronometer) {
				long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
				if (elapsedMillis > mMaxRecordingTime) {
					//mMessageHandler.sendMessage(createMessage(getString(R.string.noteMaxRecordingTimeExceeded)));
					stopRecording();
				}
			}
		});


		if (mPrefs.getBoolean("keyAutoStart", false)) {
			startRecordingOrFinish();
		} else {
			mBStartStop.setText(getString(R.string.buttonSpeak));
			mBStartStop.setVisibility(View.VISIBLE);
		}
	}


	@Override
	public void onStop() {
		super.onStop();
		releaseResources();
		mSendLooper.quit();
	}


	/**
	 * <p>We have to call the super otherwise the app crashes
	 * with the "super not called" exception.</p>
	 */
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(LOG_TAG, "Configuration changed");
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.recognizer, menu);
		return true;
	}


	/**
	 * The menu is only for developers.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuRecognizerShowInput:
			Intent details = new Intent(this, DetailsActivity.class);
			details.putExtra(DetailsActivity.EXTRA_TITLE, (String) null);
			details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, getDetails());
			startActivity(details);
			return true;
		case R.id.menuRecognizerTest1:
			if (! transcribeFile("test_kaks_minutit_sekundites.flac", "audio/x-flac;rate=16000")) {
				finish();
			}
			return true;
		case R.id.menuRecognizerTest3:
			returnOrForwardMatches(mMessageHandler,
					new ArrayList<String>(
							Arrays.asList(mRes.getStringArray(R.array.entriesTestResult))));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	private void setRecorderStyle(int color) {
		mTvBytes.setTextColor(color);
		mChronometer.setTextColor(color);
	}


	// TODO: maybe change the order of these calls
	private void stopRecording() {
		mRecorder.stop();
		setGuiStoppedRecording();
		playStopSound();
		transcribeAndFinishInBackground(mMessageHandler, mRecorder.consumeRecording());
	}


	private void startTasks() {
		mStatusHandler.postDelayed(mShowStatusTask, TASK_DELAY_STAT);
		mVolumeHandler.postDelayed(mShowVolumeTask, TASK_DELAY_VOL);
		mSendHandler.postDelayed(mSendTask, TASK_DELAY_SEND);
	}


	private void stopTasks() {
		mStatusHandler.removeCallbacks(mShowStatusTask);
		mVolumeHandler.removeCallbacks(mShowVolumeTask);
		mSendHandler.removeCallbacks(mSendTask);
	}


	private void setGuiInitOnError(String msgAsString) {
		mLlTranscribing.setVisibility(View.GONE);
		mIvWaveform.setVisibility(View.GONE);
		// includes: bytes, chronometer, chunks
		mLlProgress.setVisibility(View.GONE);
		setPrompt();
		mBStartStop.setText(getString(R.string.buttonSpeak));
		mBStartStop.setVisibility(View.VISIBLE);
		mLlError.setVisibility(View.VISIBLE);
		mTvErrorMessage.setText(msgAsString);
	}


	private void setGuiRecording() {
		startChronometer();
		mTvChunks.setText("");
		mLlProgress.setVisibility(View.VISIBLE);
		mLlError.setVisibility(View.GONE);
		setRecorderStyle(mRes.getColor(R.color.red));
		if (mPrefs.getBoolean("keyAutoStopAfterPause", true)) {
			mBStartStop.setVisibility(View.INVISIBLE);
		} else {
			mBStartStop.setText(getString(R.string.buttonStop));
			mBStartStop.setVisibility(View.VISIBLE);
		}
		startTasks();
	}


	private void setGuiStoppedRecording() {
		stopChronometer();
		setRecorderStyle(mRes.getColor(R.color.grey2));
		mBStartStop.setVisibility(View.GONE);
		stopTasks();
	}


	private void setGuiTranscribing() {
		mTvPrompt.setVisibility(View.GONE);
		mLlTranscribing.setVisibility(View.VISIBLE);
		if (mRecorder != null) {
			byte[] bytes = mRecorder.getCurrentRecording();
			int w = ((View) mIvWaveform.getParent()).getWidth();
			if (w > 0) {
				mIvWaveform.setImageBitmap(Utils.drawWaveform(bytes, w, (int) (w / 2.5), 0, bytes.length));
			}
		}
	}


	private void setPrompt() {
		String prompt = mExtras.getString(RecognizerIntent.EXTRA_PROMPT);
		if (prompt == null || prompt.length() == 0) {
			mTvPrompt.setVisibility(View.INVISIBLE);
		} else {
			mTvPrompt.setText(prompt);
			mTvPrompt.setVisibility(View.VISIBLE);
		}
	}


	private void stopChronometer() {
		mChronometer.stop();
	}


	private void startChronometer() {
		mChronometer.setBase(SystemClock.elapsedRealtime());
		mChronometer.start();
	}


	/**
	 * 1. Beep
	 * 2. Wait until the beep has stopped
	 * 3. Set up the recorder and start recording (finish if failed)
	 * 4. Create the HTTP-connection to the recognition server
	 * 5. Update the GUI to show that the recording is in progress
	 */
	private void startRecordingOrFinish() {
		int sampleRate = Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyRecordingRate),
						getString(R.string.defaultRecordingRate)));
		boolean success = createRecSession(Utils.getContentType(sampleRate));

		if (success) {
			try {
				startRecording(sampleRate);
				setGuiRecording();
			} catch (IOException e) {
				handleResultError(mMessageHandler, RecognizerIntent.RESULT_AUDIO_ERROR, "recorder", e);
			}
		}
	}


	/**
	 * <p>Starts recording from the microphone with 16kHz sample rate.</p>
	 *
	 * @throws IOException if recorder could not be created
	 */
	private void startRecording(int recordingRate) throws IOException {
		mRecorder = new RawAudioRecorder(recordingRate);
		if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
			mRecorder = null;
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		mRecorder.prepare();

		if (mRecorder.getState() != RawAudioRecorder.State.READY) {
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		playStartSound();

		SystemClock.sleep(DELAY_AFTER_START_BEEP);

		mRecorder.start();

		if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}
	}


	private void setResultIntent(ArrayList<String> matches) {
		Intent intent = new Intent();
		intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
		setResult(Activity.RESULT_OK, intent);
	}


	private void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}


	private boolean transcribeAndFinishInBackground(final Handler handler, final byte[] bytes) {
		Thread t = new Thread() {
			public void run() {
				sendChunk(handler, bytes, true);
				getResult(handler, mRecSession);
			}
		};
		t.start();
		setGuiTranscribing();
		return true;
	}


	private void playStartSound() {
		playSound(R.raw.explore_begin);
	}


	private void playStopSound() {
		playSound(R.raw.explore_end);
	}


	private void playSound(int sound) {
		if (mPrefs.getBoolean("keyAudioCues", true)) {
			MediaPlayer.create(this, sound).start();
		}
	}


	/**
	 * <p>Only for developers, i.e. we are not going to localize these strings.</p>
	 */
	private String[] getDetails() {
		String callingActivityClassName = null;
		String callingActivityPackageName = null;
		String pendingIntentTargetPackage = null;
		ComponentName callingActivity = getCallingActivity();
		if (callingActivity != null) {
			callingActivityClassName = callingActivity.getClassName();
			callingActivityPackageName = callingActivity.getPackageName();
		}
		if (mExtraResultsPendingIntent != null) {
			pendingIntentTargetPackage = mExtraResultsPendingIntent.getTargetPackage();
		}
		String pendingBundle = null;
		if (mExtraResultsPendingIntentBundle != null) {
			pendingBundle = mExtraResultsPendingIntentBundle.keySet().toString();
		}
		return new String[] {
				"ID: " + mUniqueId,
				"User-Agent comment: " + makeUserAgentComment(),
				"Calling activity class name: " + callingActivityClassName,
				"Calling activity package name: " + callingActivityPackageName,
				"Pending intent target package: " + pendingIntentTargetPackage,
				"Selected grammar: " + mGrammarUrl,
				"Selected target lang: " + mGrammarTargetLang,
				"Selected server: " + mServerUrl,
				"Intent extras: " + mExtras.keySet().toString(),
				"LANGUAGE_MODEL: " + mExtras.getString(RecognizerIntent.EXTRA_LANGUAGE_MODEL),
				"LANGUAGE: " + mExtras.getString(RecognizerIntent.EXTRA_LANGUAGE),
				"MAX_RESULTS: " + mExtraMaxResults,
				"PROMPT: " + mExtras.getString(RecognizerIntent.EXTRA_PROMPT),
				"RESULTS_PENDING_INTENT: " + mExtraResultsPendingIntent,
				"RESULTS_PENDING_INTENT_BUNDLE: " + pendingBundle,
				"SERVER_URL: " + mExtras.getString(Extras.EXTRA_SERVER_URL),
				"GRAMMAR_URL: " + mExtras.getString(Extras.EXTRA_GRAMMAR_URL),
				"GRAMMAR_TARGET_LANG: " + mExtras.getString(Extras.EXTRA_GRAMMAR_TARGET_LANG)
		};
	}


	/**
	 * <p>This method is called onStop, i.e. when the user presses HOME,
	 * or presses BACK, or a call comes in, etc. We do not continue the activity
	 * in the background, instead we kill all the ongoing processes.</p>
	 * 
	 * <p>We kill the running processes in this order: GUI tasks,
	 * recognizer session, audio recorder.</p>
	 * 
	 * <p>Note that mRecorder.release() can be called in any state.
	 * After that the recorder object is no longer available,
	 * so we should set it to <code>null</code>.</p>
	 */
	private void releaseResources() {
		setGuiStoppedRecording();

		if (mRecSession != null && ! mRecSession.isFinished()) {
			mRecSession.cancel();
		}
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
	}


	private static Message createMessage(int type, String str) {
		Bundle b = new Bundle();
		b.putString(MSG, str);
		Message msg = Message.obtain();
		msg.what = type;
		msg.setData(b);
		return msg;
	}


	public class SimpleMessageHandler extends Handler {
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			String msgAsString = b.getString(MSG);
			switch (msg.what) {
			case MSG_TOAST:
				toast(msgAsString);
				break;
			case MSG_CHUNKS:
				mTvChunks.setText(mTvChunks.getText() + msgAsString);
				break;
			case MSG_RESULT_ERROR:
				setGuiInitOnError(msgAsString);
				break;
			}
		}
	}


	/**
	 * <p>Tries to create a speech recognition session and returns <code>true</code>
	 * if succeeds. Otherwise sends out an error message and returns <code>false</code>.
	 * Set the content-type to <code>null</code> if you want to use the default
	 * net-speech-api content type (raw audio).</p>
	 * 
	 * @param contentType content type of the audio (e.g. "audio/x-flac;rate=16000")
	 * @return <code>true</code> iff success
	 */
	private boolean createRecSession(String contentType) {
		int nbest = (mExtraMaxResults > 1) ? mExtraMaxResults : 1;
		mRecSession = new ChunkedWebRecSession(mServerUrl, mGrammarUrl, mGrammarTargetLang, nbest);
		Log.i(LOG_TAG, "Created ChunkedWebRecSession: " + mServerUrl + ": lm=" + mGrammarUrl + ": lang=" + mGrammarTargetLang + ": nbest=" + nbest);
		mRecSession.setUserAgentComment(makeUserAgentComment());
		try {
			if (contentType != null) {
				mRecSession.setContentType(contentType);
			}
			mRecSession.create();
			Log.i(LOG_TAG, "Created recognition session: " + contentType);
			return true;
		} catch (IOException e) {
			setResultNetworkError(mMessageHandler, "create", e);
		} catch (NotAvailableException e) {
			// This cannot happen in the current net-speech-api?
			handleResultError(mMessageHandler, RecognizerIntent.RESULT_SERVER_ERROR, "create", e);
		}
		return false;
	}


	/**
	 * 
	 * @param handler message handler
	 * @param bytes byte array representing the audio data
	 * @param isLast indicates that this is the last chunk that is sent
	 */
	private void sendChunk(Handler handler, byte[] bytes, boolean isLast) {
		if (bytes != null && bytes.length > 0 && ! mRecSession.isFinished()) {
			try {
				mRecSession.sendChunk(bytes, isLast);
				handler.sendMessage(createMessage(MSG_CHUNKS, "."));
				Log.i(LOG_TAG, "Send chunk: " + bytes.length);
			} catch (IOException e) {
				setResultNetworkError(handler, "sendChunk", e);
			}
		}
	}


	private void getResult(Handler handler, RecSession recSession) {
		RecSessionResult result = null;
		try {
			result = recSession.getResult();
		} catch (IOException e) {
			releaseResources();
			setResultNetworkError(handler, "getResult", e);
		}

		if (result == null || result.getLinearizations().isEmpty()) {
			releaseResources();
			handleResultError(handler, RecognizerIntent.RESULT_NO_MATCH, "", null);
		} else {
			ArrayList<String> matches = new ArrayList<String>();
			matches.addAll(result.getLinearizations());
			returnOrForwardMatches(handler, matches);
		}
	}


	/**
	 * <p>Sets the intent result to NETWORK_ERROR.</p>
	 * 
	 * <p>Note that we are not showing the exception message to the user,
	 * as it has proved to be uninformative.</p>
	 * 
	 * @param handler message handler
	 * @param type identifier of the caller
	 * @param e exception
	 */
	private void setResultNetworkError(Handler handler, String type, IOException e) {
		handleResultError(handler, RecognizerIntent.RESULT_NETWORK_ERROR, type, e);
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
	private void returnOrForwardMatches(Handler handler, ArrayList<String> matches) {
		// Throw away matches that the user is not interested in
		if (mExtraMaxResults > 0 && matches.size() > mExtraMaxResults) {
			matches.subList(mExtraMaxResults, matches.size()).clear();
		}

		if (mExtraResultsPendingIntent == null) {
			setResultIntent(matches);
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
			String message = "";
			if (matches.size() == 1) {
				message = match;
			} else {
				message = matches.toString();
			}
			handler.sendMessage(createMessage(MSG_TOAST, String.format(getString(R.string.toastForwardedMatches), message)));
			try {
				// TODO: dummy number 1234
				mExtraResultsPendingIntent.send(this, 1234, intent);
			} catch (CanceledException e) {
				handler.sendMessage(createMessage(MSG_TOAST, e.getMessage()));
			}
		}
		finish();
	}


	private String makeUserAgentComment() {
		return 	"RecognizerIntentActivity/" + Utils.getVersionName(this) + "; " + mUniqueId + "; " + getCaller();
	}


	/**
	 * <p>Returns the package name of the app that receives the transcription,
	 * or <code>null</code> if the package name could not be resolved.</p>
	 */
	private String getCaller() {
		if (mExtraResultsPendingIntent == null) {
			ComponentName callingActivity = getCallingActivity();
			if (callingActivity != null) {
				return callingActivity.getPackageName();
			}
		} else {
			return mExtraResultsPendingIntent.getTargetPackage();
		}
		return null;
	}


	private void handleResultError(Handler handler, int resultCode, String type, Exception e) {
		String message = "";
		switch (resultCode) {
		case RecognizerIntent.RESULT_AUDIO_ERROR:
			message = getString(R.string.errorResultAudioError);
			break;
		case RecognizerIntent.RESULT_CLIENT_ERROR:
			message = getString(R.string.errorResultClientError);
			break;
		case RecognizerIntent.RESULT_NETWORK_ERROR:
			message = getString(R.string.errorResultNetworkError);
			break;
		case RecognizerIntent.RESULT_SERVER_ERROR:
			message = getString(R.string.errorResultServerError);
			break;
		case RecognizerIntent.RESULT_NO_MATCH:
			message = getString(R.string.errorResultNoMatch);
			break;
		default:
			// TODO: This should never happen
			message = getString(R.string.error);
		}
		if (e != null) {
			Log.e(LOG_TAG, "Exception: " + type + ": " + e.getMessage());
		}
		handler.sendMessage(createMessage(MSG_RESULT_ERROR, message));
	}


	private void setUrls(PackageNameRegistry wrapper) throws MalformedURLException {
		// The server URL should never be null
		mServerUrl = new URL(
				Utils.chooseValue(
						wrapper.getServerUrl(),
						mExtras.getString(Extras.EXTRA_SERVER_URL),
						mPrefs.getString(getString(R.string.keyService), getString(R.string.defaultService))
						));

		// If the user has not overridden the grammar then use the app's EXTRA.
		String urlAsString = Utils.chooseValue(wrapper.getGrammarUrl(), mExtras.getString(Extras.EXTRA_GRAMMAR_URL));
		if (urlAsString != null && urlAsString.length() > 0) {
			mGrammarUrl = new URL(urlAsString);
		}
	}


	// What follows is some test code and some unused code.

	// BUG: experimenting with 200
	// How to do it properly?
	private int scaleVolume(int volume) {
		if (volume <= 200) return 0;
		return (int) (MAX_BAR.length() * Math.log10((double) volume) / LOG_OF_MAX_VOLUME);
	}


	private String makeBar(int len) {
		if (len <= 0) return "";
		if (len >= MAX_BAR.length()) return MAX_BAR;
		return MAX_BAR.substring(0, len);
	}


	private boolean transcribeFile(String fileName, String contentType) {
		mBStartStop.setVisibility(View.GONE);
		try {
			byte[] bytes = getBytesFromAsset(fileName);
			Log.i(LOG_TAG, "Transcribing bytes: " + bytes.length);
			boolean success = createRecSession(contentType);
			if (success) {
				transcribeAndFinishInBackground(mMessageHandler, bytes);
				return true;
			}
		} catch (IOException e) {
			// Failed to get data from the asset
			handleResultError(mMessageHandler, RecognizerIntent.RESULT_CLIENT_ERROR, "file", e);
		}
		return false;
	}


	private byte[] getBytesFromAsset(String assetName) throws IOException {
		InputStream is = getAssets().open(assetName);
		//long length = getAssets().openFd(assetName).getLength();
		return IOUtils.toByteArray(is);
	}


	/*
	private void test_upload_from_res_raw() {
		InputStream ins = res.openRawResource(R.raw.test_12345);
		demoMatch = transcribe(ins, ins.available());
	}
	 */
}
