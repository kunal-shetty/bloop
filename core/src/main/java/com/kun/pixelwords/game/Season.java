package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.SeedUtil;

/**
 * A 30-day pass worked out from the date, so a new season arrives with no update and no server. Each
 * tier pays on a free track and on a premium track. Anything earned but not claimed when a season ends
 * is paid automatically, so a player never loses a reward.
 */
public final class Season {

    private Season() {}

    public static final int TIERS = 30;
    public static final int XP_PER_TIER = 80;
    public static final int DAYS = 30;
    /** Coins for the premium track. Turn this off to sell it for real money instead. */
    public static final boolean PREMIUM_WITH_COINS = true;
    public static final int PREMIUM_COINS = 1500;

    public static final int XP_LEVEL = 10;
    public static final int XP_DAILY = 25;
    public static final int XP_QUEST = 20;

    /** 2026-11-01, the first day of season 1. */
    private static final long FIRST_DAY = 20758;
    private static final long SALT = 0x5345_4153_4F4E_0001L;

    /** The six seasons, repeated in order. Keep the order: a season number picks its theme from it. */
    public enum Theme {
        FROST("Frost", 0xFF62C6FF, Shop.Item.ICE_CROWN, Home.Item.WINDOW_AURORA),
        BLOOM("Bloom", 0xFFFF8BA7, Shop.Item.BLOSSOM_HAT, Home.Item.CHERRY_TREE),
        SUNSHINE("Sunshine", 0xFFFFD34D, Shop.Item.SUN_HAT, Home.Item.RUG_WAVE),
        STORM("Storm", 0xFF8C68DC, Shop.Item.BOLT_SHADES, Home.Item.LAMP_STORM),
        HARVEST("Harvest", 0xFFFF9A3C, Shop.Item.PUMPKIN_HAT, Home.Item.BED_PUMPKIN),
        STARLIGHT("Starlight", 0xFF6BE3A8, Shop.Item.STAR_CLOAK, Home.Item.ART_GALAXY);

        public final String title;
        public final int color;
        public final Shop.Item accessory;
        public final Home.Item furniture;

        Theme(String title, int color, Shop.Item accessory, Home.Item furniture) {
            this.title = title;
            this.color = color;
            this.accessory = accessory;
            this.furniture = furniture;
        }
    }

    /** The season number for a day. Season 1 is the first; the save's 0 means none has started. */
    public static long indexOf(long epochDay) {
        return Math.max(1, Math.floorDiv(epochDay - FIRST_DAY, (long) DAYS) + 1);
    }

    /** Days into the season: 0 is its first day. */
    public static int dayOf(long epochDay) {
        return (int) Math.floorMod(epochDay - FIRST_DAY, (long) DAYS);
    }

    public static int daysLeft(long epochDay) {
        return DAYS - dayOf(epochDay);
    }

    public static Theme theme(long season) {
        Theme[] themes = Theme.values();
        return themes[(int) Math.floorMod(season - 1, (long) themes.length)];
    }

    /** Starts a new season if one has begun, and returns true if the save changed. */
    public static boolean rollOver(Progress p, long epochDay) {
        long season = indexOf(epochDay);
        if (p.seasonId == season) return false;
        payUnclaimed(p);
        FishHunt.settle(p);
        p.seasonId = season;
        p.seasonXp = 0;
        p.seasonClaimedFree = 0;
        p.seasonClaimedPremium = 0;
        p.seasonPremium = false;
        return true;
    }

    private static void payUnclaimed(Progress p) {
        for (int tier = 1; tier <= tiersReached(p); tier++) {
            claim(p, tier, false);
            if (p.seasonPremium) claim(p, tier, true);
        }
    }

    /** Adds XP to the season in progress, starting a new one first if it's due. */
    public static void addXp(Progress p, long epochDay, int xp) {
        rollOver(p, epochDay);
        p.seasonXp += xp;
    }

    public static int tiersReached(Progress p) {
        return Math.min(TIERS, p.seasonXp / XP_PER_TIER);
    }

    /** XP gained towards the next tier. */
    public static int xpIntoTier(Progress p) {
        return tiersReached(p) >= TIERS ? XP_PER_TIER : p.seasonXp % XP_PER_TIER;
    }

    /** The reward on a track at a tier. */
    public static Reward reward(long season, int tier, boolean premium) {
        return premium ? premiumReward(theme(season), season, tier) : freeReward(tier);
    }

    private static Reward freeReward(int tier) {
        if (tier == TIERS) return Reward.coins(300);
        if (tier == 15) return Reward.hints(3);
        if (tier % 10 == 0) return Reward.powerUp(PowerUps.Kind.PICK_LETTER, 1);
        if (tier % 5 == 0) return Reward.hints(2);
        return Reward.coins(20 + tier * 2);
    }

    private static Reward premiumReward(Theme theme, long season, int tier) {
        if (tier == TIERS) return Reward.item(theme.accessory);
        if (tier == 20) return Reward.furniture(theme.furniture);
        if (tier == 25 && theme == Theme.STARLIGHT) return Reward.breed(Shop.Breed.GALAXY);
        if (tier == 10) return Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 2);
        if (tier % 5 == 0) return Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1);
        if (tier % 3 == 0) return Reward.powerUp(PowerUps.Kind.PICK_LETTER, 1);
        return Reward.coins(40 + tier * 3);
    }

    public static boolean isClaimed(Progress p, int tier, boolean premium) {
        long claimed = premium ? p.seasonClaimedPremium : p.seasonClaimedFree;
        return (claimed & 1L << tier) != 0;
    }

    public static boolean isClaimable(Progress p, int tier, boolean premium) {
        return tier >= 1 && tier <= tiersReached(p) && !isClaimed(p, tier, premium) && (!premium || p.seasonPremium);
    }

    public static int claimableCount(Progress p) {
        int ready = 0;
        for (int tier = 1; tier <= TIERS; tier++) {
            if (isClaimable(p, tier, false)) ready++;
            if (isClaimable(p, tier, true)) ready++;
        }
        return ready;
    }

    /** Pays one tier once. Returns what was paid, or null if it isn't ready. */
    public static Reward claim(Progress p, int tier, boolean premium) {
        if (!isClaimable(p, tier, premium)) return null;
        if (premium) p.seasonClaimedPremium |= 1L << tier;
        else p.seasonClaimedFree |= 1L << tier;
        long seed = SeedUtil.mix(SALT + p.seasonId, tier * 2L + (premium ? 1 : 0));
        return reward(p.seasonId, tier, premium).grant(p, seed);
    }

    /** Unlocks the premium track with coins. False if it's unlocked, or can't be bought with coins. */
    public static boolean buyPremium(Progress p) {
        if (!PREMIUM_WITH_COINS || p.seasonPremium || p.coins < PREMIUM_COINS) return false;
        p.coins -= PREMIUM_COINS;
        p.seasonPremium = true;
        return true;
    }
}
