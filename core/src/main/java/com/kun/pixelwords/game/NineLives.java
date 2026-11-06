package com.kun.pixelwords.game;

/** One run of Nine Lives. Runs aren't saved: leaving ends the run, and only the best score is kept. */
public final class NineLives {

    public static final int MAX_LIVES = 9;
    public static final int BOARDS_PER_LIFE = 5;
    public static final int CONTINUE_COINS = 150;
    public static final int CONTINUE_LIVES = 3;
    public static final int COINS_PER_BOARD = 5;
    /** Boards from this depth on carry tile twists. */
    public static final int FIRST_TWIST_DEPTH = 3;

    private int lives = MAX_LIVES;
    private int boards;
    private int score;
    private boolean continued;

    public int lives() { return lives; }
    public int boards() { return boards; }
    public int score() { return score; }

    public boolean isOver() {
        return lives <= 0;
    }

    /** Costs a life for a word that doesn't exist. */
    public void wrongWord() {
        lives = Math.max(0, lives - 1);
    }

    /** Banks a cleared board. Returns true if it won back a life. */
    public boolean boardCleared(int boardScore) {
        boards++;
        score += boardScore;
        if (boards % BOARDS_PER_LIFE != 0 || lives >= MAX_LIVES) return false;
        lives++;
        return true;
    }

    /** Adds a board's score without counting it as cleared, for the board the run ended on. */
    public void addScore(int boardScore) {
        score += boardScore;
    }

    public boolean canContinue(Progress p) {
        return isOver() && !continued && p.coins >= CONTINUE_COINS;
    }

    /** Pays to carry on. Once per run. */
    public boolean continueRun(Progress p) {
        if (!canContinue(p)) return false;
        p.coins -= CONTINUE_COINS;
        continued = true;
        lives = CONTINUE_LIVES;
        return true;
    }

    /** Records the run. Returns true if it beat the player's best. */
    public boolean finish(Progress p) {
        p.coins += boards * COINS_PER_BOARD;
        boolean record = score > p.bestLives;
        p.bestLives = Math.max(p.bestLives, score);
        return record;
    }
}
