package com.puzzleverse.game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ToastHelper {

    public static final int MODE_NORMAL    = 0;
    public static final int MODE_DIFFICULT = 1;
    public static final int MODE_DAILY     = 2;

    public static void showModeToast(AppCompatActivity activity, int mode) {
        if (mode == MODE_NORMAL) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        View toast = LayoutInflater.from(activity)
                .inflate(R.layout.view_mode_toast, root, false);

        TextView tvEmoji   = toast.findViewById(R.id.tv_toast_emoji);
        TextView tvLabel   = toast.findViewById(R.id.tv_toast_label);
        TextView tvDesc    = toast.findViewById(R.id.tv_toast_desc);

        // NO grid size text — only mode label
        if (mode == MODE_DAILY) {
            tvEmoji.setText(activity.getString(R.string.emoji_calendar));
            tvLabel.setText(activity.getString(R.string.label_mode_daily));
            tvDesc.setText(activity.getString(R.string.mode_daily_desc));
        } else {
            tvEmoji.setText(activity.getString(R.string.emoji_fire));
            tvLabel.setText(activity.getString(R.string.label_mode_difficult));
            tvDesc.setText(activity.getString(R.string.mode_difficult_desc));
        }

        // Position at top, centred
        ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dpToPx(activity, 110);
        toast.setLayoutParams(p);
        root.addView(toast);

        // Centre after layout
        toast.post(() -> {
            int rw = root.getWidth();
            int tw = toast.getWidth();
            toast.setX((rw - tw) / 2f);
        });

        // Entrance — scale up from small with overshoot
        toast.setAlpha(0f);
        toast.setScaleX(0.5f);
        toast.setScaleY(0.5f);
        toast.setTranslationY(-40f);

        AnimatorSet enter = new AnimatorSet();
        enter.playTogether(
                ObjectAnimator.ofFloat(toast, "alpha", 0f, 1f).setDuration(350),
                ObjectAnimator.ofFloat(toast, "scaleX", 0.5f, 1f).setDuration(500),
                ObjectAnimator.ofFloat(toast, "scaleY", 0.5f, 1f).setDuration(500),
                ObjectAnimator.ofFloat(toast, "translationY", -40f, 0f).setDuration(400));
        enter.setInterpolator(new OvershootInterpolator(1.8f));
        enter.start();

        // Auto dismiss after 2500ms
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet exit = new AnimatorSet();
            exit.playTogether(
                    ObjectAnimator.ofFloat(toast, "alpha", 1f, 0f).setDuration(280),
                    ObjectAnimator.ofFloat(toast, "scaleX", 1f, 0.6f).setDuration(280),
                    ObjectAnimator.ofFloat(toast, "scaleY", 1f, 0.6f).setDuration(280),
                    ObjectAnimator.ofFloat(toast, "translationY", 0f, -30f).setDuration(280));
            exit.setInterpolator(new DecelerateInterpolator());
            exit.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    if (toast.getParent() != null)
                        ((ViewGroup) toast.getParent()).removeView(toast);
                }
            });
            exit.start();
        }, 2500);
    }

    private static int dpToPx(Context ctx, int dp) {
        return (int)(dp * ctx.getResources().getDisplayMetrics().density);
    }
}