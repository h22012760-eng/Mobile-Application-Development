package com.puzzleverse.game;

import java.util.ArrayList;
import java.util.List;

public class AchievementManager {

    private final GamePreferences prefs;

    // Callback so PuzzleActivity can show the unlock popup
    public interface OnAchievementUnlocked {
        void onUnlocked(Achievement achievement);
    }

    private OnAchievementUnlocked listener;

    public AchievementManager(GamePreferences prefs) {
        this.prefs = prefs;
    }

    public void setListener(OnAchievementUnlocked l) {
        this.listener = l;
    }

    public void checkAfterLevel(int levelNum, boolean isDaily,
                                int moves, long elapsedMs,
                                boolean peekUsed) {
        boolean isDifficult = !isDaily
                && GamePreferences.isDifficultLevel(levelNum);

        // ── Home level achievements ────────────────────────────────────
        if (!isDaily) {
            grant("first_piece",
                    prefs.getTotalLevelsCompleted() >= 1);

            grant("ten_levels",
                    prefs.getTotalLevelsCompleted() >= 10);

            grant("halfway",
                    prefs.getTotalLevelsCompleted() >= 25);

            grant("master",
                    prefs.getTotalLevelsCompleted()
                            >= GamePreferences.TOTAL_HOME_LEVELS);

            grant("fire_starter",
                    prefs.getTotalDifficultCompleted() >= 3);

            // Pure instinct — difficult level without peek
            if (isDifficult && !peekUsed) {
                grant("pure_instinct", true);
            }

            // No-peek streak — only track for home levels
            if (!peekUsed) {
                prefs.incrementNoPeekStreak();
            } else {
                prefs.resetNoPeekStreak();
            }
            grant("purist",
                    prefs.getConsecutiveNoPeek() >= 10);

            // Speed achievements — home levels
            if (!isDifficult
                    && elapsedMs < GamePreferences.SPEED_NORMAL_MS) {
                grant("speed_solver", true);
            }
            if (isDifficult
                    && elapsedMs < GamePreferences.SPEED_DIFFICULT_MS) {
                grant("lightning_mind", true);
            }
        }

        // ── Daily achievements ─────────────────────────────────────────
        if (isDaily) {
            grant("daily_debut",
                    prefs.getTotalDailyCompleted() >= 1);

            grant("habit_forming",
                    prefs.getTotalDailyCompleted() >= 5);

            grant("daily_devotee",
                    prefs.getTotalDailyCompleted() >= 15);

            grant("daily_legend",
                    prefs.getTotalDailyCompleted()
                            >= GamePreferences.TOTAL_DAILY_LEVELS);

            if (elapsedMs < GamePreferences.SPEED_DAILY_MS) {
                grant("untouchable", true);
            }
            // Note: daily levels do NOT affect no-peek streak
        }

        // ── Cross-category achievements ────────────────────────────────
        grant("grand_master",
                prefs.getTotalLevelsCompleted()
                        >= GamePreferences.TOTAL_HOME_LEVELS
                        && prefs.getTotalDailyCompleted()
                        >= GamePreferences.TOTAL_DAILY_LEVELS);

        grant("coin_hoarder",
                prefs.getTotalCoinsEarned() >= 500);
    }

    // ── Call when peek is used for the first time ─────────────────────
    public void checkPeekUsed() {
        grant("eagle_eye", true);
    }

    // ── Call when a chain reaction of 5+ pieces correct in one swap ───
    public void checkChainReaction(int piecesSnappedAtOnce) {
        if (piecesSnappedAtOnce >= 5) {
            grant("chain_reaction", true);
        }
    }

    // ── Internal grant helper ─────────────────────────────────────────
    private void grant(String id, boolean condition) {
        if (!condition) return;
        if (prefs.isAchievementGranted(id)) return;
        prefs.markAchievementGranted(id);
        Achievement a = findById(id);
        if (a == null) return;
        a.unlocked = true;
        prefs.addCoins(a.coinReward);
        if (listener != null) listener.onUnlocked(a);
    }

    private Achievement findById(String id) {
        for (Achievement a : buildAll()) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }

    // ── Build full achievement list (used by AchievementsActivity) ────
    public List<Achievement> buildAll() {
        List<Achievement> list = new ArrayList<>();
        int levels    = prefs.getTotalLevelsCompleted();
        int daily     = prefs.getTotalDailyCompleted();
        int difficult = prefs.getTotalDifficultCompleted();
        int noPeek    = prefs.getConsecutiveNoPeek();
        long earned   = prefs.getTotalCoinsEarned();

        // ── Bronze ────────────────────────────────────────────────────
        list.add(new Achievement("first_piece", "🧩",
                "First Piece",
                "Complete your very first puzzle",
                Achievement.TIER_BRONZE,
                prefs.isAchievementGranted("first_piece"),
                Math.min(levels, 1), 1));

        list.add(new Achievement("chain_reaction", "⛓",
                "Chain Reaction",
                "Swap a piece that makes 5+ pieces correct at once",
                Achievement.TIER_BRONZE,
                prefs.isAchievementGranted("chain_reaction"),
                prefs.isAchievementGranted("chain_reaction") ? 1 : 0, 1));

        list.add(new Achievement("daily_debut", "📅",
                "Daily Debut",
                "Complete your first daily challenge",
                Achievement.TIER_BRONZE,
                prefs.isAchievementGranted("daily_debut"),
                Math.min(daily, 1), 1));

        list.add(new Achievement("eagle_eye", "👁",
                "Eagle Eye",
                "Use the Peek feature for the first time",
                Achievement.TIER_BRONZE,
                prefs.isAchievementGranted("eagle_eye"),
                prefs.isAchievementGranted("eagle_eye") ? 1 : 0, 1));

        // ── Silver ────────────────────────────────────────────────────
        list.add(new Achievement("ten_levels", "🔟",
                "Getting Hooked",
                "Complete 10 home levels",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("ten_levels"),
                Math.min(levels, 10), 10));

        list.add(new Achievement("pure_instinct", "🎯",
                "Pure Instinct",
                "Complete any difficult level without using Peek",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("pure_instinct"),
                prefs.isAchievementGranted("pure_instinct") ? 1 : 0, 1));

        list.add(new Achievement("speed_solver", "⚡",
                "Speed Solver",
                "Complete any normal level in under 3 minutes",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("speed_solver"),
                prefs.isAchievementGranted("speed_solver") ? 1 : 0, 1));

        list.add(new Achievement("habit_forming", "📆",
                "Habit Forming",
                "Complete 5 daily challenges",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("habit_forming"),
                Math.min(daily, 5), 5));

        list.add(new Achievement("halfway", "⭐",
                "Halfway There",
                "Complete 25 home levels",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("halfway"),
                Math.min(levels, 25), 25));

        list.add(new Achievement("fire_starter", "🔥",
                "Fire Starter",
                "Complete 3 difficult levels",
                Achievement.TIER_SILVER,
                prefs.isAchievementGranted("fire_starter"),
                Math.min(difficult, 3), 3));

        // ── Gold ──────────────────────────────────────────────────────
        list.add(new Achievement("master", "🏆",
                "Puzzle Master",
                "Complete all 50 home levels",
                Achievement.TIER_GOLD,
                prefs.isAchievementGranted("master"),
                Math.min(levels, 50), 50));

        list.add(new Achievement("lightning_mind", "🌩",
                "Lightning Mind",
                "Complete a difficult level in under 5 minutes",
                Achievement.TIER_GOLD,
                prefs.isAchievementGranted("lightning_mind"),
                prefs.isAchievementGranted("lightning_mind") ? 1 : 0, 1));

        list.add(new Achievement("purist", "🛡",
                "The Purist",
                "Complete 10 levels in a row without using Peek",
                Achievement.TIER_GOLD,
                prefs.isAchievementGranted("purist"),
                Math.min(noPeek, 10), 10));

        list.add(new Achievement("daily_devotee", "🌟",
                "Daily Devotee",
                "Complete 15 daily challenges",
                Achievement.TIER_GOLD,
                prefs.isAchievementGranted("daily_devotee"),
                Math.min(daily, 15), 15));

        // ── Diamond ───────────────────────────────────────────────────
        list.add(new Achievement("daily_legend", "💎",
                "Daily Legend",
                "Complete all 30 daily challenges",
                Achievement.TIER_DIAMOND,
                prefs.isAchievementGranted("daily_legend"),
                Math.min(daily, 30), 30));

        list.add(new Achievement("untouchable", "🚀",
                "Untouchable",
                "Complete any 7×7 daily challenge in under 8 minutes",
                Achievement.TIER_DIAMOND,
                prefs.isAchievementGranted("untouchable"),
                prefs.isAchievementGranted("untouchable") ? 1 : 0, 1));

        list.add(new Achievement("coin_hoarder", "🪙",
                "Coin Hoarder",
                "Earn 500 coins total across your entire journey",
                Achievement.TIER_DIAMOND,
                prefs.isAchievementGranted("coin_hoarder"),
                (int) Math.min(earned, 500), 500));

        list.add(new Achievement("grand_master", "👑",
                "Grand Master",
                "Complete all 50 levels AND all 30 daily challenges",
                Achievement.TIER_DIAMOND,
                prefs.isAchievementGranted("grand_master"),
                Math.min(levels + daily, 80), 80));

        return list;
    }
}