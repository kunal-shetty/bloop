package com.kun.pixelwords.game;

/**
 * One go at a survival mode: boards one after another until the lives run out. Runs aren't saved: leaving
 * ends the run, and only the best result is kept. Each mode sets its own numbers and says which result
 * counts as its best.
 */
public abstract class Run {

    public final int maxLives;
    /** A life comes back every this many boards, or never if zero. */
    public final int boardsPerLife;
    public final int continueCoins;
    public final int continueLives;
    public final int coinsPerBoard;

    private int lives;
    private int boards;
    private int score;
    private boolean continued;

    protected Run(int maxLives, int boardsPerLife, int continueCoins, int continueLives, int coinsPerBoard) {
        this.maxLives = maxLives;
        this.boardsPerLife = boardsPerLife;
        this.continueCoins = continueCoins;
        this.continueLives = continueLives;
        this.coinsPerBoard = coinsPerBoard;
        lives = maxLives;
    }

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
        if (boardsPerLife == 0 || boards % boardsPerLife != 0 || lives >= maxLives) return false;
        lives++;
        return true;
    }

    /** Adds a board's score without counting it as cleared, for the board the run ended on. */
    public void addScore(int boardScore) {
        score += boardScore;
    }

    public boolean canContinue(Progress p) {
        return isOver() && !continued && p.coins >= continueCoins;
    }

    /** Pays to carry on. Once per run. */
    public boolean continueRun(Progress p) {
        if (!canContinue(p)) return false;
        p.coins -= continueCoins;
        continued = true;
        lives = continueLives;
        return true;
    }

    public int coinsEarned() {
        return boards * coinsPerBoard;
    }

    /** What this mode ranks a run by. */
    public abstract int result();

    /** The player's best {@link #result()} so far. */
    public abstract int best(Progress p);

    protected abstract void setBest(Progress p, int best);

    /** Records the run. Returns true if it beat the player's best. */
    public boolean finish(Progress p) {
        p.coins += coinsEarned();
        boolean record = result() > best(p);
        if (record) setBest(p, result());
        return record;
    }
}
