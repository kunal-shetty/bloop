package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Goals worked out from the stats Progress already keeps, so unlocking saves nothing extra. Every stat
 * only ever grows, so an achievement stays unlocked once earned. The save only records which rewards
 * have been claimed, by id, so add new achievements with a new id and never renumber.
 */
public enum Achievement {
    LEVELS_10(1, "Clear 10 levels", "star", Tier.BRONZE, Stat.LEVELS, 10),
    LEVELS_50(2, "Clear 50 levels", "star", Tier.SILVER, Stat.LEVELS, 50),
    LEVELS_100(3, "Clear 100 levels", "star", Tier.GOLD, Stat.LEVELS, 100),
    WORDS_100(4, "Find 100 words", "book", Tier.BRONZE, Stat.WORDS, 100),
    WORDS_1000(5, "Find 1,000 words", "book", Tier.SILVER, Stat.WORDS, 1000),
    DAILIES_7(6, "Solve 7 dailies", "calendar", Tier.BRONZE, Stat.DAILIES, 7),
    DAILIES_30(7, "Solve 30 dailies", "calendar", Tier.GOLD, Stat.DAILIES, 30),
    STREAK_7(8, "Reach a 7-day streak", "flame", Tier.SILVER, Stat.BEST_STREAK, 7),
    STREAK_30(9, "Reach a 30-day streak", "flame", Tier.GOLD, Stat.BEST_STREAK, 30),
    TIMED_500(10, "Score 500 in Timed", "clock", Tier.BRONZE, Stat.BEST_TIMED, 500),
    TIMED_1000(11, "Score 1,000 in Timed", "clock", Tier.SILVER, Stat.BEST_TIMED, 1000),
    DUEL_1(12, "Win a duel", "swords", Tier.BRONZE, Stat.DUEL_WINS, 1),
    DUEL_10(13, "Win 10 duels", "swords", Tier.SILVER, Stat.DUEL_WINS, 10),
    FIRST_ITEM(14, "Buy your cat an accessory", "bag", Tier.BRONZE, Stat.ITEMS, 1),
    ALL_ITEMS(15, "Own 12 accessories", "bag", Tier.GOLD, Stat.ITEMS, 12);

    public enum Tier {
        BRONZE(25), SILVER(75), GOLD(200);

        public final int coins;

        Tier(int coins) {
            this.coins = coins;
        }
    }

    private enum Stat { LEVELS, WORDS, DAILIES, BEST_STREAK, BEST_TIMED, DUEL_WINS, ITEMS }

    public final String title;
    public final String icon;
    public final Tier tier;
    public final int goal;
    private final int id;
    private final Stat stat;

    Achievement(int id, String title, String icon, Tier tier, Stat stat, int goal) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.tier = tier;
        this.stat = stat;
        this.goal = goal;
    }

    /** How far the player has got, capped at the goal. */
    public int progress(Progress p) {
        return Math.min(goal, value(p));
    }

    public boolean isUnlocked(Progress p) {
        return value(p) >= goal;
    }

    public boolean isClaimed(Progress p) {
        return (p.claimedAchievements & bit()) != 0;
    }

    public boolean isClaimable(Progress p) {
        return isUnlocked(p) && !isClaimed(p);
    }

    /** Pays the reward once. Returns false if it isn't earned yet or was already paid. */
    public boolean claim(Progress p) {
        if (!isClaimable(p)) return false;
        p.claimedAchievements |= bit();
        p.coins += tier.coins;
        return true;
    }

    private long bit() {
        return 1L << id;
    }

    private int value(Progress p) {
        switch (stat) {
            case LEVELS: return p.level - 1; // p.level is the next level to play
            case WORDS: return p.totalWordsFound;
            case DAILIES: return p.dailiesCompleted;
            case BEST_STREAK: return p.streak.best;
            case BEST_TIMED: return p.bestTimed;
            case DUEL_WINS: return p.duelWins;
            default: return Long.bitCount(p.ownedItems);
        }
    }

    public static Set<Achievement> unlocked(Progress p) {
        Set<Achievement> set = EnumSet.noneOf(Achievement.class);
        for (Achievement a : values()) if (a.isUnlocked(p)) set.add(a);
        return set;
    }

    /** What the player has earned since the {@code before} snapshot was taken. */
    public static List<Achievement> newlyUnlocked(Set<Achievement> before, Progress now) {
        List<Achievement> fresh = new ArrayList<>();
        for (Achievement a : unlocked(now)) if (!before.contains(a)) fresh.add(a);
        return fresh;
    }

    public static List<Achievement> claimable(Progress p) {
        List<Achievement> ready = new ArrayList<>();
        for (Achievement a : values()) if (a.isClaimable(p)) ready.add(a);
        return ready;
    }
}
