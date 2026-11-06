package com.kun.pixelwords.game;

/** When ads may appear, kept here so the limits stay in one place and can be tested. */
public final class AdPolicy {

    private AdPolicy() {}

    /** New players see no ad breaks until they have finished this level. */
    public static final int FIRST_AD_BREAK_LEVEL = 8;
    /** Levels played between ad breaks. */
    public static final int LEVELS_PER_AD_BREAK = 3;
    public static final long MIN_MILLIS_BETWEEN_AD_BREAKS = 3 * 60 * 1000L;
    /** Coin videos a player can watch in a day. */
    public static final int COIN_VIDEOS_PER_DAY = 5;

    /**
     * @param levelsPlayed      levels played since the last ad break
     * @param millisSinceBreak  time since the last ad break
     */
    public static boolean adBreakDue(Progress p, int levelsPlayed, long millisSinceBreak) {
        return !p.adsRemoved
                && p.level > FIRST_AD_BREAK_LEVEL
                && levelsPlayed >= LEVELS_PER_AD_BREAK
                && millisSinceBreak >= MIN_MILLIS_BETWEEN_AD_BREAKS;
    }

    public static int coinVideosLeft(Progress p, long today) {
        return p.coinVideoDay == today ? Math.max(0, COIN_VIDEOS_PER_DAY - p.coinVideosToday) : COIN_VIDEOS_PER_DAY;
    }

    /** Pays for a watched video. False if today's videos have run out. */
    public static boolean payCoinVideo(Progress p, long today) {
        if (coinVideosLeft(p, today) == 0) return false;
        p.coinVideosToday = p.coinVideoDay == today ? p.coinVideosToday + 1 : 1;
        p.coinVideoDay = today;
        p.coins += Scoring.REWARDED_AD_COINS;
        return true;
    }
}
