package com.puzzleverse.game;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class AnimationHelper {

    // ─── Fade in ──────────────────────────────────────────────────────
    public static void fadeIn(View view, long durationMs) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(durationMs);
        anim.setFillAfter(true);
        anim.setInterpolator(new DecelerateInterpolator());
        view.startAnimation(anim);
    }

    public static void fadeIn(View view, long durationMs, long delayMs) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(durationMs);
        anim.setStartOffset(delayMs);
        anim.setFillAfter(true);
        anim.setInterpolator(new DecelerateInterpolator());
        view.startAnimation(anim);
    }

    // ─── Scale pop (used on coin display when coins change) ───────────
    public static void scalePop(View view) {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new BounceInterpolator());

        ScaleAnimation grow = new ScaleAnimation(
                1f, 1.25f, 1f, 1.25f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        grow.setDuration(150);

        ScaleAnimation shrink = new ScaleAnimation(
                1.25f, 1f, 1.25f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        shrink.setDuration(150);
        shrink.setStartOffset(150);

        set.addAnimation(grow);
        set.addAnimation(shrink);
        set.setFillAfter(true);
        view.startAnimation(set);
    }

    // ─── Slide up from bottom (for cards and dialogs) ─────────────────
    public static void slideUp(View view, long durationMs) {
        TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f);
        anim.setDuration(durationMs);
        anim.setInterpolator(new DecelerateInterpolator(1.5f));
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    // ─── Bounce in (for level complete emoji) ─────────────────────────
    public static void bounceIn(View view) {
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new BounceInterpolator());

        ScaleAnimation scale = new ScaleAnimation(
                0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);

        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        alpha.setDuration(300);

        set.addAnimation(scale);
        set.addAnimation(alpha);
        set.setFillAfter(true);
        view.startAnimation(set);
    }

    // ─── Shake (for locked level tap) ─────────────────────────────────
    public static void shake(View view) {
        TranslateAnimation anim = new TranslateAnimation(
                0, 18, 0, 0);
        anim.setDuration(80);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(4);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(anim);
    }

    // ─── Pulse (for coin display) ─────────────────────────────────────
    public static void pulse(View view) {
        ScaleAnimation anim = new ScaleAnimation(
                1f, 1.1f, 1f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(300);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        view.startAnimation(anim);
    }
}