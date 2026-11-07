package com.kun.pixelwords.game;

import java.util.Random;

/**
 * The shape of the campaign: chapters, which levels are hard, and chapter rewards. Everything here is
 * worked out from the level number, so every device agrees and only progress needs saving.
 */
public final class Journey {

    public static final int LEVELS_PER_CHAPTER = 20;
    public static final int MAX_STARS = 3;

    private static final long HARD_SALT = 0x4A4F_5552_4E45_5901L;
    /** Levels at the start of a chapter stay normal, so a new region opens gently. */
    private static final int FIRST_HARD_SLOT = 6;
    /** The last level of a chapter is its boss, so the levels before it are left to lead up to that. */
    private static final int LAST_HARD_SLOT = LEVELS_PER_CHAPTER - 1;
    private static final int MIN_HARD_PER_CHAPTER = 2;
    private static final int MAX_HARD_PER_CHAPTER = 3;

    private Journey() {}

    /** Zero-based chapter of a level. */
    public static int chapterOf(int level) {
        return (level - 1) / LEVELS_PER_CHAPTER;
    }

    /** A level's place in its chapter, from 1 to LEVELS_PER_CHAPTER. */
    public static int slotOf(int level) {
        return (level - 1) % LEVELS_PER_CHAPTER + 1;
    }

    public static int firstLevel(int chapter) {
        return chapter * LEVELS_PER_CHAPTER + 1;
    }

    public static int lastLevel(int chapter) {
        return firstLevel(chapter) + LEVELS_PER_CHAPTER - 1;
    }

    public static String chapterName(int chapter) {
        return Region.forChapter(chapter).name;
    }

    /** Whether a level is one of its chapter's few hard levels. */
    public static boolean isHard(int level) {
        return hardSlots(chapterOf(level))[slotOf(level)];
    }

    /** Index by slot: true means that slot is hard. Random-looking, but the same on every device. */
    private static boolean[] hardSlots(int chapter) {
        Random rnd = new Random(HARD_SALT + chapter * 0x9E37_79B9L);
        int range = LAST_HARD_SLOT - FIRST_HARD_SLOT + 1;
        int wanted = MIN_HARD_PER_CHAPTER + rnd.nextInt(MAX_HARD_PER_CHAPTER - MIN_HARD_PER_CHAPTER + 1);
        boolean[] hard = new boolean[LEVELS_PER_CHAPTER + 1];
        for (int picked = 0; picked < wanted; ) {
            int slot = FIRST_HARD_SLOT + rnd.nextInt(range);
            if (!hard[slot]) {
                hard[slot] = true;
                picked++;
            }
        }
        return hard;
    }

    /** The last level of every chapter is a boss. */
    public static boolean isBoss(int level) {
        return slotOf(level) == LEVELS_PER_CHAPTER;
    }

    /** The region a level is in, which also says who its boss is. */
    public static Region regionOf(int level) {
        return Region.forChapter(chapterOf(level));
    }

    /** What beating a chapter's boss wins, the first time. */
    public static Reward bossPrize(int level) {
        return chapterOf(level) % 2 == 0 ? Reward.sticker() : Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1);
    }

    public static int coinsForLevel(int level) {
        if (isBoss(level)) return Scoring.COINS_PER_BOSS;
        return isHard(level) ? Scoring.COINS_PER_HARD_LEVEL : Scoring.COINS_PER_LEVEL;
    }

    /** Stars to show on the map. Levels cleared before stars existed show one, and can be replayed for more. */
    public static int starsShown(Progress p, int level) {
        if (level >= p.level) return p.starsFor(level);
        return Math.max(1, p.starsFor(level));
    }

    public static int chaptersFinished(Progress p) {
        return (p.level - 1) / LEVELS_PER_CHAPTER;
    }

    public static int chaptersToReward(Progress p) {
        return Math.max(0, chaptersFinished(p) - p.chaptersRewarded);
    }

    /** Pays for every finished chapter not yet rewarded, and returns how many that was. */
    public static int payChapterRewards(Progress p) {
        int owed = chaptersToReward(p);
        p.coins += owed * Scoring.COINS_PER_CHAPTER;
        p.hintTokens += owed * Scoring.HINTS_PER_CHAPTER;
        p.chaptersRewarded += owed;
        return owed;
    }
}
