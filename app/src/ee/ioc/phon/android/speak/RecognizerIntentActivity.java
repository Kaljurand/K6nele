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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.PendingIntent.CanceledException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;

import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.SparseArray;

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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import ee.ioc.phon.android.speak.RecognizerIntentService.RecognizerBinder;
import ee.ioc.phon.android.speak.RecognizerIntentService.State;
import ee.ioc.phon.android.speak.provider.FileContentProvider;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;


/**
 * <p>This activity responds to the following intent types:</p>
 * <ul>
 * <li>android.speech.action.RECOGNIZE_SPEECH</li>
 * <li>android.speech.action.WEB_SEARCH</li>
 * </ul>
 * <p>We have tried to implement the complete interface of RecognizerIntent as of API level 7 (v2.1).</p>
 * 
 * <p>It records audio, transcribes it using a speech-to-text server
 * and returns the result as a non-empty list of Strings.
 * In case of <code>android.intent.action.MAIN</code>,
 * it submits the recorded/transcribed audio to a web search.
 * It never returns an error code,
 * all the errors are processed within this activity.</p>
 * 
 * <p>This activity rewrites the error codes which originally come from the
 * speech recognizer webservice (and which are then rewritten by the net-speech-api)
 * to the RecognizerIntent result error codes. The RecognizerIntent error codes are the
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

	private static final int DELAY_AFTER_START_BEEP = 200;

	private static final String MSG = "MSG";
	private static final int MSG_TOAST = 1;
	private static final int MSG_RESULT_ERROR = 2;

	private static final String DOTS = "............";

	private SparseArray<String> mErrorMessages;

	private SharedPreferences mPrefs;

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

	private SimpleMessageHandler mMessageHandler;
	private Handler mHandlerBytes = new Handler();
	private Handler mHandlerStop = new Handler();
	private Handler mHandlerVolume = new Handler();
	private Handler mHandlerChunks = new Handler();

	private Runnable mRunnableBytes;
	private Runnable mRunnableStop;
	private Runnable mRunnableVolume;
	private Runnable mRunnableChunks;

	private ChunkedWebRecSessionBuilder mRecSessionBuilder;

	private Resources mRes;
	private MediaPlayer mMediaPlayer;

	private PendingIntent mExtraResultsPendingIntent;

	private Bundle mExtras;

	private RecognizerIntentService mService;
	private boolean mIsBound = false;
	private boolean mStartRecording = false;
	private int mLevel = 0;

	// Note: only used with pre-Honeycomb
	private boolean mIsStartActivity = false;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.i(LOG_TAG, "Service connected");
			mService = ((RecognizerBinder) service).getService();

			mService.setOnResultListener(new RecognizerIntentService.OnResultListener() {
				public boolean onResult(RecSessionResult result) {
					// We trust that getLinearizations() returns a non-null non-empty list.
					ArrayList<String> matches = new ArrayList<String>();
					matches.addAll(result.getLinearizations());
					returnOrForwardMatches(mMessageHandler, matches);
					return true;
				}
			});

			mService.setOnErrorListener(new RecognizerIntentService.OnErrorListener() {
				public boolean onError(int errorCode, Exception e) {
					handleResultError(mMessageHandler, errorCode, "onError", e);
					return true;
				}
			});


			if (mStartRecording && ! mService.isWorking()) {
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
			Log.i(LOG_TAG, "Service disconnected");
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.recognizer);

		mMessageHandler = new SimpleMessageHandler(this);
		mErrorMessages = createErrorMessages();

		// Don't shut down the screen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
		mVolumeLevels = new ArrayList<Drawable>();
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level0));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level1));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level2));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level3));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level4));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level5));
		mVolumeLevels.add(mRes.getDrawable(R.drawable.speak_now_level6));

		mExtras = getIntent().getExtras();
		if (mExtras == null) {
			// For some reason getExtras() can return null, we map it
			// to an empty Bundle if this occurs.
			mExtras = new Bundle();
		} else {
			mExtraResultsPendingIntent = Utils.getPendingIntent(mExtras);
		}

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		// For the change in the autostart-setting to take effect,
		// the user must restart the app. This seems more natural.
		mStartRecording = mPrefs.getBoolean("keyAutoStart", false);

		try {
			mRecSessionBuilder = new ChunkedWebRecSessionBuilder(this, mExtras, getCallingActivity());
		} catch (MalformedURLException e) {
			// The user has managed to store a malformed URL in the configuration.
			handleResultError(mMessageHandler, RecognizerIntent.RESULT_CLIENT_ERROR, "", e);
		}
	}


	@Override
	public void onStart() {
		super.onStart();

		mIsStartActivity = false;

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
		final int maxRecordingTime = 1000 * Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyAutoStopAfterTime),
						getString(R.string.defaultAutoStopAfterTime)));

		mRunnableStop = new Runnable() {
			public void run() {
				if (mService != null) {
					if (maxRecordingTime < (SystemClock.elapsedRealtime() - mService.getStartTime())) {
						Log.i(LOG_TAG, "Max recording time exceeded");
						stopRecording();
					} else if (mPrefs.getBoolean("keyAutoStopAfterPause", true) && mService.isPausing()) {
						Log.i(LOG_TAG, "Speaker finished speaking");
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
					// TODO: take these from some configuration
					float min = 15.f;
					float max = 30.f;

					float db = mService.getRmsdb();
					final int maxLevel = mVolumeLevels.size() - 1;

					int index = (int) ((db - min) / (max - min) * maxLevel);
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

		// Settings button
		((Button) findViewById(R.id.bSettings)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mIsStartActivity = true;
				startActivity(new Intent(getApplicationContext(), Preferences.class));
			}
		});

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
		// Note: on pre-honeycomb HOME does not stop the service, as there does not seem
		// to be a nice way to detect configuration change in onStop().
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (! isChangingConfigurations()) {
				stopService(new Intent(this, RecognizerIntentService.class));
			}
		} else if (mIsStartActivity || isFinishing()) {
			stopService(new Intent(this, RecognizerIntentService.class));
		}

		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
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
			details.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, getDetails());
			startActivity(details);
			return true;
		case R.id.menuRecognizerTest1:
			transcribeFile("test_kaks_minutit_sekundites.flac", "audio/x-flac;rate=16000");
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


	void doBindService() {
		// This can be called also on an already running service
		startService(new Intent(this, RecognizerIntentService.class));

		bindService(new Intent(this, RecognizerIntentService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.i(LOG_TAG, "Service is bound");
	}


	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
			mService = null;
			Log.i(LOG_TAG, "Service is UNBOUND");
		}
	}


	private void setGui() {
		if (mService == null) {
			// in onResume() the service might not be ready yet
			return;
		}
		switch(mService.getState()) {
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
		setTvPrompt();
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
		setTvPrompt();
		mBStartStop.setText(getString(R.string.buttonSpeak));
		mBStartStop.setVisibility(View.VISIBLE);
		mLlError.setVisibility(View.VISIBLE);
		mTvErrorMessage.setText(mErrorMessages.get(errorCode));
	}


	private void setGuiRecording() {
		mChronometer.setBase(mService.getStartTime());
		startChronometer();
		startAllTasks();
		setTvPrompt();
		mLlProgress.setVisibility(View.VISIBLE);
		mLlError.setVisibility(View.GONE);
		setRecorderStyle(mRes.getColor(R.color.red));
		if (mPrefs.getBoolean("keyAutoStopAfterPause", true)) {
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


	private void setTvPrompt() {
		String prompt = getPrompt();
		if (prompt == null || prompt.length() == 0) {
			mTvPrompt.setVisibility(View.INVISIBLE);
		} else {
			mTvPrompt.setText(prompt);
			mTvPrompt.setVisibility(View.VISIBLE);
		}
	}

    private String getPrompt() {
        String prompt = mExtras.getString(RecognizerIntent.EXTRA_PROMPT);
        if (prompt == null && mExtraResultsPendingIntent == null && getCallingActivity() == null) {
            return getString(R.string.promptSearch);
        }
        return prompt;
    }


	private void stopChronometer() {
		mChronometer.stop();
	}


	private void startChronometer() {
		mChronometer.start();
	}


	private void startRecording() {
		int sampleRate = Integer.parseInt(
				mPrefs.getString(
						getString(R.string.keyRecordingRate),
						getString(R.string.defaultRecordingRate)));
		mRecSessionBuilder.setContentType(sampleRate);
		if (mService.init(mRecSessionBuilder.build())) {
			playStartSound();
			mService.start(sampleRate);
			setGui();
		}
	}


	/**
	 * Sets the RESULT_OK intent. Adds the recorded audio data if the caller has requested it
	 * and the requested format is supported or unset.
	 */
	private void setResultIntent(final Handler handler, ArrayList<String> matches) {
		Intent intent = new Intent();
		if (mExtras.getBoolean(Extras.GET_AUDIO)) {
			String audioFormat = mExtras.getString(Extras.GET_AUDIO_FORMAT);
			if (audioFormat == null) {
				audioFormat = Constants.DEFAULT_AUDIO_FORMAT;
			}
			if (Constants.SUPPORTED_AUDIO_FORMATS.contains(audioFormat)) {
				try {
					FileOutputStream fos = openFileOutput(Constants.AUDIO_FILENAME, Context.MODE_PRIVATE);
					fos.write(mService.getCompleteRecordingAsWav());
					fos.close();

					Uri uri = Uri.parse("content://" + FileContentProvider.AUTHORITY + "/" + Constants.AUDIO_FILENAME);
					// TODO: not sure about the type (or if it's needed)
					intent.setDataAndType(uri, audioFormat);
				} catch (FileNotFoundException e) {
					Log.e(LOG_TAG, "FileNotFoundException: " + e.getMessage());
				} catch (IOException e) {
					Log.e(LOG_TAG, "IOException: " + e.getMessage());
				}
			} else {
				if (Log.DEBUG) {
					handler.sendMessage(createMessage(MSG_TOAST,
							String.format(getString(R.string.toastRequestedAudioFormatNotSupported), audioFormat)));
				}
			}
		}
		intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, matches);
		setResult(Activity.RESULT_OK, intent);
	}


	private void toast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}


    // TODO: Use AudioCue
	private void playStartSound() {
		boolean soundPlayed = playSound(R.raw.explore_begin);
		if (soundPlayed) {
			SystemClock.sleep(DELAY_AFTER_START_BEEP);
		}
	}


	private void playStopSound() {
		playSound(R.raw.explore_end);
	}


	private void playErrorSound() {
		playSound(R.raw.error);
	}


	private boolean playSound(int sound) {
        if (mPrefs.getBoolean(mRes.getString(R.string.keyAudioCues),
                mRes.getBoolean(R.bool.defaultAudioCues))) {
			mMediaPlayer = MediaPlayer.create(this, sound);
			mMediaPlayer.start();
			return true;
		}
		return false;
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
		List<String> info = new ArrayList<String>();
		info.add("ID: " + Utils.getUniqueId(PreferenceManager.getDefaultSharedPreferences(this)));
		info.add("User-Agent comment: " + mRecSessionBuilder.getUserAgentComment());
		info.add("Calling activity class name: " + callingActivityClassName);
		info.add("Calling activity package name: " + callingActivityPackageName);
		info.add("Pending intent target package: " + pendingIntentTargetPackage);
		info.add("Selected grammar: " + mRecSessionBuilder.getGrammarUrl());
		info.add("Selected target lang: " + mRecSessionBuilder.getGrammarTargetLang());
		info.add("Selected server: " + mRecSessionBuilder.getServerUrl());
		info.add("Intent action: " + getIntent().getAction());
		info.addAll(Utils.ppBundle(mExtras));
		return info.toArray(new String[info.size()]);
	}


	private static Message createMessage(int type, String str) {
		Bundle b = new Bundle();
		b.putString(MSG, str);
		Message msg = Message.obtain();
		msg.what = type;
		msg.setData(b);
		return msg;
	}


	private static class SimpleMessageHandler extends Handler {
		private final WeakReference<RecognizerIntentActivity> mRef;

		public SimpleMessageHandler(RecognizerIntentActivity c) {
			mRef = new WeakReference<RecognizerIntentActivity>(c);
		}

		public void handleMessage(Message msg) {
			RecognizerIntentActivity outerClass = mRef.get();
			if (outerClass != null) {
				Bundle b = msg.getData();
				String msgAsString = b.getString(MSG);
				switch (msg.what) {
				case MSG_TOAST:
					outerClass.toast(msgAsString);
					break;
				case MSG_RESULT_ERROR:
					outerClass.playErrorSound();
					outerClass.stopAllTasks();
					outerClass.setGuiError();
					break;
				}
			}
		}
	}


	/**
	 * <p>Returns the transcription results (matches) to the caller,
	 * or sends them to the pending intent, or performs a web search.</p>
	 *
	 * <p>If a pending intent was specified then use it. This is the case with
	 * applications that use the standard search bar (e.g. Google Maps and YouTube).</p>
	 *
	 * <p>Otherwise. If there was no caller (i.e. we cannot return the results), or
	 * the caller asked us explicitly to perform "web search", then do that, possibly
	 * disambiguating the results or redoing the recognition.
	 * This is the case when K6nele was launched from its launcher icon (i.e. no caller),
	 * or from a browser app.
	 * (Note that trying to return the results to Google Chrome does not seem to work.)</p>
	 *
	 * <p>Otherwise. Just return the results to the caller.</p>
	 *
	 * <p>Note that we assume that the given list of matches contains at least one
	 * element.</p>
	 *
	 * @param handler message handler
	 * @param matches transcription results (one or more hypotheses)
	 */
	private void returnOrForwardMatches(final Handler handler, ArrayList<String> matches) {
		// Throw away matches that the user is not interested in
		int maxResults = mExtras.getInt(RecognizerIntent.EXTRA_MAX_RESULTS);
		if (maxResults > 0 && matches.size() > maxResults) {
			matches.subList(maxResults, matches.size()).clear();
		}

		if (mExtraResultsPendingIntent == null) {
			if (getCallingActivity() == null
					|| RecognizerIntent.ACTION_WEB_SEARCH.equals(getIntent().getAction())
					|| mExtras.getBoolean(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY)) {
				handleResultsByWebSearch(this, handler, matches);
				return;
			} else {
				setResultIntent(handler, matches);
			}
		} else {
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
			String message = "";
			if (matches.size() == 1) {
				message = match;
			} else {
				message = matches.toString();
			}
			// Display a toast with the transcription.
			handler.sendMessage(createMessage(MSG_TOAST, String.format(getString(R.string.toastForwardedMatches), message)));
			try {
				mExtraResultsPendingIntent.send(this, Activity.RESULT_OK, intent);
			} catch (CanceledException e) {
				handler.sendMessage(createMessage(MSG_TOAST, e.getMessage()));
			}
		}
		finish();
	}


	// In case of multiple hypotheses, ask the user to select from a list dialog.
	// TODO: fetch also confidence scores and treat a very confident hypothesis
	// as a single hypothesis.
	private void handleResultsByWebSearch(final Context context, final Handler handler, final ArrayList<String> results) {
		// Some tweaking to cleanup the UI that would show under the
		// dialog window that we are about to open.
		runOnUiThread(new Runnable() {
			public void run() {
				mLlTranscribing.setVisibility(View.GONE);
			}
		});

		Intent searchIntent;
		if (results.size() == 1) {
			searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
			searchIntent.putExtra(SearchManager.QUERY, results.get(0));
		} else {
			// TODO: it would be a bit cleaner to pass ACTION_WEB_SEARCH
			// via a pending intent
			searchIntent = new Intent(this, DetailsActivity.class);
			searchIntent.putExtra(DetailsActivity.EXTRA_TITLE, getString(R.string.dialogTitleHypotheses));
			searchIntent.putExtra(DetailsActivity.EXTRA_STRING_ARRAY, results.toArray(new String[results.size()]));
		}
		startActivity(searchIntent);
	}


	private void handleResultError(Handler handler, int resultCode, String type, Exception e) {
		if (e != null) {
			Log.e(LOG_TAG, "Exception: " + type + ": " + e.getMessage());
		}
		handler.sendMessage(createMessage(MSG_RESULT_ERROR, mErrorMessages.get(resultCode)));
	}


	private static String makeBar(String bar, int len) {
		if (len <= 0) return "";
		if (len >= bar.length()) return Integer.toString(len);
		return bar.substring(0, len);
	}


	private void transcribeFile(String fileName, String contentType) {
		try {
			byte[] bytes = getBytesFromAsset(fileName);
			Log.i(LOG_TAG, "Transcribing bytes: " + bytes.length);
			mRecSessionBuilder.setContentType(contentType);
			if (mService.init(mRecSessionBuilder.build())) {
				mService.transcribe(bytes);
				setGui();
			}
		} catch (IOException e) {
			// Failed to get data from the asset
			handleResultError(mMessageHandler, RecognizerIntent.RESULT_CLIENT_ERROR, "file", e);
		}
	}


	private byte[] getBytesFromAsset(String assetName) throws IOException {
		InputStream is = getAssets().open(assetName);
		//long length = getAssets().openFd(assetName).getLength();
		return IOUtils.toByteArray(is);
	}


	private SparseArray<String> createErrorMessages() {
		SparseArray<String> errorMessages = new SparseArray<String>();
		errorMessages.put(RecognizerIntent.RESULT_AUDIO_ERROR, getString(R.string.errorResultAudioError));
		errorMessages.put(RecognizerIntent.RESULT_CLIENT_ERROR, getString(R.string.errorResultClientError));
		errorMessages.put(RecognizerIntent.RESULT_NETWORK_ERROR, getString(R.string.errorResultNetworkError));
		errorMessages.put(RecognizerIntent.RESULT_SERVER_ERROR, getString(R.string.errorResultServerError));
		errorMessages.put(RecognizerIntent.RESULT_NO_MATCH, getString(R.string.errorResultNoMatch));
		return errorMessages;
	}


	/*
	private void test_upload_from_res_raw() {
		InputStream ins = res.openRawResource(R.raw.test_12345);
		demoMatch = transcribe(ins, ins.available());
	}
	 */
}
