package ee.ioc.phon.android.speak.view;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Fires an action upon movement, provided that certain amount of time has passed.
 * TODO: fires less actions if there is less movement, which is a little confusing.
 */
public abstract class OnPressAndHoldListener implements View.OnTouchListener {

    private static final int TIMEOUT = ViewConfiguration.getKeyRepeatTimeout();
    private static final int DELAY = ViewConfiguration.getKeyRepeatDelay();

    private boolean mIsTimeout;
    private long mActionTime = 0;

    abstract void onAction();

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsTimeout = false;
                mActionTime = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                long time = event.getEventTime() - event.getDownTime();
                if (mIsTimeout) {
                    if (time - mActionTime > DELAY) {
                        onAction();
                        mActionTime = time;
                    }
                } else if (time > TIMEOUT) {
                    mIsTimeout = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onAction();
                break;
        }
        return true;
    }
}