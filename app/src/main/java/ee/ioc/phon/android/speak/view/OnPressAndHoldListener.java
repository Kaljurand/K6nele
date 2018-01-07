package ee.ioc.phon.android.speak.view;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public abstract class OnPressAndHoldListener implements View.OnTouchListener {

    private static final int TIMEOUT = ViewConfiguration.getKeyRepeatTimeout();
    private static final int DELAY = ViewConfiguration.getKeyRepeatDelay();

    private volatile Looper mLooper;
    private volatile Handler mHandler;
    private Runnable mTask;

    abstract void onAction();

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                HandlerThread thread = new HandlerThread("Thread", Process.THREAD_PRIORITY_BACKGROUND);
                thread.start();
                mLooper = thread.getLooper();
                mHandler = new Handler(mLooper);
                mTask = new Runnable() {
                    public void run() {
                        onAction();
                        mHandler.postDelayed(this, DELAY);
                    }
                };
                mHandler.postDelayed(mTask, TIMEOUT);
                onAction();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mHandler != null) mHandler.removeCallbacks(mTask);
                break;
        }
        return true;
    }
}