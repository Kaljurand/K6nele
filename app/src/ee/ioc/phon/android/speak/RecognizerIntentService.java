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
 * 
 * @author Kaarel Kaljurand
 */
public class RecognizerIntentService extends Service {

	private static final String LOG_TAG = RecognizerIntentService.class.getName();

	private final IBinder mBinder = new RecognizerBinder();

	// Send the chunk every 1.5 seconds
	private static final int TASK_INTERVAL_SEND = 1500;
	private static final int TASK_DELAY_SEND = TASK_INTERVAL_SEND;

	private volatile Looper mSendLooper;
	private volatile Handler mSendHandler;

	private Runnable mSendTask;

	private ChunkedWebRecSession mRecSession;

	private RawAudioRecorder mRecorder;

	private OnResultListener mOnResultListener;
	private OnErrorListener mOnErrorListener;

	private int mChunkCount = 0;

	private long mStartTime = 0;

	// TODO: reduce the number of states
	public enum State {
		// Service created
		CREATED,
		// Recognizer session created
		INITIALIZED,
		// Started the recording
		RECORDING,
		// Finished recording
		PROCESSING,
		// Got an error
		ERROR,
		// Resources released
		RELEASED;
	}

	private State mState = null;


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
		setState(State.CREATED);
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
		State currentState = getState();
		return currentState == State.RECORDING || currentState == State.PROCESSING;
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


	/**
	 * <p>Return the complete audio data from the beginning
	 * of the recording.</p>
	 */
	public byte[] getCurrentRecording() {
		if (mRecorder == null) {
			return new byte[0];

		}
		return mRecorder.getCurrentRecording();
	}


	public void init(String contentType, String userAgentComment, URL serverUrl, URL grammarUrl, String grammarTargetLang, int nbest) {
		try {
			createRecSession(contentType, userAgentComment, serverUrl, grammarUrl, grammarTargetLang, nbest);
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
			return;
		} catch (NotAvailableException e) {
			processError(RecognizerIntent.RESULT_SERVER_ERROR, e);
			return;
		}
	}


	public void start(int sampleRate) {
		try {
			startRecording(sampleRate);
			mStartTime = SystemClock.elapsedRealtime();
			startChunkSending(TASK_INTERVAL_SEND, TASK_DELAY_SEND, false);
			setState(State.RECORDING);
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_AUDIO_ERROR, e);
		}
	}


	public void finishRecording() {
		if (mRecorder == null) {
			processError(RecognizerIntent.RESULT_NETWORK_ERROR, null);
		} else {
			mRecorder.stop();
			mSendHandler.removeCallbacks(mSendTask);

			// TODO: improve this
			Thread t = new Thread() {
				public void run() {
					transcribe(mRecorder.consumeRecording());
				}
			};
			t.start();
		}
	}


	public int getChunkCount() {
		return mChunkCount;
	}


	/**
	 * <p>This can be called by clients who want to skip the recording
	 * steps, but who have existing audio data which they want to transcribe.</p>
	 */
	public void transcribe(byte[] bytes) {
		setState(State.PROCESSING);
		RecSessionResult result = null;
		try {
			sendChunk(bytes, true);
			result = getResult();
		} catch (IOException e) {
			processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
		}
		if (result == null || result.getLinearizations().isEmpty()) {
			processError(RecognizerIntent.RESULT_NO_MATCH, null);
		} else {
			processResult(result);
		}
	}


	/**
	 * <p>Starting chunk sending in a separate thread so that slow internet would not block the UI.</p>
	 *
	 * TODO
	 */
	private void startChunkSending(final int interval, int delay, final boolean consumeAll) {
		mChunkCount = 0;
		HandlerThread thread = new HandlerThread("SendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mSendLooper = thread.getLooper();
		mSendHandler = new Handler(mSendLooper);

		mSendTask = new Runnable() {
			public void run() {
				if (mRecorder != null) {
					try {
						sendChunk(mRecorder.consumeRecording(), consumeAll);
					} catch (IOException e) {
						processError(RecognizerIntent.RESULT_NETWORK_ERROR, e);
						return;
					}
					if (consumeAll == false) {
						mSendHandler.postDelayed(this, interval);
					}
				}
			}
		};
		mSendHandler.postDelayed(mSendTask, delay);
	}


	/**
	 * <p>Starts recording from the microphone with the given sample rate.</p>
	 *
	 * @throws IOException if recorder could not be created
	 */
	private void startRecording(int recordingRate) throws IOException {
		mRecorder = new RawAudioRecorder(recordingRate);
		if (mRecorder.getState() == RawAudioRecorder.State.ERROR) {
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


	/**
	 * <p>We kill the running processes in this order:
	 * chunk sending, recognizer session, audio recorder.</p>
	 * 
	 * <p>Note that mRecorder.release() can be called in any state.
	 * After that the recorder object is no longer available,
	 * so we should set it to <code>null</code>.</p>
	 */
	private void releaseResources() {
		if (mSendLooper != null) {
			// TODO: null sending message to a Handler on a dead thread
			mSendLooper.quit();
		}

		if (mSendHandler != null) {
			mSendHandler.removeCallbacks(mSendTask);
		}

		if (mRecSession != null && ! mRecSession.isFinished()) {
			mRecSession.cancel();
		}
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}
		setState(State.RELEASED);
	}


	/**
	 * <p>Tries to create a speech recognition session.</p>
	 *
	 * <p>Set the content-type to <code>null</code> if you want to use the default
	 * net-speech-api content type (raw audio).</p>
	 * 
	 * @param contentType content type of the audio (e.g. "audio/x-flac;rate=16000")
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
		setState(State.INITIALIZED);
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
		Log.i(LOG_TAG, "State changed to: " + state);
		mState = state;
	}


	// TODO: not sure what stopSelf does
	private void processResult(RecSessionResult result) {
		mOnResultListener.onResult(result);
		releaseResources();
		stopSelf();
	}


	private void processError(int errorCode, Exception e) {
		setState(State.ERROR);
		mOnErrorListener.onError(errorCode, e);
		releaseResources();
		stopSelf();
	}
}