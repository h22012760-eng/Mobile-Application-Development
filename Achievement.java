package com.puzzleverse.game;

public class Achievement {

    // Tier constants
    public static final int TIER_BRONZE  = 0;
    public static final int TIER_SILVER  = 1;
    public static final int TIER_GOLD    = 2;
    public static final int TIER_DIAMOND = 3;

    // Coin rewards per tier
    public static final int[] TIER_COINS = {20, 50, 100, 200};

    public String id;
    public String emoji;
    public String title;
    public String description;
    public int    tier;
    public int    coinReward;
    public boolean unlocked;
    public int    progress;
    public int    maxProgress;

    public Achievement(String id, String emoji, String title,
                       String description, int tier,
                       boolean unlocked, int progress, int maxProgress) {
        this.id          = id;
        this.emoji       = emoji;
        this.title       = title;
        this.description = description;
        this.tier        = tier;
        this.coinReward  = TIER_COINS[tier];
        this.unlocked    = unlocked;
        this.progress    = progress;
        this.maxProgress = maxProgress;
    }

    public String getTierLabel() {
        switch (tier) {
            case TIER_BRONZE:  return "Bronze";
            case TIER_SILVER:  return "Silver";
            case TIER_GOLD:    return "Gold";
            case TIER_DIAMOND: return "Diamond";
            default:           return "";
        }
    }
}