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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * <p>Records raw audio using AudioRecord and stores it into a byte array as</p>
 * <ul>
 * <li>signed</li>
 * <li>16-bit</li>
 * <li>little endian</li>
 * <li>mono</li>
 * <li>16kHz</li>
 * </ul>
 * 
 * <p>For example, the corresponding <code>arecord</code> settings are</p>
 * 
 * <pre>
 * arecord --file-type raw --format=S16_LE --channels 1 --rate 16000
 * </pre>
 * 
 * TODO: use: ByteArrayOutputStream (instead of copying all the time)
 * 
 * @author Kaarel Kaljurand
 */
public class RawAudioRecorder {

	// little endian = 1234
	// big endian = 4321
	//private static final String CONTENT_TYPE = "audio/x-raw-int,rate=16000,channels=1,signed=true,endianness=1234,depth=16,width=16";

	// Setting this to "true" is not yet compatible with pause detection.
	private static final boolean TRUNCATE_UPON_CONSUME = false;

	private static final String LOG_TAG = RawAudioRecorder.class.getName();

	private static final int RESOLUTION = AudioFormat.ENCODING_PCM_16BIT;
	private static final short RESOLUTION_IN_BITS = 16;

	// Number of channels (MONO = 1, STEREO = 2)
	private static final short CHANNELS = 1;

	public enum State {
		// recorder is initializing
		INITIALIZING,

		// recorder has been initialized, recorder not yet started
		READY,

		// recording
		RECORDING,

		// reconstruction needed
		ERROR,

		// reset needed
		STOPPED
	};


	// The interval in which the recorded samples are output to the file
	private static final int TIMER_INTERVAL = 120;

	// The size in bytes of 1 seconds of 16kHz 16-bit mono audio.
	// This assumes that we always record with these settings.
	// TODO: calculate this dynamically
	private static final int ONE_SEC = 32000; // (RESOLUTION_IN_BITS / 8) * CHANNELS * 16000

	private AudioRecord mRecorder = null;

	private double mAvgEnergy = 0;

	// Recorder state
	private State mState;

	// Buffer size
	private int mBufferSize;

	// Number of frames written to file on each output
	private int	mFramePeriod;

	// The complete recording
	private byte[] mCurrentRecording = new byte[0];

	// Used only in TRUNCATE-mode, otherwise = 0. Indicates the total length of the recording
	// after the recording was last consumed. Only used in getLength().
	private int mLength = 0;

	// The position up to which the recording has been consumed if not in TRUNCATE-mode.
	// Ignored in the TRUNCATE-mode.
	private int mEndPos = 0;

	// Buffer for output
	private byte[] mBuffer;


	private AudioRecord.OnRecordPositionUpdateListener mListener = new AudioRecord.OnRecordPositionUpdateListener() {
		public void onPeriodicNotification(AudioRecord recorder) {
			// public int read (byte[] audioData, int offsetInBytes, int sizeInBytes)
			int numberOfBytes = recorder.read(mBuffer, 0, mBuffer.length); // Fill buffer

			// Some error checking
			if (numberOfBytes == AudioRecord.ERROR_INVALID_OPERATION) {
				Log.e(LOG_TAG, "The AudioRecord object was not properly initialized");
				stop();
			} else if (numberOfBytes == AudioRecord.ERROR_BAD_VALUE) {
				Log.e(LOG_TAG, "The parameters do not resolve to valid data and indexes.");
				stop();
			} else if (numberOfBytes > mBuffer.length) {
				Log.e(LOG_TAG, "Read more bytes than is buffer length:" + numberOfBytes + ": " + mBuffer.length);
				stop();
			} else {
				// Everything seems to be OK, writing the buffer into the file.
				mCurrentRecording = concat(mCurrentRecording, mBuffer);
			}
		}

		public void onMarkerReached(AudioRecord recorder) {
			// BUG: NOT USED
		}
	};


	/** 
	 * <p>Instantiates a new recorder and sets the state to INITIALIZING.
	 * In case of errors, no exception is thrown, but the state is set to ERROR.</p>
	 * 
	 * TODO: we currently assume that the sample rate is always 16 kHz, support
	 * other sample rates as well
	 *
	 * @param audioSource Identifier of the audio source (e.g. microphone)
	 * @param sampleRate Sample rate (e.g. 16kHz)
	 */
	public RawAudioRecorder(int audioSource, int sampleRate) {
		try {
			mFramePeriod = sampleRate * TIMER_INTERVAL / 1000;
			mBufferSize = mFramePeriod * 2 * RESOLUTION_IN_BITS * CHANNELS / 8;

			// Check to make sure buffer size is not smaller than the smallest allowed one
			if (mBufferSize < AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, RESOLUTION)) {
				mBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, RESOLUTION);
				// Set frame period and timer interval accordingly
				mFramePeriod = mBufferSize / ( 2 * RESOLUTION_IN_BITS * CHANNELS / 8 );
				Log.w(LOG_TAG, "Increasing buffer size to " + Integer.toString(mBufferSize));
			}

			mRecorder = new AudioRecord(audioSource, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, RESOLUTION, mBufferSize);
			if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
				throw new Exception("AudioRecord initialization failed");
			}
			mRecorder.setRecordPositionUpdateListener(mListener);
			mRecorder.setPositionNotificationPeriod(mFramePeriod);

			mState = State.INITIALIZING;
		} catch (Exception e) {
			if (e.getMessage() == null) {
				Log.e(LOG_TAG, "Unknown error occured while initializing recording");
			} else {
				Log.e(LOG_TAG, e.getMessage());
			}
			mState = State.ERROR;
		}
	}


	public RawAudioRecorder() {
		this(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000);
	}


	/**
	 * <p>Returns the state of the recorder in a RawAudioRecord.State typed object.
	 * Useful, as no exceptions are thrown.</p>
	 *
	 * @return recorder state
	 */
	public State getState() {
		return mState;
	}


	public byte[] getCurrentRecording() {
		return mCurrentRecording;
	}


	public byte[] consumeRecording() {
		// TODO: read about synchronized
		synchronized (mCurrentRecording) {
			int totalLength = mCurrentRecording.length;
			if (TRUNCATE_UPON_CONSUME) {
				byte[] bytes = new byte[(int) totalLength];
				System.arraycopy(mCurrentRecording, 0, bytes, 0, totalLength);
				Log.e(LOG_TAG, "Copied from: 0" + ": " + totalLength + " bytes");
				mCurrentRecording = new byte[0];
				mLength += totalLength;
				return bytes;
			} else {
				int len = totalLength - mEndPos;
				byte[] bytes = new byte[(int) len];
				System.arraycopy(mCurrentRecording, mEndPos, bytes, 0, len);
				Log.e(LOG_TAG, "Copied from: " + mEndPos + ": " + len + " bytes");
				mEndPos = totalLength;
				return bytes;
			}
		}
	}


	public int getLength() {
		return mLength + mCurrentRecording.length;
	}


	/**
	 * @return <code>true</code> iff a speech-ending pause has occurred at the end of the recorded data
	 */
	public boolean isPausing() {
		double pauseScore = getPauseScore();
		Log.e(LOG_TAG, "Pause score: " + pauseScore);
		return pauseScore > 7;
	}


	public float getRmsdb() {
		long sumOfSquares = getRms(mCurrentRecording.length, ONE_SEC);
		long samplesLength = ONE_SEC/2;
		if (sumOfSquares == 0 || samplesLength == 0) {
			return 0;
		}
		return getDb(sumOfSquares, samplesLength);
	}


	/**
	 * <p>In order to calculate if the user has stopped speaking we take the
	 * data from the last second of the recording, map it to a number
	 * and compare this number to the numbers obtained previously. We
	 * return a confidence score (0-INF) of a longer pause having occurred in the
	 * speech input.</p>
	 * 
	 * <p>TODO: base the implementation on some well-known technique.</p>
	 * 
	 * @return positive value which the caller can use to determine if there is a pause
	 */
	private double getPauseScore() {
		long t2 = getRms(mCurrentRecording.length, ONE_SEC);
		if (t2 == 0) {
			return 0;
		}
		double t = mAvgEnergy / t2;
		mAvgEnergy = (2 * mAvgEnergy + t2) / 3;
		return t;
	}


	/**
	 * TODO: there is no need for this method we could merge it with the constructor
	 * 
	 * <p>Prepares the recorder for recording and sets the state to READY.
	 * In case the recorder is not in the INITIALIZING state
	 * the recorder is set to the ERROR state, which makes a reconstruction necessary.
	 * In case of an exception, the state is changed to ERROR.</p>
	 */
	public void prepare() {
		try {
			if (mState == State.INITIALIZING) {
				if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
					mBuffer = new byte[mFramePeriod * (RESOLUTION_IN_BITS/8) * CHANNELS];
					mState = State.READY;
				} else {
					Log.e(LOG_TAG, "prepare() method called on uninitialized recorder");
					mState = State.ERROR;
				}
			} else {
				Log.e(LOG_TAG, "prepare() method called on illegal state");
				release();
				mState = State.ERROR;
			}
		} catch(Exception e) {
			if (e.getMessage() == null) {
				Log.e(LOG_TAG, "Unknown error occured in prepare()");
			} else {
				Log.e(LOG_TAG, e.getMessage());
			}
			mState = State.ERROR;
		}
	}


	/**
	 * <p>Releases the resources associated with this class, and removes the unnecessary files, when necessary.</p>
	 */
	public void release() {
		if (mState == State.RECORDING) {
			stop();
		}

		if (mRecorder != null) {
			mRecorder.release();
		}
	}


	/**
	 * <p>Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().</p>
	 */
	public void start() {
		if (mState == State.READY) {
			//mPayloadSize = 0;
			mRecorder.startRecording();
			Log.e(LOG_TAG, "startRecording()");
			mRecorder.read(mBuffer, 0, mBuffer.length);
			mState = State.RECORDING;
		} else {
			Log.e(LOG_TAG, "start() called on illegal state");
			mState = State.ERROR;
		}
	}


	/**
	 * <p>Stops the recording, and sets the state to STOPPED.
	 * Also finalizes the wave file.</p>
	 */
	public void stop() {
		if (mState == State.RECORDING) {
			Log.e(LOG_TAG, "Stopping the recorder...");
			// TODO: not sure if we need to set the listener to null
			mRecorder.setRecordPositionUpdateListener(null);
			mRecorder.stop();
			mState = State.STOPPED;
		} else {
			Log.e(LOG_TAG, "stop() called in illegal state: " + mState);
			mState = State.ERROR;
		}
	}


	/**
	 * TODO: how does the performance of this method behave if arrays get large
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	private static byte[] concat(byte[] A, byte[] B) {
		long size = A.length+B.length;
		if (size < Integer.MAX_VALUE) {
			byte[] C = new byte[(int) size];
			System.arraycopy(A, 0, C, 0, A.length);
			System.arraycopy(B, 0, C, A.length, B.length);
			return C;
		} else {
			return A;
		}
	}


	private long getRms(int end, int span) {
		int begin = end - span;
		if (begin < 0) {
			begin = 0;
		}
		// make sure begin is even
		if (0 != (begin % 2)) {
			begin++;
		}

		long sum = 0;
		for (int i = begin; i < end; i+=2) {
			// TODO: We don't need the whole short, just take the 2nd byte (the more significant one)
			// byte curSample = mCurrentRecording[i+1];

			short curSample = getShort(mCurrentRecording[i], mCurrentRecording[i+1]);
			if (curSample > 0) {
				sum += curSample * curSample;
			}
		}
		return sum;
	}


	private static float getDb(long sumOfSquares, long samplesLength) {
		double rootMeanSquare = Math.sqrt(sumOfSquares / samplesLength);
		if (rootMeanSquare > 1) {
			// TODO: why 10?
			return (float) (10 * Math.log10(rootMeanSquare));
		}
		return 0;
	}


	/*
	 * <p>Converts two bytes to a short, assuming that the 2nd byte is
	 * more significant (LITTLE_ENDIAN format).</p>
	 *
	 * <pre>
	 * 255 | (255 << 8)
	 * 65535
	 * </pre>
	 */
	private static short getShort(byte argB1, byte argB2) {
		return (short) (argB1 | (argB2 << 8));
	}
}