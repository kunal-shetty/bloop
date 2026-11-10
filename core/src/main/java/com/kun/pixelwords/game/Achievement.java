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
    // JOURNEY
    LEVELS_10(1, Category.JOURNEY, "Clear 10 levels", "star", Tier.BRONZE, Stat.LEVELS, 10),
    LEVELS_50(2, Category.JOURNEY, "Clear 50 levels", "star", Tier.SILVER, Stat.LEVELS, 50),
    LEVELS_100(3, Category.JOURNEY, "Clear 100 levels", "star", Tier.GOLD, Stat.LEVELS, 100),
    LEVELS_250(16, Category.JOURNEY, "Clear 250 levels", "star", Tier.GOLD, Stat.LEVELS, 250),
    LEVELS_500(17, Category.JOURNEY, "Clear 500 levels", "star", Tier.PLATINUM, Stat.LEVELS, 500),
    BOSS_1(18, Category.JOURNEY, "Beat your first boss", "trophy", Tier.BRONZE, Stat.CHAPTERS, 1),
    BOSS_5(19, Category.JOURNEY, "Beat 5 bosses", "trophy", Tier.SILVER, Stat.CHAPTERS, 5),
    BOSS_15(20, Category.JOURNEY, "Beat 15 bosses", "trophy", Tier.GOLD, Stat.CHAPTERS, 15),
    BOSS_40(21, Category.JOURNEY, "Beat 40 bosses", "trophy", Tier.PLATINUM, Stat.CHAPTERS, 40),
    STARS_25(22, Category.JOURNEY, "Earn 3 stars on 25 levels", "star", Tier.SILVER, Stat.PERFECT_LEVELS, 25),
    STARS_100(23, Category.JOURNEY, "Earn 3 stars on 100 levels", "star", Tier.GOLD, Stat.PERFECT_LEVELS, 100),

    // WORDS
    WORDS_100(4, Category.WORDS, "Find 100 words", "book", Tier.BRONZE, Stat.WORDS, 100),
    WORDS_1000(5, Category.WORDS, "Find 1,000 words", "book", Tier.SILVER, Stat.WORDS, 1000),
    WORDS_5000(24, Category.WORDS, "Find 5,000 words", "book", Tier.GOLD, Stat.WORDS, 5000),
    WORDS_20000(25, Category.WORDS, "Find 20,000 words", "book", Tier.PLATINUM, Stat.WORDS, 20000),

    // DAILY AND STREAK
    DAILIES_7(6, Category.DAILY, "Solve 7 dailies", "calendar", Tier.BRONZE, Stat.DAILIES, 7),
    DAILIES_30(7, Category.DAILY, "Solve 30 dailies", "calendar", Tier.GOLD, Stat.DAILIES, 30),
    DAILIES_100(26, Category.DAILY, "Solve 100 dailies", "calendar", Tier.GOLD, Stat.DAILIES, 100),
    DAILIES_365(27, Category.DAILY, "Solve 365 dailies", "calendar", Tier.PLATINUM, Stat.DAILIES, 365),
    STREAK_3(28, Category.DAILY, "Reach a 3-day streak", "flame", Tier.BRONZE, Stat.BEST_STREAK, 3),
    STREAK_7(8, Category.DAILY, "Reach a 7-day streak", "flame", Tier.SILVER, Stat.BEST_STREAK, 7),
    STREAK_14(29, Category.DAILY, "Reach a 14-day streak", "flame", Tier.SILVER, Stat.BEST_STREAK, 14),
    STREAK_30(9, Category.DAILY, "Reach a 30-day streak", "flame", Tier.GOLD, Stat.BEST_STREAK, 30),
    STREAK_100(30, Category.DAILY, "Reach a 100-day streak", "flame", Tier.PLATINUM, Stat.BEST_STREAK, 100),

    // MODES
    TIMED_500(10, Category.MODES, "Score 500 in Timed", "clock", Tier.BRONZE, Stat.BEST_TIMED, 500),
    TIMED_1000(11, Category.MODES, "Score 1,000 in Timed", "clock", Tier.SILVER, Stat.BEST_TIMED, 1000),
    TIMED_2500(31, Category.MODES, "Score 2,500 in Timed", "clock", Tier.GOLD, Stat.BEST_TIMED, 2500),
    DUEL_1(12, Category.MODES, "Win a duel", "swords", Tier.BRONZE, Stat.DUEL_WINS, 1),
    DUEL_10(13, Category.MODES, "Win 10 duels", "swords", Tier.SILVER, Stat.DUEL_WINS, 10),
    DUEL_50(32, Category.MODES, "Win 50 duels", "swords", Tier.GOLD, Stat.DUEL_WINS, 50),
    TOWER_5(33, Category.MODES, "Climb 5 floors of the Word Tower", "tower", Tier.BRONZE, Stat.TOWER, 5),
    TOWER_20(34, Category.MODES, "Climb 20 floors of the Word Tower", "tower", Tier.SILVER, Stat.TOWER, 20),
    TOWER_50(35, Category.MODES, "Climb 50 floors of the Word Tower", "tower", Tier.GOLD, Stat.TOWER, 50),
    RUSH_3(36, Category.MODES, "Beat 3 bosses in one Boss Rush", "trophy", Tier.SILVER, Stat.RUSH, 3),
    RUSH_10(37, Category.MODES, "Beat 10 bosses in one Boss Rush", "trophy", Tier.GOLD, Stat.RUSH, 10),

    // COLLECTION
    FIRST_ITEM(14, Category.COLLECTION, "Buy your cat an accessory", "bag", Tier.BRONZE, Stat.ITEMS, 1),
    ALL_ITEMS(15, Category.COLLECTION, "Own 12 accessories", "bag", Tier.GOLD, Stat.ITEMS, 12),
    ITEMS_40(38, Category.COLLECTION, "Own 40 accessories", "bag", Tier.PLATINUM, Stat.ITEMS, 40),
    FURNITURE_5(39, Category.COLLECTION, "Place 5 pieces of furniture", "home", Tier.BRONZE, Stat.FURNITURE, 5),
    FURNITURE_20(40, Category.COLLECTION, "Own 20 pieces of furniture", "home", Tier.SILVER, Stat.FURNITURE, 20),
    THEMES_3(41, Category.COLLECTION, "Own 3 room themes", "home", Tier.SILVER, Stat.THEMES, 3),
    BREEDS_2(42, Category.COLLECTION, "Adopt a second cat breed", "heart", Tier.SILVER, Stat.BREEDS, 2),
    BREEDS_5(43, Category.COLLECTION, "Own 5 cat breeds", "heart", Tier.GOLD, Stat.BREEDS, 5),
    GIFT_CODE(44, Category.COLLECTION, "Redeem a gift code", "ticket", Tier.BRONZE, Stat.CODES, 1);

    /** Where an achievement is listed on the Goals screen. */
    public enum Category {
        JOURNEY("JOURNEY"), WORDS("WORDS"), DAILY("DAILY AND STREAK"), MODES("GAME MODES"), COLLECTION("COLLECTION");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public enum Tier {
        BRONZE(25), SILVER(75), GOLD(200), PLATINUM(500);

        public final int coins;

        Tier(int coins) {
            this.coins = coins;
        }
    }

    private enum Stat {
        LEVELS, WORDS, DAILIES, BEST_STREAK, BEST_TIMED, DUEL_WINS, ITEMS, CHAPTERS, PERFECT_LEVELS,
        TOWER, RUSH, FURNITURE, THEMES, BREEDS, CODES
    }

    public final Category category;
    public final String title;
    public final String icon;
    public final Tier tier;
    public final int goal;
    private final int id;
    private final Stat stat;

    Achievement(int id, Category category, String title, String icon, Tier tier, Stat stat, int goal) {
        this.id = id;
        this.category = category;
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
        p.addCoins(tier.coins);
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
            case CHAPTERS: return Journey.chaptersFinished(p);
            case PERFECT_LEVELS: return perfectLevels(p);
            case TOWER: return p.bestTower;
            case RUSH: return p.bestRush;
            case FURNITURE: return Long.bitCount(p.homeOwned);
            case THEMES: return Long.bitCount(p.themesOwned);
            case BREEDS: return Long.bitCount(p.breedsOwned);
            case CODES: return Long.bitCount(p.redeemedCodes);
            default: return Shop.ownedCount(p);
        }
    }

    private static int perfectLevels(Progress p) {
        int n = 0;
        for (int level = 1; level < p.level; level++) if (p.starsFor(level) >= Journey.MAX_STARS) n++;
        return n;
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
