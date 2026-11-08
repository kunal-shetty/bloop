package com.kun.pixelwords.game;

import java.util.Random;

/**
 * Boss Rush: the bosses the player has already beaten, back to back, with one set of lives for the whole
 * run. They come in a shuffled order fixed by the run's seed; after the last one the order starts over.
 * A run is ranked by bosses beaten.
 */
public final class BossRush extends Run {

    /** Bosses to beat in the Journey before the rush opens. */
    public static final int BOSSES_TO_OPEN = 2;
    public static final int LIVES = 5;
    public static final int CONTINUE_COINS = 200;
    public static final int CONTINUE_LIVES = 2;
    public static final int COINS_PER_BOSS = 15;

    private final int[] order;

    /** A rush through every boss the player has beaten so far. */
    public BossRush(Progress p, long seed) {
        super(LIVES, 0, CONTINUE_COINS, CONTINUE_LIVES, COINS_PER_BOSS);
        order = new int[Math.max(1, Trophies.count(p))];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Random rnd = new Random(seed);
        for (int i = order.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1), t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
    }

    public static boolean isOpen(Progress p) {
        return Trophies.count(p) >= BOSSES_TO_OPEN;
    }

    /** The chapter whose boss is up next. */
    public int chapter() {
        return order[boards() % order.length];
    }

    /** How many different bosses are in the rush. */
    public int bosses() {
        return order.length;
    }

    @Override
    public int result() {
        return boards();
    }

    @Override
    public int best(Progress p) {
        return p.bestRush;
    }

    @Override
    protected void setBest(Progress p, int best) {
        p.bestRush = best;
    }
}
