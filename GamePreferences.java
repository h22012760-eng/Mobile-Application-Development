package com.puzzleverse.game;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

public class GamePreferences {

    private static final String PREF_NAME = "PuzzleVersePrefs";

    // Keys
    private static final String KEY_COINS               = "coins";
    private static final String KEY_TOTAL_EARNED        = "total_coins_ever_earned";
    private static final String KEY_SOUND               = "sound_enabled";
    private static final String KEY_MUSIC               = "music_enabled";
    private static final String KEY_VIBRATION           = "vibration_enabled";
    private static final String KEY_MYSTERY_MODE        = "mystery_mode";
    private static final String KEY_LEVEL_STATUS        = "level_status_";
    private static final String KEY_DAILY_STATUS        = "daily_status_";
    private static final String KEY_DAILY_LAST_PLAYED   = "daily_last_played_";
    private static final String KEY_TOTAL_LEVELS        = "total_levels_completed";
    private static final String KEY_TOTAL_DAILY         = "total_daily_completed";
    private static final String KEY_TOTAL_DIFFICULT     = "total_difficult_completed";
    private static final String KEY_NO_PEEK_STREAK      = "consecutive_no_peek";
    private static final String KEY_FIRST_LAUNCH        = "first_launch";
    private static final String KEY_FIRST_WIN_SHOWN     = "first_win_shown";
    private static final String KEY_TUTORIAL_SHOWN      = "tutorial_shown";
    private static final String KEY_BEST_MOVES          = "best_moves_";
    private static final String KEY_BEST_TIME           = "best_time_";
    private static final String KEY_BEST_STARS          = "stars_";
    private static final String KEY_ACH                 = "ach_granted_";
    private static final String KEY_LAST_LOGIN          = "last_login_date_ms";
    private static final String KEY_LOGIN_STREAK        = "login_streak";
    private static final String KEY_TOTAL_LOGIN_DAYS    = "total_login_days";

    // Status constants
    public static final int STATUS_LOCKED    = 0;
    public static final int STATUS_UNLOCKED  = 1;
    public static final int STATUS_COMPLETED = 2;

    // Coin values
    public static final int COINS_NORMAL_LEVEL    = 10;
    public static final int COINS_DIFFICULT_LEVEL = 25;
    public static final int COINS_DAILY_CHALLENGE = 40;
    public static final int COINS_PEEK_COST       = 15;
    public static final int COINS_GHOST_COST      = 30;
    public static final int COINS_DAILY_LOGIN     = 50;
    public static final int COINS_STREAK_BONUS    = 200;

    // Level counts
    public static final int TOTAL_HOME_LEVELS  = 50;
    public static final int TOTAL_DAILY_LEVELS = 30;

    // Move limits
    public static final int MOVE_LIMIT_NORMAL    = 60;
    public static final int MOVE_LIMIT_DIFFICULT = 100;
    public static final int MOVE_LIMIT_DAILY     = 150;

    // Star thresholds (% of move limit used)
    public static final float STAR3_THRESHOLD = 0.40f;
    public static final float STAR2_THRESHOLD = 0.70f;

    // Speed thresholds (ms)
    public static final long SPEED_NORMAL_MS    = 180_000L;
    public static final long SPEED_DIFFICULT_MS = 300_000L;
    public static final long SPEED_DAILY_MS     = 480_000L;

    private final SharedPreferences      prefs;
    private final SharedPreferences.Editor editor;

    public GamePreferences(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        initIfFirstLaunch();
    }

    // ── First launch ───────────────────────────────────────────────────
    private void initIfFirstLaunch() {
        if (!prefs.getBoolean(KEY_FIRST_LAUNCH, true)) return;
        editor.putBoolean(KEY_FIRST_LAUNCH, false);
        editor.putInt(KEY_COINS, 0);
        editor.putLong(KEY_TOTAL_EARNED, 0L);
        editor.putBoolean(KEY_SOUND, true);
        editor.putBoolean(KEY_MUSIC, true);
        editor.putBoolean(KEY_VIBRATION, true);
        editor.putBoolean(KEY_MYSTERY_MODE, false);
        editor.putInt(KEY_TOTAL_LEVELS, 0);
        editor.putInt(KEY_TOTAL_DAILY, 0);
        editor.putInt(KEY_TOTAL_DIFFICULT, 0);
        editor.putInt(KEY_NO_PEEK_STREAK, 0);
        editor.putInt(KEY_LOGIN_STREAK, 0);
        editor.putInt(KEY_TOTAL_LOGIN_DAYS, 0);
        editor.putLong(KEY_LAST_LOGIN, 0L);
        // Unlock level 1 and daily 1
        editor.putInt(KEY_LEVEL_STATUS + 1, STATUS_UNLOCKED);
        for (int i = 2; i <= TOTAL_HOME_LEVELS; i++)
            editor.putInt(KEY_LEVEL_STATUS + i, STATUS_LOCKED);
        editor.putInt(KEY_DAILY_STATUS + 1, STATUS_UNLOCKED);
        for (int i = 2; i <= TOTAL_DAILY_LEVELS; i++)
            editor.putInt(KEY_DAILY_STATUS + i, STATUS_LOCKED);
        editor.apply();
    }

    // ── Coins ──────────────────────────────────────────────────────────
    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        editor.putInt(KEY_COINS, getCoins() + amount);
        editor.putLong(KEY_TOTAL_EARNED,
                prefs.getLong(KEY_TOTAL_EARNED, 0L) + amount);
        editor.apply();
    }

    public boolean spendCoins(int amount) {
        int cur = getCoins();
        if (cur < amount) return false;
        editor.putInt(KEY_COINS, cur - amount);
        editor.apply();
        return true;
    }

    public long getTotalCoinsEarned() {
        return prefs.getLong(KEY_TOTAL_EARNED, 0L);
    }

    // ── Settings ───────────────────────────────────────────────────────
    public boolean isSoundEnabled()     { return prefs.getBoolean(KEY_SOUND, true); }
    public void setSoundEnabled(boolean v)  { editor.putBoolean(KEY_SOUND, v); editor.apply(); }
    public boolean isMusicEnabled()     { return prefs.getBoolean(KEY_MUSIC, true); }
    public void setMusicEnabled(boolean v)  { editor.putBoolean(KEY_MUSIC, v); editor.apply(); }
    public boolean isVibrationEnabled() { return prefs.getBoolean(KEY_VIBRATION, true); }
    public void setVibrationEnabled(boolean v){ editor.putBoolean(KEY_VIBRATION, v); editor.apply(); }
    public boolean isMysteryModeEnabled(){ return prefs.getBoolean(KEY_MYSTERY_MODE, false); }
    public void setMysteryMode(boolean v){ editor.putBoolean(KEY_MYSTERY_MODE, v); editor.apply(); }

    // ── Home levels ────────────────────────────────────────────────────
    public int getLevelStatus(int n) {
        return prefs.getInt(KEY_LEVEL_STATUS + n, STATUS_LOCKED);
    }

    public void completeLevel(int n) {
        editor.putInt(KEY_LEVEL_STATUS + n, STATUS_COMPLETED);
        if (n < TOTAL_HOME_LEVELS
                && getLevelStatus(n + 1) == STATUS_LOCKED)
            editor.putInt(KEY_LEVEL_STATUS + (n + 1), STATUS_UNLOCKED);
        editor.putInt(KEY_TOTAL_LEVELS,
                prefs.getInt(KEY_TOTAL_LEVELS, 0) + 1);
        if (isDifficultLevel(n))
            editor.putInt(KEY_TOTAL_DIFFICULT,
                    prefs.getInt(KEY_TOTAL_DIFFICULT, 0) + 1);
        editor.apply();
    }

    public int getTotalLevelsCompleted() {
        return prefs.getInt(KEY_TOTAL_LEVELS, 0);
    }

    public int getTotalDifficultCompleted() {
        return prefs.getInt(KEY_TOTAL_DIFFICULT, 0);
    }

    // ── Daily challenges ───────────────────────────────────────────────
    public int getDailyStatus(int n) {
        return prefs.getInt(KEY_DAILY_STATUS + n, STATUS_LOCKED);
    }

    public void completeDaily(int n) {
        editor.putInt(KEY_DAILY_STATUS + n, STATUS_COMPLETED);
        editor.putLong(KEY_DAILY_LAST_PLAYED + n, System.currentTimeMillis());
        if (n < TOTAL_DAILY_LEVELS
                && getDailyStatus(n + 1) == STATUS_LOCKED)
            editor.putInt(KEY_DAILY_STATUS + (n + 1), STATUS_UNLOCKED);
        editor.putInt(KEY_TOTAL_DAILY,
                prefs.getInt(KEY_TOTAL_DAILY, 0) + 1);
        editor.apply();
    }

    public boolean isDailyPlayedToday(int n) {
        long last = prefs.getLong(KEY_DAILY_LAST_PLAYED + n, 0L);
        if (last == 0) return false;
        Calendar a = Calendar.getInstance(); a.setTimeInMillis(last);
        Calendar b = Calendar.getInstance();
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public int getTotalDailyCompleted() {
        return prefs.getInt(KEY_TOTAL_DAILY, 0);
    }

    public long getMsUntilNextDay() {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.add(Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis() - System.currentTimeMillis();
    }

    public static String formatCountdown(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        return String.format("%02d:%02d:%02d", h, m % 60, s % 60);
    }

    // ── Login streak ───────────────────────────────────────────────────
    public int getLoginStreak() {
        return prefs.getInt(KEY_LOGIN_STREAK, 0);
    }

    // Call once per app open. Returns coins awarded (0 if already opened today).
    public int recordDailyLogin() {
        long lastMs  = prefs.getLong(KEY_LAST_LOGIN, 0L);
        Calendar now = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(lastMs);

        boolean isToday = lastMs != 0
                && now.get(Calendar.YEAR) == last.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR);

        if (isToday) return 0; // Already recorded today

        boolean isYesterday = false;
        if (lastMs != 0) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            isYesterday = yesterday.get(Calendar.YEAR) == last.get(Calendar.YEAR)
                    && yesterday.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR);
        }

        int streak = isYesterday ? getLoginStreak() + 1 : 1;
        int days   = prefs.getInt(KEY_TOTAL_LOGIN_DAYS, 0) + 1;

        editor.putLong(KEY_LAST_LOGIN, now.getTimeInMillis());
        editor.putInt(KEY_LOGIN_STREAK, streak);
        editor.putInt(KEY_TOTAL_LOGIN_DAYS, days);

        int coinsAwarded = COINS_DAILY_LOGIN;
        if (streak % 7 == 0) coinsAwarded += COINS_STREAK_BONUS;
        addCoins(coinsAwarded);
        editor.apply();
        return coinsAwarded;
    }

    public int getTotalLoginDays() {
        return prefs.getInt(KEY_TOTAL_LOGIN_DAYS, 0);
    }

    // ── No-peek streak ─────────────────────────────────────────────────
    public int getConsecutiveNoPeek() {
        return prefs.getInt(KEY_NO_PEEK_STREAK, 0);
    }

    public void incrementNoPeekStreak() {
        editor.putInt(KEY_NO_PEEK_STREAK,
                getConsecutiveNoPeek() + 1);
        editor.apply();
    }

    public void resetNoPeekStreak() {
        editor.putInt(KEY_NO_PEEK_STREAK, 0);
        editor.apply();
    }

    // ── Personal bests ─────────────────────────────────────────────────
    public int getBestMoves(int n, boolean isDaily) {
        return prefs.getInt(
                (isDaily ? "d" : "l") + KEY_BEST_MOVES + n,
                Integer.MAX_VALUE);
    }

    public boolean updateBestMoves(int n, boolean isDaily, int moves) {
        int prev = getBestMoves(n, isDaily);
        if (moves < prev) {
            editor.putInt((isDaily ? "d" : "l") + KEY_BEST_MOVES + n, moves);
            editor.apply();
            return true;
        }
        return false;
    }

    public long getBestTime(int n, boolean isDaily) {
        return prefs.getLong(
                (isDaily ? "d" : "l") + KEY_BEST_TIME + n,
                Long.MAX_VALUE);
    }

    public boolean updateBestTime(int n, boolean isDaily, long ms) {
        long prev = getBestTime(n, isDaily);
        if (ms < prev) {
            editor.putLong((isDaily ? "d" : "l") + KEY_BEST_TIME + n, ms);
            editor.apply();
            return true;
        }
        return false;
    }

    public int getBestStars(int n, boolean isDaily) {
        return prefs.getInt(
                (isDaily ? "d" : "l") + KEY_BEST_STARS + n, 0);
    }

    public void updateBestStars(int n, boolean isDaily, int stars) {
        if (stars > getBestStars(n, isDaily)) {
            editor.putInt((isDaily ? "d" : "l") + KEY_BEST_STARS + n, stars);
            editor.apply();
        }
    }

    // ── Achievements ───────────────────────────────────────────────────
    public boolean isAchievementGranted(String id) {
        return prefs.getBoolean(KEY_ACH + id, false);
    }

    public void markAchievementGranted(String id) {
        editor.putBoolean(KEY_ACH + id, true);
        editor.apply();
    }

    // ── Special flags ──────────────────────────────────────────────────
    public boolean isFirstWinShown() {
        return prefs.getBoolean(KEY_FIRST_WIN_SHOWN, false);
    }

    public void markFirstWinShown() {
        editor.putBoolean(KEY_FIRST_WIN_SHOWN, true);
        editor.apply();
    }

    public boolean isTutorialShown() {
        return prefs.getBoolean(KEY_TUTORIAL_SHOWN, false);
    }

    // ── Static helpers ─────────────────────────────────────────────────
    public static boolean isDifficultLevel(int n) { return n % 3 == 0; }

    // Grid cols and rows — rectangular pieces
    public static int getGridCols(int levelNumber) {
        return isDifficultLevel(levelNumber) ? 6 : 5;
    }

    public static int getGridRows(int levelNumber) {
        // More rows than cols = taller pieces = less blank space
        return isDifficultLevel(levelNumber) ? 7 : 6;
    }

    public static int getDailyGridCols() { return 7; }
    public static int getDailyGridRows() { return 9; }

    public static int getCoinReward(int n) {
        return isDifficultLevel(n)
                ? COINS_DIFFICULT_LEVEL : COINS_NORMAL_LEVEL;
    }

    public static int getMoveLimit(boolean isDifficult, boolean isDaily) {
        if (isDaily)     return MOVE_LIMIT_DAILY;
        if (isDifficult) return MOVE_LIMIT_DIFFICULT;
        return MOVE_LIMIT_NORMAL;
    }

    public static int calcStars(int moves, int moveLimit) {
        float used = (float) moves / moveLimit;
        if (used <= STAR3_THRESHOLD) return 3;
        if (used <= STAR2_THRESHOLD) return 2;
        return 1;
    }

    // ── Reset everything ───────────────────────────────────────────────
    public void resetAllProgress() {
        editor.clear();
        editor.putBoolean(KEY_TUTORIAL_SHOWN, true); // Keep tutorial shown
        editor.apply();
        // Re-initialise
        initIfFirstLaunch();
    }
}