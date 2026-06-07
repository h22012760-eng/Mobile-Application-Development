package com.puzzleverse.game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AuroraToast {

    public enum Position { TOP, CENTER, BOTTOM }

    // Show a quick aurora toast — auto-dismisses, no buttons needed
    public static void show(AppCompatActivity activity,
                            String emoji, String title, String message,
                            Position position, int durationMs) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        View toast = LayoutInflater.from(activity)
                .inflate(R.layout.view_aurora_toast, root, false);

        TextView tvEmoji   = toast.findViewById(R.id.tv_aurora_toast_emoji);
        TextView tvTitle   = toast.findViewById(R.id.tv_aurora_toast_title);
        TextView tvMessage = toast.findViewById(R.id.tv_aurora_toast_message);

        if (tvEmoji   != null) tvEmoji.setText(emoji);
        if (tvTitle   != null) tvTitle.setText(title);
        if (tvMessage != null) {
            if (message == null || message.isEmpty()) {
                tvMessage.setVisibility(View.GONE);
            } else {
                tvMessage.setText(message);
                tvMessage.setVisibility(View.VISIBLE);
            }
        }

        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        int marginDp = (int)(24 * activity.getResources().getDisplayMetrics().density);
        switch (position) {
            case TOP:
                lp.topMargin = (int)(100 * activity.getResources().getDisplayMetrics().density);
                break;
            case BOTTOM:
                lp.bottomMargin = marginDp;
                break;
            case CENTER:
            default:
                break;
        }
        toast.setLayoutParams(lp);
        root.addView(toast);

        // Centre horizontally
        toast.post(() -> {
            int rw = root.getWidth();
            int tw = toast.getWidth();
            if (rw > 0 && tw > 0) toast.setX((rw - tw) / 2f);

            if (position == Position.CENTER) {
                int rh = root.getHeight();
                int th = toast.getHeight();
                if (rh > 0 && th > 0) toast.setY((rh - th) / 2f);
            } else if (position == Position.BOTTOM) {
                int rh = root.getHeight();
                int th = toast.getHeight();
                if (rh > 0 && th > 0)
                    toast.setY(rh - th - lp.bottomMargin);
            }
        });

        // Entrance animation — scale up with overshoot
        toast.setAlpha(0f);
        toast.setScaleX(0.4f);
        toast.setScaleY(0.4f);

        AnimatorSet enter = new AnimatorSet();
        enter.playTogether(
                ObjectAnimator.ofFloat(toast, "alpha", 0f, 1f).setDuration(350),
                ObjectAnimator.ofFloat(toast, "scaleX", 0.4f, 1f).setDuration(500),
                ObjectAnimator.ofFloat(toast, "scaleY", 0.4f, 1f).setDuration(500));
        enter.setInterpolator(new OvershootInterpolator(2f));
        enter.start();

        // Auto-dismiss
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet exit = new AnimatorSet();
            exit.playTogether(
                    ObjectAnimator.ofFloat(toast, "alpha", 1f, 0f).setDuration(280),
                    ObjectAnimator.ofFloat(toast, "scaleX", 1f, 0.5f).setDuration(280),
                    ObjectAnimator.ofFloat(toast, "scaleY", 1f, 0.5f).setDuration(280));
            exit.setInterpolator(new DecelerateInterpolator());
            exit.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    if (toast.getParent() != null)
                        ((ViewGroup) toast.getParent()).removeView(toast);
                }
            });
            exit.start();
        }, durationMs);
    }

    // Convenience shortcuts
    public static void showTop(AppCompatActivity a, String emoji,
                               String title, String msg) {
        show(a, emoji, title, msg, Position.TOP, 2500);
    }

    public static void showCenter(AppCompatActivity a, String emoji,
                                  String title, String msg) {
        show(a, emoji, title, msg, Position.CENTER, 2000);
    }
}