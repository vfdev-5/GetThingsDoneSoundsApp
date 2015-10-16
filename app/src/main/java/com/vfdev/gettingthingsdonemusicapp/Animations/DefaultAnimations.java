package com.vfdev.gettingthingsdonemusicapp.Animations;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

/**
 * Created by vfomin on 10/17/15.
 */
public class DefaultAnimations {

    TranslateAnimation mButtonAnimation;

    // -------- Public methods
    public DefaultAnimations() {
        setupButtonAnimation();
    }

    public Animation getButtonAnimation() {
        return mButtonAnimation;
    }

    // -------- Other methods

    private void setupButtonAnimation() {
        mButtonAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0.05f);
        mButtonAnimation.setDuration(150);

    }

}
