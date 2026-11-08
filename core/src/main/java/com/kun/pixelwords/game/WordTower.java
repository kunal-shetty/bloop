package com.kun.pixelwords.game;

/**
 * One climb of the Word Tower. Each floor is a small board, and the words get longer as the tower rises
 * (see LevelGenerator.specForTower). Three slips and the climb is over; a run is ranked by floors climbed.
 */
public final class WordTower extends Run {

    public static final int LIVES = 3;
    public static final int CONTINUE_COINS = 200;
    public static final int CONTINUE_LIVES = 1;
    public static final int COINS_PER_FLOOR = 8;

    public WordTower() {
        super(LIVES, 0, CONTINUE_COINS, CONTINUE_LIVES, COINS_PER_FLOOR);
    }

    /** The floor being played, from 1. */
    public int floor() {
        return boards() + 1;
    }

    @Override
    public int result() {
        return boards();
    }

    @Override
    public int best(Progress p) {
        return p.bestTower;
    }

    @Override
    protected void setBest(Progress p, int best) {
        p.bestTower = best;
    }
}
