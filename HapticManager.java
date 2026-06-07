package com.puzzleverse.game;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class HapticManager {

    private final Vibrator        vibrator;
    private final GamePreferences prefs;

    public HapticManager(Context context, GamePreferences prefs) {
        this.prefs    = prefs;
        this.vibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);
    }

    // Single piece placed correctly
    public void vibrateSnap() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createOneShot(
                22, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    // Group of 2-3 pieces
    public void vibrateSmallGroup() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createWaveform(
                new long[]{0, 22, 40, 30}, -1));
    }

    // Group of 4+ pieces
    public void vibrateLargeGroup() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createWaveform(
                new long[]{0, 22, 35, 30, 35, 40}, -1));
    }

    // Scales vibration to group size automatically
    public void vibrateForGroup(int groupSize) {
        if (!enabled()) return;
        if (groupSize <= 1)      vibrateSnap();
        else if (groupSize <= 3) vibrateSmallGroup();
        else                     vibrateLargeGroup();
    }

    // Puzzle complete — celebratory pattern
    public void vibrateComplete() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createWaveform(
                new long[]{0, 80, 60, 120, 60, 160}, -1));
    }

    // Move rejected — locked piece tapped
    public void vibrateReject() {
        if (!enabled()) return;
        vibrate(VibrationEffect.createOneShot(
                15, 80));
    }

    private boolean enabled() {
        return prefs.isVibrationEnabled()
                && vibrator != null
                && vibrator.hasVibrator();
    }

    private void vibrate(VibrationEffect effect) {
        try { vibrator.vibrate(effect); }
        catch (Exception ignored) {}
    }
}