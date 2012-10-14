package ee.ioc.phon.android.speak;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

public class AudioPauser {

	private final AudioManager mAudioManager;
	private final OnAudioFocusChangeListener mAfChangeListener;
	private int mCurrentVolume;
	private boolean isPausing = false;

	public AudioPauser(Context context) {
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		mAfChangeListener = new OnAudioFocusChangeListener() {
			public void onAudioFocusChange(int focusChange) {
				Log.i("onAudioFocusChange" + focusChange);
			}
		};
	}


	/**
	 * Requests audio focus with the goal of pausing any existing audio player.
	 * Additionally mutes the music stream, since some audio players might
	 * ignore the focus request.
	 * In other words, during the pause no sound will be heard,
	 * but whether the audio resumes from the same position after the pause
	 * depends on the audio player.
	 */
	public void pause() {
		if (! isPausing) {
			mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			int result = mAudioManager.requestAudioFocus(mAfChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				Log.i("AUDIOFOCUS_REQUEST_GRANTED");
			}
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
			isPausing = true;
			Log.i("AudioPauser: pause: " + mCurrentVolume);
		}
	}


	/**
	 * Abandons audio focus and restores the audio volume.
	 */
	public void resume() {
		if (isPausing) {
			mAudioManager.abandonAudioFocus(mAfChangeListener);
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
			isPausing = false;
			Log.i("AudioPauser: resume: " + mCurrentVolume);
		}
	}

}