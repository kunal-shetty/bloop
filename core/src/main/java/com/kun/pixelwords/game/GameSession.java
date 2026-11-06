package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Pure game rules for one play of one level. No Android dependencies, fully unit-testable. */
public final class GameSession {

    public enum Result { FOUND_GRID, FOUND_BONUS, ALREADY_FOUND, FROZEN, NOT_A_WORD, TOO_SHORT }

    private final Level level;
    private final TileMods mods;
    private final Set<String> foundGrid = new LinkedHashSet<>();
    private final Set<String> foundBonus = new LinkedHashSet<>();
    private final boolean[][] revealed;
    private final Random hintRandom;
    private int gridPoints;
    private int bonusPoints;
    private int hintsUsed;
    private int wordsSubmitted;
    private boolean sparkWon;

    public GameSession(Level level, long hintSeed) {
        this(level, hintSeed, TileMods.NONE);
    }

    public GameSession(Level level, long hintSeed, TileMods mods) {
        this.level = level;
        this.mods = mods;
        this.revealed = new boolean[level.rows][level.cols];
        this.hintRandom = new Random(hintSeed);
    }

    public Level level() { return level; }
    public TileMods mods() { return mods; }
    public Set<String> foundGridWords() { return foundGrid; }
    public Set<String> foundBonusWords() { return foundBonus; }
    public int gridPoints() { return gridPoints; }
    public int bonusPoints() { return bonusPoints; }
    public int hintsUsed() { return hintsUsed; }
    public boolean isRevealed(int row, int col) { return revealed[row][col]; }
    public boolean isComplete() { return foundGrid.size() == level.placements.size(); }

    /** Coins from the board's twists: revealed gold letters and a spark word found in time. */
    public int coinsEarned() {
        int gold = 0;
        for (int r = 0; r < level.rows; r++) {
            for (int c = 0; c < level.cols; c++) {
                if (revealed[r][c] && mods.isGold(level, r, c)) gold++;
            }
        }
        return gold * TileMods.GOLD_COINS + (sparkWon ? TileMods.SPARK_COINS : 0);
    }

    /** True while finding the spark word would still pay. */
    public boolean sparkLive() {
        return mods.sparkWord() >= 0 && !sparkWon && wordsSubmitted < TileMods.SPARK_WINDOW
                && !foundGrid.contains(level.placements.get(mods.sparkWord()).word);
    }

    public boolean sparkWon() {
        return sparkWon;
    }

    /** True if this placement is still frozen: none of its letters have been revealed yet. */
    public boolean isFrozen(int placement) {
        if (!mods.isFrozen(placement)) return false;
        Level.Placement p = level.placements.get(placement);
        for (int i = 0; i < p.word.length(); i++) {
            if (revealed[p.rowAt(i)][p.colAt(i)]) return false;
        }
        return true;
    }

    /** The found grid words as a bit per placement, for keeping a big board in the save. */
    public long foundMask() {
        long mask = 0;
        for (int i = 0; i < level.placements.size(); i++) {
            if (foundGrid.contains(level.placements.get(i).word)) mask |= 1L << i;
        }
        return mask;
    }

    /** Puts back grid words from {@link #foundMask()}. */
    public void restore(long mask) {
        for (int i = 0; i < level.placements.size(); i++) {
            Level.Placement p = level.placements.get(i);
            if ((mask & 1L << i) != 0 && !foundGrid.contains(p.word)) markFound(p);
        }
    }

    public int score(int secondsLeft) {
        return Scoring.levelScore(gridPoints, bonusPoints, secondsLeft, hintsUsed);
    }

    /** Called when the player lifts their finger off the wheel with a word. */
    public Result submit(String raw) {
        String word = raw.toLowerCase(Locale.ROOT);
        if (word.length() < 3) return Result.TOO_SHORT;

        Level.Placement p = findPlacement(word);
        if (p != null) {
            if (foundGrid.contains(word)) return Result.ALREADY_FOUND;
            if (isFrozen(level.placements.indexOf(p))) return Result.FROZEN;
            wordsSubmitted++;
            if (level.placements.indexOf(p) == mods.sparkWord() && wordsSubmitted <= TileMods.SPARK_WINDOW) sparkWon = true;
            markFound(p);
            return Result.FOUND_GRID;
        }
        boolean picked = level.bonusWords.contains(word);
        if (!picked && !level.extraWords.contains(word)) return Result.NOT_A_WORD;
        if (!foundBonus.add(word)) return Result.ALREADY_FOUND;
        wordsSubmitted++;
        // Only the level's picked bonus words score, so leaderboards and duels stay comparable.
        if (picked) bonusPoints += Scoring.bonusWordPoints(word);
        return Result.FOUND_BONUS;
    }

    /**
     * Reveals one hidden cell of a word the player has not found yet. Returns {row, col}, or null if
     * nothing is left to reveal. If the cell completes a word, that word counts as found.
     */
    public int[] revealHint() {
        List<int[]> candidates = hiddenCells();
        if (candidates.isEmpty()) return null;
        int[] cell = candidates.get(hintRandom.nextInt(candidates.size()));
        reveal(cell[0], cell[1]);
        hintsUsed++;
        return cell;
    }

    /** Reveals a cell the player chose. False if it's empty or already showing. */
    public boolean revealCell(int row, int col) {
        for (int[] cell : hiddenCells()) {
            if (cell[0] == row && cell[1] == col) {
                reveal(row, col);
                hintsUsed += PowerUps.Kind.PICK_LETTER.hintCost;
                return true;
            }
        }
        return false;
    }

    /** Fills in a whole unfound word and returns it, or null if none is left. */
    public Level.Placement revealWord() {
        List<Level.Placement> left = new ArrayList<>();
        for (Level.Placement p : level.placements) if (!foundGrid.contains(p.word)) left.add(p);
        if (left.isEmpty()) return null;
        Level.Placement p = left.get(hintRandom.nextInt(left.size()));
        markFound(p);
        hintsUsed += PowerUps.Kind.REVEAL_WORD.hintCost;
        return p;
    }

    private List<int[]> hiddenCells() {
        List<int[]> candidates = new ArrayList<>();
        for (Level.Placement p : level.placements) {
            if (foundGrid.contains(p.word)) continue;
            for (int i = 0; i < p.word.length(); i++) {
                int r = p.rowAt(i), c = p.colAt(i);
                if (!revealed[r][c]) candidates.add(new int[]{r, c});
            }
        }
        return candidates;
    }

    private void reveal(int row, int col) {
        revealed[row][col] = true;
        for (Level.Placement p : level.placements) {
            if (!foundGrid.contains(p.word) && fullyRevealed(p)) markFound(p);
        }
    }

    /** Placements that are now found, for the UI to render. */
    public List<Level.Placement> foundPlacements() {
        List<Level.Placement> out = new ArrayList<>();
        for (Level.Placement p : level.placements) if (foundGrid.contains(p.word)) out.add(p);
        return out;
    }

    private void markFound(Level.Placement p) {
        foundGrid.add(p.word);
        for (int i = 0; i < p.word.length(); i++) revealed[p.rowAt(i)][p.colAt(i)] = true;
        gridPoints += Scoring.gridWordPoints(p.word);
        // Crossing words may have been completed by letters revealed through this word.
        for (Level.Placement q : level.placements) {
            if (!foundGrid.contains(q.word) && fullyRevealed(q)) markFound(q);
        }
    }

    private boolean fullyRevealed(Level.Placement p) {
        for (int i = 0; i < p.word.length(); i++) {
            if (!revealed[p.rowAt(i)][p.colAt(i)]) return false;
        }
        return true;
    }

    private Level.Placement findPlacement(String word) {
        for (Level.Placement p : level.placements) if (p.word.equals(word)) return p;
        return null;
    }
}
