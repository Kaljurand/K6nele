package ee.ioc.phon.android.speak.view;

import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;

// TODO:
// Maybe reuse some code from
// https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/view/GestureDetector.java
public class OnCursorTouchListener implements View.OnTouchListener {

    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int DELAY = ViewConfiguration.getKeyRepeatDelay();

    // constants for Message.what used by GestureHandler below
    private static final int MSG_LONG_PRESS = 1;
    private static final int MSG_DOUBLE_TAP = 2;

    private final Handler mHandlerPress = new GestureHandler(this);

    // TODO: calculate dynamically
    private static final float VERTICAL_SPEED = 3.5f;
    private static final float DISTANCE_SCALE = 0.04f;

    private final int mEdge;

    private boolean mIsLongPress;
    private boolean mIsFirstMove;
    private boolean mIsEdge;

    private float mStartX = 0;
    private float mStartY = 0;
    private int mCursorType = -1;
    private int mTapCounter = 0;

    private Handler mHandler = new Handler();
    private Runnable mTask1 = new Runnable() {
        public void run() {
            onMoveAux(-1, 0);
            mHandler.postDelayed(this, DELAY);
        }
    };
    private Runnable mTask2 = new Runnable() {
        public void run() {
            onMoveAux(1, 1);
            mHandler.postDelayed(this, DELAY);
        }
    };

    public OnCursorTouchListener(int edge) {
        mEdge = edge;
    }

    public void onMove(int numOfChars) {
        // intentionally empty
    }

    public void onMoveSel(int numOfChars, int type) {
        // intentionally empty
    }

    public void onSingleTapMotion() {
        // intentionally empty
    }

    public void onDoubleTapMotion() {
        // intentionally empty
    }

    public void onDown() {
        // intentionally empty
    }

    public void onUp() {
        // intentionally empty
    }

    public void onLongPress() {
        // intentionally empty
    }

    public void onSwipeUp() {
        // intentionally empty
    }

    public void onSwipeDown() {
        // intentionally empty
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();

        float newX = event.getX();
        float newY = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                cancelEdge();
                mStartX = newX;
                mStartY = newY;
                mCursorType = -1;
                mIsLongPress = false;
                mIsFirstMove = true;
                mHandlerPress.removeMessages(MSG_LONG_PRESS);
                mHandlerPress.sendEmptyMessageAtTime(MSG_LONG_PRESS, event.getEventTime() + LONG_PRESS_TIMEOUT);
                if (mTapCounter == 2) {
                    mTapCounter = 3;
                } else {
                    mTapCounter = 1;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = getDistance(mStartX, mStartY, event);
                // TODO: scale by size of the panel / font size?
                int numOfChars = Math.round(DISTANCE_SCALE * distance);
                //Log.i("cursor: " + numOfChars + " (" + distance + "), " + newX + "-" + newY);
                if (newX < mEdge) {
                    mTapCounter = 0;
                    mHandlerPress.removeMessages(MSG_LONG_PRESS);
                    if (!mIsEdge) {
                        mIsEdge = true;
                        mHandler.post(mTask1);
                    }
                } else if (newX > v.getWidth() - mEdge) {
                    mTapCounter = 0;
                    mHandlerPress.removeMessages(MSG_LONG_PRESS);
                    if (!mIsEdge) {
                        mIsEdge = true;
                        mHandler.post(mTask2);
                    }
                } else if (numOfChars > 0) {
                    mHandlerPress.removeMessages(MSG_LONG_PRESS);
                    cancelEdge();
                    mTapCounter = 0;
                    double atan2 = Math.atan2(mStartY - newY, mStartX - newX);
                    if (atan2 > -0.4 && atan2 < 1.97) {
                        if (mCursorType == -1) {
                            mCursorType = 0;
                        }
                        // Swiping left up, allowing +/- 0.4 error
                        onMoveAux(-1 * numOfChars, mCursorType);
                    } else if (atan2 > 2.74 || atan2 < -1.17) {
                        if (mCursorType == -1) {
                            mCursorType = 1;
                        }
                        // Swiping right down
                        onMoveAux(numOfChars, mCursorType);
                    } else if (atan2 > 2) {
                        //onSwipeUp();
                    } else if (atan2 < -0.4) {
                        //onSwipeDown();
                    }
                    mStartX = newX;
                    mStartY = newY;
                } else {
                    cancelEdge();
                }
                if (mIsFirstMove && mTapCounter == 0) {
                    mIsFirstMove = false;
                    onDown();
                }
                break;
            case MotionEvent.ACTION_UP:
                mHandlerPress.removeMessages(MSG_LONG_PRESS);
                cancelEdge();
                if (mTapCounter == 1) {
                    mTapCounter = 2;
                    mHandlerPress.removeMessages(MSG_DOUBLE_TAP);
                    mHandlerPress.sendEmptyMessageAtTime(MSG_DOUBLE_TAP, event.getEventTime() + DOUBLE_TAP_TIMEOUT);
                } else if (mTapCounter == 3) {
                } else {
                    mTapCounter = 0;
                }
                mCursorType = -1;
                onUp();
                break;
            case MotionEvent.ACTION_CANCEL:
                // TODO: not sure when this is called
                mHandlerPress.removeMessages(MSG_DOUBLE_TAP);
                mHandlerPress.removeMessages(MSG_LONG_PRESS);
                cancelEdge();
                mCursorType = -1;
                mIsLongPress = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void cancelEdge() {
        if (mHandler != null) mHandler.removeCallbacks(mTask1);
        if (mHandler != null) mHandler.removeCallbacks(mTask2);
        mIsEdge = false;
    }

    private void onMoveAux(int numOfChars, int type) {
        if (mIsLongPress) {
            onMoveSel(numOfChars, type);
        } else {
            onMove(numOfChars);
        }
    }

    private float getDistance(float startX, float startY, MotionEvent ev) {
        float distanceSum = 0;
        /*
        final int historySize = ev.getHistorySize();
        for (int h = 0; h < historySize; h++) {
            // historical point
            float hx = ev.getHistoricalX(0, h);
            float hy = ev.getHistoricalY(0, h);
            // distance between startX,startY and historical point
            float dx = (hx - startX);
            float dy = VERTICAL_SPEED * (hy - startY);
            distanceSum += Math.sqrt(dx * dx + dy * dy);
            // make historical point the start point for next loop iteration
            startX = hx;
            startY = hy;
        }
        // add distance from last historical point to event's point
        */
        float dx = (ev.getX(0) - startX);
        float dy = VERTICAL_SPEED * (ev.getY(0) - startY);
        distanceSum += Math.sqrt(dx * dx + dy * dy);
        return distanceSum;
    }

    private void fireLongPress() {
        mHandlerPress.removeMessages(MSG_DOUBLE_TAP);
        mIsLongPress = true;
        mTapCounter = 0;
        cancelEdge();
        onLongPress();
    }

    private void fireTap() {
        if (!mIsLongPress) {
            if (mTapCounter == 3) {
                onDoubleTapMotion();
            } else if (mTapCounter == 2) {
                onSingleTapMotion();
            }
        }
        mTapCounter = 0;
    }

    private static class GestureHandler extends Handler {
        private final WeakReference<OnCursorTouchListener> mRef;

        public GestureHandler(OnCursorTouchListener c) {
            mRef = new WeakReference<>(c);
        }

        @Override
        public void handleMessage(Message msg) {
            OnCursorTouchListener outerClass = mRef.get();
            if (outerClass != null) {
                switch (msg.what) {
                    case MSG_DOUBLE_TAP:
                        outerClass.fireTap();
                        break;
                    case MSG_LONG_PRESS:
                        outerClass.fireLongPress();
                        break;
                    default:
                        throw new RuntimeException("Unknown message " + msg); //never
                }
            }
        }
    }
}