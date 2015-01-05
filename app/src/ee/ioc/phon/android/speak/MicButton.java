package ee.ioc.phon.android.speak;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

public class MicButton extends ImageButton {

    // TODO: take these from some configuration
    private static final float DB_MIN = 15.0f;
    private static final float DB_MAX = 30.0f;

    private AudioCue mAudioCue;

    private Drawable mDrawableMic;
    private Drawable mDrawableMicTranscribing;

    private List<Drawable> mVolumeLevels;

    private Animation mAnimFadeInOutInf;

    private int mVolumeLevel = 0;
    private int mMaxLevel;
    private AudioPauser mAudioPauser;

    public MicButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public MicButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public MicButton(Context context) {
        super(context);
        init(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public void setState(Constants.State state) {
        switch (state) {
            case INIT:
                mAudioPauser.resume();
                clearAnimation();
                setBackgroundDrawable(mDrawableMic);
                break;
            case RECORDING:
                if (mAudioCue != null) mAudioCue.playStartSoundAndSleep();
                mAudioPauser.pause();
                setBackgroundDrawable(mVolumeLevels.get(0));
                break;
            case LISTENING:
                break;
            case TRANSCRIBING:
                if (mAudioCue != null) mAudioCue.playStopSound();
                mAudioPauser.resume();
                setBackgroundDrawable(mDrawableMicTranscribing);
                startAnimation(mAnimFadeInOutInf);
                break;
            case ERROR:
                if (mAudioCue != null) mAudioCue.playErrorSound();
                mAudioPauser.resume();
                clearAnimation();
                setBackgroundDrawable(mDrawableMic);
                break;
            default:
                break;
        }
        setEnabled(true);
    }


    public void setVolumeLevel(float rmsdB) {
        int index = (int) ((rmsdB - DB_MIN) / (DB_MAX - DB_MIN) * mMaxLevel);
        int level = Math.min(Math.max(0, index), mMaxLevel);
        if (level != mVolumeLevel) {
            mVolumeLevel = level;
            setBackgroundDrawable(mVolumeLevels.get(level));
        }
    }

    private void initAnimations(Context context) {
        Resources res = getResources();
        mDrawableMic = res.getDrawable(R.drawable.button_mic);
        mDrawableMicTranscribing = res.getDrawable(R.drawable.button_mic_transcribing);

        mVolumeLevels = new ArrayList<Drawable>();
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_0));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_1));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_2));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_3));
        mMaxLevel = mVolumeLevels.size() - 1;

        mAnimFadeInOutInf = AnimationUtils.loadAnimation(context, R.anim.fade_inout_inf);
    }

    // TODO: get the prefs outside of this class, from the attrs
    // TODO: preferences can change meanwhile
    private void init(Context context, SharedPreferences prefs) {
        if (Utils.getPrefBoolean(prefs, getResources(), R.string.keyImeAudioCues, R.bool.defaultImeAudioCues)) {
            mAudioCue = new AudioCue(context);
        } else {
            mAudioCue = null;
        }
        mAudioPauser = new AudioPauser(context);
        initAnimations(context);

        // Vibrate when the microphone key is pressed down
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // TODO: what is the diff between KEYBOARD_TAP and the other constants?
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                return false;
            }
        });
    }

}