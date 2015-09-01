package ee.ioc.phon.android.speak;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
    private Drawable mDrawableMic;
    private Drawable mDrawableMicTranscribing;

    private List<Drawable> mVolumeLevels;

    private Animation mAnimFadeInOutInf;

    private int mVolumeLevel = 0;
    private int mMaxLevel;

    public MicButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MicButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MicButton(Context context) {
        super(context);
        init(context);
    }

    public void setState(Constants.State state) {
        switch (state) {
            case INIT:
                clearAnimation();
                setBackgroundDrawable(mDrawableMic);
                break;
            case RECORDING:
                break;
            case LISTENING:
                setBackgroundDrawable(mVolumeLevels.get(0));
                break;
            case TRANSCRIBING:
                setBackgroundDrawable(mDrawableMicTranscribing);
                startAnimation(mAnimFadeInOutInf);
                break;
            case ERROR:
                clearAnimation();
                setBackgroundDrawable(mDrawableMic);
                break;
            default:
                break;
        }
    }


    public void setVolumeLevel(float rmsdB) {
        int index = (int) ((rmsdB - Constants.DB_MIN) / (Constants.DB_MAX - Constants.DB_MIN) * mMaxLevel);
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

        mVolumeLevels = new ArrayList<>();
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_0));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_1));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_2));
        mVolumeLevels.add(res.getDrawable(R.drawable.button_mic_recording_3));
        mMaxLevel = mVolumeLevels.size() - 1;

        mAnimFadeInOutInf = AnimationUtils.loadAnimation(context, R.anim.fade_inout_inf);
    }

    private void init(Context context) {
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