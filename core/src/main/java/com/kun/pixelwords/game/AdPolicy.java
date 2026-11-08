package com.kun.pixelwords.game;

/** When ads may appear, kept here so the limits stay in one place and can be tested. */
public final class AdPolicy {

    private AdPolicy() {}

    /** New players see no ad breaks until they have finished this level. */
    public static final int FIRST_AD_BREAK_LEVEL = 8;
    /** Levels won between ad breaks. */
    public static final int WINS_PER_AD_BREAK = 5;
    /** A video watched this recently pushes a due ad break to the next win, so two ads never run back to back. */
    public static final long REWARDED_GRACE_MILLIS = 60 * 1000L;
    /** Coin videos a player can watch in a day. */
    public static final int COIN_VIDEOS_PER_DAY = 5;

    /**
     * @param winsSinceBreak     levels won since the last ad break
     * @param millisSinceVideo   time since the player last watched a rewarded video
     */
    public static boolean adBreakDue(Progress p, int winsSinceBreak, long millisSinceVideo) {
        return !p.adsRemoved
                && p.level > FIRST_AD_BREAK_LEVEL
                && winsSinceBreak >= WINS_PER_AD_BREAK
                && millisSinceVideo >= REWARDED_GRACE_MILLIS;
    }

    public static int coinVideosLeft(Progress p, long today) {
        return p.coinVideoDay == today ? Math.max(0, COIN_VIDEOS_PER_DAY - p.coinVideosToday) : COIN_VIDEOS_PER_DAY;
    }

    /** Pays for a watched video. False if today's videos have run out. */
    public static boolean payCoinVideo(Progress p, long today) {
        if (coinVideosLeft(p, today) == 0) return false;
        p.coinVideosToday = p.coinVideoDay == today ? p.coinVideosToday + 1 : 1;
        p.coinVideoDay = today;
        p.addCoins(Scoring.REWARDED_AD_COINS);
        return true;
    }
}
