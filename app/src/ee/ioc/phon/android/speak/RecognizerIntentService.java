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

import android.app.Service;

import android.content.Intent;

import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import android.util.Log;

import java.io.IOException;
import java.net.URL;

import ee.ioc.phon.netspeechapi.recsession.ChunkedWebRecSession;
import ee.ioc.phon.netspeechapi.recsession.NotAvailableException;
import ee.ioc.phon.netspeechapi.recsession.RecSessionResult;


/**
 * TODO: kill itself as soon as there is an error
 * 
 * @author Kaarel Kaljurand
 */
public class RecognizerIntentService extends Service {

	private static final String LOG_TAG = RecognizerIntentService.class.getName();

	private final IBinder mBinder = new RecognizerBinder();

	// Send the chunk every 2 seconds
	private static final int TASK_INTERVAL_SEND = 2000;
	private static final int TASK_DELAY_SEND = TASK_INTERVAL_SEND;

	private volatile Looper mSendLooper;
	private volatile Handler mSendHandler;

	private Runnable mSendTask;

	private ChunkedWebRecSession mRecSession;

	private RawAudioRecorder mRecorder;

	private OnResultListener mOnResultListener;
	private OnErrorListener mOnErrorListener;

	private int mChunkCount = 0;
	private int mSampleRate = 16000;

	private long mStartTime = 0;

	public enum State {
		INIT,
		RECORDING,
		PROCESSING;
	}

	private State mState = State.INIT;


	public class RecognizerBinder extends Binder {
		public RecognizerIntentService getService() {
			return RecognizerIntentService.this;
		}
	}


	public interface OnResultListener {
		public boolean onResult(RecSessionResult result);
	}


	public interface OnErrorListener {
		public boolean onError(int errorCode, Exception e);
	}


	@Override
	public IBinder onBind(Intent intent) {
		Log.i(LOG_TAG, "onBind");
		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "onCreate");
	}


	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		releaseResources();
	}


	public void setOnResultListener(OnResultListener onResultListener) {
		mOnResultListener = onResultListener;
	}


	public void setOnErrorListener(OnErrorListener onErrorListener) {
		mOnErrorListener = onErrorListener;
	}


	public State getState() {
		return mState;
	}


	public long getStartTime() {
		return mStartTime;
	}


	public boolean isWorking() {
		return getState() != State.INIT;
	}


	public byte[] consumeRecording() {
		if (mRecorder == null) {
			return new byte[0];
		}
		return mRecorder.consumeRecording();
	}


	public int getLength() {
		if (mRecorder == null) {
			return 0;
		}
		return mRecorder.getLength();
	}


	public boolean isPausing() {
		return mRecorder != null && mRecorder.isPausing();
	}


	public byte[] getCurrentRecording() {
		if (mRecorder == null) {
			return new byte[0];

		}
		return mRecorder.getCurrentRecording();
	}


	/**
	 * 1. Beep
	 * 2. Wait until the beep has stopped
	 * 3. Set up the recorder and start recording (finish if failed)
	 * 4. Create the HTTP-connection to the recognition server
	 * 5. Update the GUI to show that the recording is in progress
	 */
	public void init(int sampleRate, String userAgentComment, URL serverUrl, URL grammarUrl, String grammarTargetLang, int nbest) {
		try {
			createRecSession(Utils.getContentType(sampleRate), userAgentComment, serverUrl, grammarUrl, grammarTargetLang, nbest);
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
			return;
		} catch (NotAvailableException e) {
			processError(RecognizerIntent.RESULT_SERVER_ERROR, e);
			return;
		}
		mSampleRate = sampleRate;
	}


	public void start() {
		try {
			startRecording(mSampleRate);
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_AUDIO_ERROR, e);
			return;
		}

		mStartTime = SystemClock.elapsedRealtime();
		setState(State.RECORDING);

		// Starting chunk sending in a separate thread so that slow internet
		// would not block the UI.
		HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mSendLooper = thread.getLooper();
		mSendHandler = new Handler(mSendLooper);

		mSendTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						sendChunk(mRecorder.consumeRecording(), false);
					} catch (IOException e) {
						processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
						return;
					}
					mSendHandler.postDelayed(this, TASK_INTERVAL_SEND);
				}
			}
		};
		mSendHandler.postDelayed(mSendTask, TASK_DELAY_SEND);
	}


	public void finishRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
		}
		setState(State.PROCESSING);
	}


	public void startTranscribing() {
		transcribe(consumeRecording());
	}


	public int getChunkCount() {
		return mChunkCount;
	}


	public void transcribe(byte[] bytes) {
		try {
			sendChunk(bytes, true);
			mOnResultListener.onResult(getResult());
			stopSelf();
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
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
			releaseResources();
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		mRecorder.prepare();

		if (mRecorder.getState() != RawAudioRecorder.State.READY) {
			releaseResources();
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}

		mRecorder.start();

		if (mRecorder.getState() != RawAudioRecorder.State.RECORDING) {
			releaseResources();
			throw new IOException(getString(R.string.errorCantCreateRecorder));
		}
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
		if (mSendHandler != null) {
			mSendHandler.removeCallbacks(mSendTask);
		}

		if (mSendLooper != null) {
			mSendLooper.quit();
		}

		if (mRecSession != null && ! mRecSession.isFinished()) {
			mRecSession.cancel();
		}
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
		setState(State.INIT);
	}


	/**
	 * <p>Tries to create a speech recognition session and returns <code>true</code>
	 * if succeeds. Otherwise sends out an error message and returns <code>false</code>.
	 * Set the content-type to <code>null</code> if you want to use the default
	 * net-speech-api content type (raw audio).</p>
	 * 
	 * @param contentType content type of the audio (e.g. "audio/x-flac;rate=16000")
	 * @return <code>true</code> iff success
	 * @throws NotAvailableException 
	 * @throws IOException 
	 */
	private void createRecSession(String contentType, String userAgentComment,
			URL serverUrl, URL grammarUrl, String grammarTargetLang, int nbest) throws IOException, NotAvailableException {
		mRecSession = new ChunkedWebRecSession(serverUrl, grammarUrl, grammarTargetLang, nbest);
		Log.i(LOG_TAG, "Created ChunkedWebRecSession: " + serverUrl + ": lm=" + grammarUrl + ": lang=" + grammarTargetLang + ": nbest=" + nbest);
		mRecSession.setUserAgentComment(userAgentComment);
		if (contentType != null) {
			mRecSession.setContentType(contentType);
		}
		mRecSession.create();
		Log.i(LOG_TAG, "Created recognition session: " + contentType);
	}


	private RecSessionResult getResult() throws IOException {
		if (mRecSession == null) {
			return null;
		}
		return mRecSession.getResult();
	}


	/**
	 * 
	 * @param handler message handler
	 * @param bytes byte array representing the audio data
	 * @param isLast indicates that this is the last chunk that is sent
	 * @throws IOException 
	 */
	private void sendChunk(byte[] bytes, boolean isLast) throws IOException {
		if (mRecSession != null && bytes != null && bytes.length > 0 && ! mRecSession.isFinished()) {
			mRecSession.sendChunk(bytes, isLast);
			mChunkCount++;
			Log.i(LOG_TAG, "Send chunk: " + bytes.length);
		}
	}


	private void setState(State state) {
		mState = state;
	}


	private void processError(int errorCode, Exception e) {
		mOnErrorListener.onError(errorCode, e);
		stopSelf();
	}
}