package ee.ioc.phon.android.speak;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class PcmRecorder implements Runnable {

    public interface RecorderListener {
        void onRecorderBuffer(byte[] buffer);
    }

    private static final int frequency = 16000;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private final Object mutex = new Object();
    private int bufferRead = 0;
    private byte[] tempBuffer;
    private int bufferSize;
    private RecorderListener mListener;
    private AudioRecord recordInstance;
    private volatile boolean isRecording;

    public PcmRecorder() {
        // TODO: maybe do this later
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
                throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
            } else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
                throw new IllegalStateException("read() returned AudioRecord.ERROR_BAD_VALUE");
            } else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
                throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
            }
            mListener.onRecorderBuffer(tempBuffer);
        }
        recordInstance.stop();
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

    public void setListener(RecorderListener listener) {
        mListener = listener;
    }
}