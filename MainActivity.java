package com.puzzleverse.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private GamePreferences gamePrefs;
    private TextView        tvCoinCount;
    private AuroraBackgroundView auroraBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gamePrefs = new GamePreferences(this);
        auroraBg  = findViewById(R.id.aurora_bg);

        tvCoinCount = findViewById(R.id.tv_coin_count);
        updateCoinDisplay();

        // Streak check
        View streakBanner = findViewById(R.id.streak_banner);
        TextView tvStreak = findViewById(R.id.tv_streak_label);
        int streak = gamePrefs.getLoginStreak();
        if (streak >= 2 && streakBanner != null && tvStreak != null) {
            streakBanner.setVisibility(View.VISIBLE);
            tvStreak.setText(getString(R.string.streak_format, streak));
        }

        // Bottom Nav
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_levels) {
                    startActivity(new Intent(this, LevelSelectActivity.class));
                    return true;
                } else if (id == R.id.nav_daily) {
                    startActivity(new Intent(this, DailyChallengeActivity.class));
                    return true;
                } else if (id == R.id.nav_achievements) {
                    startActivity(new Intent(this, AchievementsActivity.class));
                    return true;
                }
                return id == R.id.nav_home;
            });
        }

        // Play Now button
        MaterialButton btnPlay = findViewById(R.id.btn_play_now);
        if (btnPlay != null) {
            btnPlay.setOnClickListener(this::onPlayClicked);
        }

        // Settings button
        View btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(this::onSettingsClicked);
        }

        // Record daily login
        gamePrefs.recordDailyLogin();
    }

    private void updateCoinDisplay() {
        if (tvCoinCount != null) {
            tvCoinCount.setText(String.valueOf(gamePrefs.getCoins()));
        }
    }

    public void onPlayClicked(View v) {
        // Resume last incomplete level or just go to level select
        int lastLevel = gamePrefs.getTotalLevelsCompleted() + 1;
        int targetLevel = lastLevel > GamePreferences.TOTAL_HOME_LEVELS ? 1 : lastLevel;

        Intent i = new Intent(this, PuzzleActivity.class);
        i.putExtra(PuzzleActivity.EXTRA_LEVEL_NUMBER, targetLevel);
        i.putExtra(PuzzleActivity.EXTRA_IS_DAILY, false);
        startActivity(i);
        TransitionHelper.forward(this);
    }

    public void onSettingsClicked(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
        TransitionHelper.fade(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinDisplay();
        if (auroraBg != null) auroraBg.resumeAnimations();
        
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) nav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (auroraBg != null) auroraBg.pauseAnimations();
    }
}
