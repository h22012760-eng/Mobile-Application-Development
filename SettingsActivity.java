package com.puzzleverse.game;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private GamePreferences      gamePrefs;
    private TextView             tvCoinCount;
    private TextView             tvCurrentCoins;
    private AuroraBackgroundView auroraBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        gamePrefs      = new GamePreferences(this);
        auroraBg       = findViewById(R.id.aurora_bg);
        tvCoinCount    = findViewById(R.id.tv_coin_count);
        tvCurrentCoins = findViewById(R.id.tv_current_coins);

        // Switches
        SwitchMaterial switchSound     = findViewById(R.id.switch_sound);
        SwitchMaterial switchMusic     = findViewById(R.id.switch_music);
        SwitchMaterial switchVibration = findViewById(R.id.switch_vibration);
        SwitchMaterial switchMystery   = findViewById(R.id.switch_mystery);

        if (switchSound     != null) {
            switchSound.setChecked(gamePrefs.isSoundEnabled());
            switchSound.setOnCheckedChangeListener(
                    (b, c) -> gamePrefs.setSoundEnabled(c));
        }
        if (switchMusic     != null) {
            switchMusic.setChecked(gamePrefs.isMusicEnabled());
            switchMusic.setOnCheckedChangeListener(
                    (b, c) -> gamePrefs.setMusicEnabled(c));
        }
        if (switchVibration != null) {
            switchVibration.setChecked(gamePrefs.isVibrationEnabled());
            switchVibration.setOnCheckedChangeListener(
                    (b, c) -> gamePrefs.setVibrationEnabled(c));
        }
        if (switchMystery   != null) {
            switchMystery.setChecked(gamePrefs.isMysteryModeEnabled());
            switchMystery.setOnCheckedChangeListener(
                    (b, c) -> gamePrefs.setMysteryMode(c));
        }

        // Back button
        MaterialButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> {
            finish();
            TransitionHelper.modal(this);
        });

        // Reset progress button
        MaterialButton btnReset = findViewById(R.id.btn_reset_progress);
        if (btnReset != null) btnReset.setOnClickListener(v -> showResetConfirm());

        updateCoinDisplays();
    }

    @Override protected void onResume() {
        super.onResume();
        if (auroraBg != null) auroraBg.resumeAnimations();
        updateCoinDisplays();
    }

    @Override protected void onPause() {
        super.onPause();
        if (auroraBg != null) auroraBg.pauseAnimations();
    }

    private void updateCoinDisplays() {
        int coins = gamePrefs.getCoins();
        if (tvCoinCount    != null) tvCoinCount.setText(String.valueOf(coins));
        if (tvCurrentCoins != null)
            tvCurrentCoins.setText("You have " + coins + " coins 🪙");
    }

    private void showResetConfirm() {
        new MaterialAlertDialogBuilder(this, R.style.PuzzleVerseDialog)
                .setTitle("Reset All Progress?")
                .setMessage("This will erase all your levels, coins, achievements, and streaks. This cannot be undone.")
                .setPositiveButton("Reset Everything", (d, w) -> resetAndRestart())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void resetAndRestart() {
        gamePrefs.resetAllProgress();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}