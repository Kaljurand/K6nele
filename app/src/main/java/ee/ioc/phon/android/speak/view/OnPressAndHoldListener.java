package ee.ioc.phon.android.speak.view;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public abstract class OnPressAndHoldListener implements View.OnTouchListener {

    private static final int TIMEOUT = ViewConfiguration.getKeyRepeatTimeout();
    private static final int DELAY = ViewConfiguration.getKeyRepeatDelay();

    private Handler mHandler = new Handler();
    private Runnable mTask = new Runnable() {
        public void run() {
            onAction();
            mHandler.postDelayed(this, DELAY);
        }
    };

    abstract void onAction();

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHandler.postDelayed(mTask, TIMEOUT);
                onAction();
                v.setPressed(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                if (mHandler != null) mHandler.removeCallbacks(mTask);
                v.setPressed(false);
                break;
            default:
                break;
        }
        return true;
    }
}