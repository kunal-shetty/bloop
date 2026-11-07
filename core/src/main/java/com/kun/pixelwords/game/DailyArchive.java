package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Past daily puzzles to catch up on. Every daily can be rebuilt from its date, so this only has to
 * remember which days are done. Opening a missed puzzle costs coins and solving it pays some back, so
 * it's a way to spend coins rather than earn them. It never touches the streak.
 */
public final class DailyArchive {

    private DailyArchive() {}

    public static final int DAYS_SHOWN = 28;
    public static final int OPEN_COINS = 50;
    public static final int SOLVE_COINS = 25;

    private static final int DAYS_PER_BLOCK = 64;

    public static boolean isSolved(Progress p, long day) {
        Long block = p.dailyLog.get(Math.floorDiv(day, (long) DAYS_PER_BLOCK));
        return block != null && (block & 1L << Math.floorMod(day, (long) DAYS_PER_BLOCK)) != 0;
    }

    public static void markSolved(Progress p, long day) {
        p.dailyLog.merge(Math.floorDiv(day, (long) DAYS_PER_BLOCK), 1L << Math.floorMod(day, (long) DAYS_PER_BLOCK), (a, b) -> a | b);
    }

    /**
     * Saves from before the log existed only know the current streak, so those days are filled in the
     * first time it's needed. A streak that used a freeze may show a missed day as solved, which is harmless.
     */
    public static void backfill(Progress p) {
        if (!p.dailyLog.isEmpty() || p.streak.lastDay < 0) return;
        for (int i = 0; i < p.streak.streak; i++) markSolved(p, p.streak.lastDay - i);
    }

    /** The days on show, oldest first, ending yesterday. Today belongs to the Daily itself. */
    public static List<Long> days(long today) {
        List<Long> out = new ArrayList<>();
        for (long day = today - DAYS_SHOWN; day < today; day++) out.add(day);
        return out;
    }

    /** How many of the days on show are still to solve. */
    public static int missed(Progress p, long today) {
        int missed = 0;
        for (long day : days(today)) if (!isSolved(p, day)) missed++;
        return missed;
    }

    public static boolean isOnShow(long day, long today) {
        return day < today && day >= today - DAYS_SHOWN;
    }

    /** True if the player has already paid to open this day and hasn't solved it yet. */
    public static boolean isOpen(Progress p, long day) {
        return p.archiveOpenDay == day;
    }

    public static boolean canOpen(Progress p, long day, long today) {
        return isOnShow(day, today) && !isSolved(p, day) && (isOpen(p, day) || p.coins >= OPEN_COINS);
    }

    /** Pays to play a missed day. Quitting keeps it open, so a phone call doesn't cost the coins twice. */
    public static boolean open(Progress p, long day, long today) {
        if (!canOpen(p, day, today)) return false;
        if (isOpen(p, day)) return true;
        p.coins -= OPEN_COINS;
        p.archiveOpenDay = day;
        return true;
    }

    /** Records a solved day and pays for it, the first time only. The caller adds the returned coins to the balance. */
    public static int solve(Progress p, long day) {
        if (isSolved(p, day)) return 0;
        markSolved(p, day);
        if (isOpen(p, day)) p.archiveOpenDay = 0;
        return SOLVE_COINS;
    }
}
