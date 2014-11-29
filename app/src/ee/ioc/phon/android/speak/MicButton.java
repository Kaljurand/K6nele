package ee.ioc.phon.android.speak;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
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

    private Animation mAnimFadeIn;
    private Animation mAnimFadeOut;
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
    }


    public void setVolumeLevel(float rmsdB) {
        int index = (int) ((rmsdB - DB_MIN) / (DB_MAX - DB_MIN) * mMaxLevel);
        int level = Math.min(Math.max(0, index), mMaxLevel);
        if (level != mVolumeLevel) {
            mVolumeLevel = level;
            setBackgroundDrawable(mVolumeLevels.get(level));
        }
    }


    public void fadeIn() {
        Animations.startFadeAnimation(mAnimFadeIn, this, View.VISIBLE);
    }


    public void fadeOut() {
        Animations.startFadeAnimation(mAnimFadeOut, this, View.INVISIBLE);
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

        mAnimFadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        mAnimFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        mAnimFadeInOutInf = AnimationUtils.loadAnimation(context, R.anim.fade_inout_inf);
    }

    private void init(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(getResources().getString(R.string.keyAudioCues),
                getResources().getBoolean(R.bool.defaultAudioCues))) {
            mAudioCue = new AudioCue(context);
        } else {
            mAudioCue = null;
        }
        mAudioPauser = new AudioPauser(context);
        initAnimations(context);
    }

}