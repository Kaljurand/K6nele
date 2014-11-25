package ee.ioc.phon.android.speak;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class Animations {

    public static void startFadeAnimation(Context context, int resource, final View view, final int finalVisibility) {
        Animation fade = AnimationUtils.loadAnimation(context, resource);
        startFadeAnimation(fade, view, finalVisibility);
    }


    public static void startFadeAnimation(Animation fade, final View view, final int finalVisibility) {

        fade.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(finalVisibility);
                view.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

        });

        view.startAnimation(fade);
    }

}