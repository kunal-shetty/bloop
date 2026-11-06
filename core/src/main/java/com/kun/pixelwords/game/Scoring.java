package com.kun.pixelwords.game;

/** Score and coin rules in one place so the game, leaderboards and duels all agree. */
public final class Scoring {

    private Scoring() {}

    public static final int COINS_PER_BONUS_WORD = 2;
    public static final int COINS_PER_LEVEL = 10;
    public static final int COINS_PER_HARD_LEVEL = 30;
    public static final int COINS_PER_DAILY = 25;
    public static final int COINS_PER_CHAPTER = 100;
    public static final int HINTS_PER_CHAPTER = 1;
    public static final int HINT_COST_COINS = 30;
    public static final int HINT_SCORE_PENALTY = 25;
    public static final int REWARDED_AD_COINS = 20;

    public static int gridWordPoints(String word) {
        return 10 * word.length();
    }

    public static int bonusWordPoints(String word) {
        return 5 * word.length();
    }

    /**
     * @param secondsLeft time remaining in a timed mode (0 for untimed modes)
     * @return never negative
     */
    public static int levelScore(int gridPoints, int bonusPoints, int secondsLeft, int hintsUsed) {
        long s = (long) gridPoints + bonusPoints + 2L * Math.max(0, secondsLeft)
                - (long) HINT_SCORE_PENALTY * hintsUsed;
        return (int) Math.max(0, s);
    }
}
