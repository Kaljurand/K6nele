package ee.ioc.phon.android.speak;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;

// TODO: add a method that calls back when audio is finished
public class AudioCue {

    private static final int DELAY_AFTER_START_BEEP = 200;

    private final Context mContext;

    public AudioCue(Context context) {
        mContext = context;
    }

    public void playStartSoundAndSleep() {
        if (playSound(R.raw.explore_begin)) {
            SystemClock.sleep(DELAY_AFTER_START_BEEP);
        }
    }


    public void playStopSound() {
        playSound(R.raw.explore_end);
    }


    public void playErrorSound() {
        playSound(R.raw.error);
    }


    private boolean playSound(int sound) {
        MediaPlayer mp = MediaPlayer.create(mContext, sound);
        // create can return null, e.g. on Android Wear
        if (mp == null) {
            return false;
        }
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        mp.start();
        return true;
    }

}