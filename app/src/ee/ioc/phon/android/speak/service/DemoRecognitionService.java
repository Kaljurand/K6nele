package ee.ioc.phon.android.speak.service;

import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import ee.ioc.phon.android.speak.utils.Utils;

/**
 * Sends a growing number every few milliseconds for 3 seconds, finally sends
 * the input intent extras as the final result. Does not record audio.
 */
public class DemoRecognitionService extends AbstractRecognitionService {

    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private String mIntentAsString;

    @Override
    void configure(Intent recognizerIntent) {
        mIntentAsString = TextUtils.join("\n",
                Utils.ppBundle(recognizerIntent.getExtras()));
    }

    @Override
    void connect() {

        mRunnable = new Runnable() {
            int counter = 0;

            public void run() {
                onPartialResults(toResultsBundle((++counter) + ""));
                mHandler.postDelayed(this, 500);
            }
        };

        mHandler.postDelayed(mRunnable, 100);
    }

    @Override
    void disconnect() {
        if (mHandler != null) mHandler.removeCallbacks(mRunnable);
    }

    @Override
    void afterRecording(byte[] recording) {
        onResults(toResultsBundle(mIntentAsString));
    }

    int getAutoStopAfterMillis() {
        return 3000; // We record for 3 seconds
    }
}