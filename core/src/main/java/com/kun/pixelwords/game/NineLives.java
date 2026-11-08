package com.kun.pixelwords.game;

/** One run of Nine Lives: nine lives, one back every five boards, ranked by score. */
public final class NineLives extends Run {

    public static final int MAX_LIVES = 9;
    public static final int BOARDS_PER_LIFE = 5;
    public static final int CONTINUE_COINS = 150;
    public static final int CONTINUE_LIVES = 3;
    public static final int COINS_PER_BOARD = 5;
    /** Boards from this depth on carry tile twists. */
    public static final int FIRST_TWIST_DEPTH = 3;

    public NineLives() {
        super(MAX_LIVES, BOARDS_PER_LIFE, CONTINUE_COINS, CONTINUE_LIVES, COINS_PER_BOARD);
    }

    @Override
    public int result() {
        return score();
    }

    @Override
    public int best(Progress p) {
        return p.bestLives;
    }

    @Override
    protected void setBest(Progress p, int best) {
        p.bestLives = best;
    }
}
