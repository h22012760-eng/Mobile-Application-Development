package com.puzzleverse.game;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_MS = 3000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvIcon    = findViewById(R.id.tv_icon);
        TextView tvName    = findViewById(R.id.tv_app_name);
        TextView tvTagline = findViewById(R.id.tv_tagline);

        // Entrance animation
        if (tvIcon != null) {
            tvIcon.setAlpha(0f);
            tvIcon.setTranslationY(-180f);
            tvIcon.animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(700)
                    .setInterpolator(new BounceInterpolator())
                    .setStartDelay(200)
                    .start();
        }

        if (tvName != null) {
            tvName.setAlpha(0f);
            tvName.animate().alpha(1f).setDuration(600).setStartDelay(600).start();
        }

        if (tvTagline != null) {
            tvTagline.setAlpha(0f);
            tvTagline.animate().alpha(1f).setDuration(600).setStartDelay(1000).start();
        }

        handler.postDelayed(this::navigate, SPLASH_MS);
    }

    private void navigate() {
        // ALWAYS go to MainActivity (home/dashboard) — never to last played level
        // Clear entire back stack so pressing back exits the app cleanly
        boolean tutorialShown = getSharedPreferences(
                "PuzzleVersePrefs", MODE_PRIVATE)
                .getBoolean("tutorial_shown", false);

        Intent intent = tutorialShown
                ? new Intent(this, MainActivity.class)
                : new Intent(this, OnboardingActivity.class);

        // These flags ensure clean start — no activity from previous session remains
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
