package ee.ioc.phon.android.speak.view;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import ee.ioc.phon.android.speak.Log;

// TODO:
// - single tap
// - double tap
// - long press
// - add newline swipe (?)
// Maybe reuse some code from
// https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/view/GestureDetector.java
public class OnCursorTouchListener implements View.OnTouchListener {

    private float mStartX = 0;
    private float mStartY = 0;

    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    private boolean mIsLongPress;
    private boolean mIsMoving;

    private int cursorType = -1;

    public OnCursorTouchListener(Context context) {
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

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();

        float newX = event.getX();
        float newY = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                onDown();
                mStartX = newX;
                mStartY = newY;
                cursorType = -1;
                mIsLongPress = false;
                mIsMoving = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = getDistance(mStartX, mStartY, event);
                // TODO: scale by size of the panel
                int numOfChars = Math.round(distance / 25);
                Log.i("distance: " + numOfChars + " " + distance);
                if (numOfChars > 0) {
                    mIsMoving = true;
                    double atan2 = Math.atan2(mStartY - newY, mStartX - newX);
                    if (atan2 > -0.4 && atan2 < 1.97) {
                        if (cursorType == -1) {
                            cursorType = 0;
                        }
                        // Swiping left up, allowing +/- 0.4 error
                        onMoveAux(-1 * numOfChars, cursorType);
                    } else if (atan2 > 2.74 || atan2 < -1.17) {
                        if (cursorType == -1) {
                            cursorType = 1;
                        }
                        // Swiping right down
                        onMoveAux(numOfChars, cursorType);
                    }
                    mStartX = newX;
                    mStartY = newY;
                } else if (!mIsMoving && (event.getEventTime() - event.getDownTime()) > LONGPRESS_TIMEOUT) {
                    mIsLongPress = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onUp();
                cursorType = -1;
                mIsLongPress = false;
                mIsMoving = false;
                break;
        }
        return true;
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
        final int historySize = ev.getHistorySize();
        Log.i("distance: historySize: " + historySize);
        for (int h = 0; h < historySize; h++) {
            // historical point
            float hx = ev.getHistoricalX(0, h);
            float hy = ev.getHistoricalY(0, h);
            // distance between startX,startY and historical point
            float dx = (hx - startX);
            float dy = 3 * (hy - startY);
            distanceSum += Math.sqrt(dx * dx + dy * dy);
            // make historical point the start point for next loop iteration
            startX = hx;
            startY = hy;
        }
        // add distance from last historical point to event's point
        float dx = (ev.getX(0) - startX);
        float dy = 3 * (ev.getY(0) - startY);
        distanceSum += Math.sqrt(dx * dx + dy * dy);
        return distanceSum;
    }
}