package com.kun.pixelwords.game;

/**
 * Every bonus word drops into the jar, and a full jar opens for coins. Opens are limited each day so a
 * big wheel can't be swiped for endless coins. The jar stays full, losing nothing, until it can open.
 */
public final class PiggyJar {

    private PiggyJar() {}

    public static final int CAPACITY = 40;
    public static final int COINS = 100;
    public static final int MAX_OPENS_PER_DAY = 3;

    public static void add(Progress p, int words) {
        p.jarWords = Math.min(CAPACITY, p.jarWords + Math.max(0, words));
    }

    public static boolean isFull(Progress p) {
        return p.jarWords >= CAPACITY;
    }

    public static int opensLeft(Progress p, long today) {
        return p.jarDay == today ? Math.max(0, MAX_OPENS_PER_DAY - p.jarOpensToday) : MAX_OPENS_PER_DAY;
    }

    public static boolean canOpen(Progress p, long today) {
        return isFull(p) && opensLeft(p, today) > 0;
    }

    /** Empties a full jar into the player's coins. False if it isn't full or today's opens are used. */
    public static boolean open(Progress p, long today) {
        if (!canOpen(p, today)) return false;
        p.jarOpensToday = p.jarDay == today ? p.jarOpensToday + 1 : 1;
        p.jarDay = today;
        p.jarWords = 0;
        p.coins += COINS;
        return true;
    }
}
