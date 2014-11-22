package kaldi.speechkit;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class PcmRecorder implements Runnable {

    public static final int frequency = 16000;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private static final String TAG = "PcmRecorder";
    private final Object mutex = new Object();
    int bufferRead = 0;
    byte[] tempBuffer;
    int cAmplitude;
    int bufferSize;
    RecorderListener rl;
    AudioRecord recordInstance;
    private volatile boolean isRecording;

    public PcmRecorder() {
        // TODO Auto-generated constructor stub
        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);


        bufferSize = AudioRecord.getMinBufferSize(frequency,
                AudioFormat.CHANNEL_IN_MONO, audioEncoding);

        tempBuffer = new byte[bufferSize];
        recordInstance = new AudioRecord(
                MediaRecorder.AudioSource.MIC, frequency,
                AudioFormat.CHANNEL_IN_MONO, audioEncoding, bufferSize);


    }

    public void run() {
        synchronized (mutex) {
            while (!this.isRecording) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Wait() interrupted!", e);
                }
            }
        }
        recordInstance.startRecording();
        while (this.isRecording) {
            bufferRead = recordInstance.read(tempBuffer, 0, bufferSize);

            if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
                throw new IllegalStateException(
                        "read() returned AudioRecord.ERROR_INVALID_OPERATION");
            } else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
                throw new IllegalStateException(
                        "read() returned AudioRecord.ERROR_BAD_VALUE");
            } else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
                throw new IllegalStateException(
                        "read() returned AudioRecord.ERROR_INVALID_OPERATION");
            }
            rl.onRecorderBuffer(tempBuffer);

			/*
			for (int i=0; i<bufferRead/2; i++)
			{ // 16bit sample size
				short curSample = getShort(tempBuffer[i*2], tempBuffer[i*2+1]);
				if (curSample > cAmplitude)
				{ // Check amplitude
					cAmplitude = curSample;
				}
			}
			*/
            //this.ws_client_speech.send(tempBuffer);

        }
        recordInstance.stop();
    }

    public int getMaxAmplitude() {
        int result = cAmplitude;
        cAmplitude = 0;
        return result;

    }

    public boolean isRecording() {
        synchronized (mutex) {
            return isRecording;
        }
    }

    public void setRecording(boolean isRecording) {
        synchronized (mutex) {
            this.isRecording = isRecording;
            if (this.isRecording) {
                mutex.notify();
            }
        }
    }

    public void setOutputFile(String audioPath) {
        // TODO Auto-generated method stub

    }

    public int getSampleRate() {
        // TODO Auto-generated method stub
        return frequency;
    }

    public void setListener(Recognizer recognizer) {
        // TODO Auto-generated method stub
        rl = recognizer;
    }


}
