package com.kun.pixelwords.game;

/** The text a player shares after a daily. It never names a word, so it can't spoil the puzzle for friends. */
public final class ShareCard {

    /** Epoch day of Daily #1, 2026-11-06. Never change this, or every puzzle number changes. */
    static final long FIRST_DAILY_DAY = 20763;

    private static final String STAR = "⭐";
    private static final String FLAME = "🔥";

    private ShareCard() {}

    /** Puzzle number for a day. A phone with its clock set before launch still gets a valid number. */
    public static long dailyNumber(long epochDay) {
        return Math.max(1, epochDay - FIRST_DAILY_DAY + 1);
    }

    public static String daily(long epochDay, int stars, int streak, int hints, int score) {
        StringBuilder sb = new StringBuilder("Bloop Daily #").append(dailyNumber(epochDay)).append('\n');
        for (int i = 0; i < stars; i++) sb.append(STAR);
        sb.append(hints == 0 ? "  No hints!" : "  " + hints + (hints == 1 ? " hint" : " hints")).append('\n');
        sb.append(FLAME).append(' ').append(streak).append("-day streak\n");
        sb.append("Score ").append(score);
        return sb.toString();
    }
}
