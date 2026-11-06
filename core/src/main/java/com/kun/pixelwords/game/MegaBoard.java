package com.kun.pixelwords.game;

/**
 * One huge puzzle each weekend, the same for everyone. Found words are kept in the save, so a board this
 * size can be worked on over both days.
 */
public final class MegaBoard {

    private MegaBoard() {}

    public static final int COINS = 150;
    public static final int SEASON_XP = 100;

    private static final int SATURDAY = 5;

    public static boolean isOpen(long epochDay) {
        return weekday(epochDay) >= SATURDAY;
    }

    /** Days until the next board opens. Zero while one is open. */
    public static int daysUntilOpen(long epochDay) {
        return isOpen(epochDay) ? 0 : SATURDAY - weekday(epochDay);
    }

    /** 0 is Monday: 1970-01-01 was a Thursday. */
    private static int weekday(long epochDay) {
        return (int) Math.floorMod(epochDay + 3, 7L);
    }

    public static boolean isSolved(Progress p, long epochDay) {
        return p.megaSolvedWeek == Quest.weekOf(epochDay);
    }

    /** Makes the save ready for this weekend's board, clearing last weekend's found words. */
    public static void begin(Progress p, long epochDay) {
        long week = Quest.weekOf(epochDay);
        if (p.megaWeek == week) return;
        p.megaWeek = week;
        p.megaFound = 0;
    }

    /** Pays once for this weekend's board. False if it was already paid. */
    public static boolean claim(Progress p, long epochDay) {
        if (isSolved(p, epochDay)) return false;
        p.megaSolvedWeek = Quest.weekOf(epochDay);
        p.coins += COINS;
        return true;
    }
}
