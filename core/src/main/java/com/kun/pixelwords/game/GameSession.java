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

    public enum Result { FOUND_GRID, FOUND_BONUS, ALREADY_FOUND, NOT_IN_PUZZLE, NOT_A_WORD, TOO_SHORT }

    private final Level level;
    private final Set<String> foundGrid = new LinkedHashSet<>();
    private final Set<String> foundBonus = new LinkedHashSet<>();
    private final boolean[][] revealed;
    private final Random hintRandom;
    private int gridPoints;
    private int bonusPoints;
    private int hintsUsed;
    private int coinsEarned;

    public GameSession(Level level, long hintSeed) {
        this.level = level;
        this.revealed = new boolean[level.rows][level.cols];
        this.hintRandom = new Random(hintSeed);
    }

    public Level level() { return level; }
    public Set<String> foundGridWords() { return foundGrid; }
    public Set<String> foundBonusWords() { return foundBonus; }
    public int gridPoints() { return gridPoints; }
    public int bonusPoints() { return bonusPoints; }
    public int hintsUsed() { return hintsUsed; }
    public int coinsEarned() { return coinsEarned; }
    public boolean isRevealed(int row, int col) { return revealed[row][col]; }
    public boolean isComplete() { return foundGrid.size() == level.placements.size(); }

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
            markFound(p);
            return Result.FOUND_GRID;
        }
        if (level.bonusWords.contains(word)) {
            if (!foundBonus.add(word)) return Result.ALREADY_FOUND;
            bonusPoints += Scoring.bonusWordPoints(word);
            coinsEarned += Scoring.COINS_PER_BONUS_WORD;
            return Result.FOUND_BONUS;
        }
        return level.extraWords.contains(word) ? Result.NOT_IN_PUZZLE : Result.NOT_A_WORD;
    }

    /**
     * Reveals one hidden cell of a word the player has not found yet. Returns {row, col}, or null if
     * nothing is left to reveal. If the cell completes a word, that word counts as found.
     */
    public int[] revealHint() {
        List<int[]> candidates = new ArrayList<>();
        for (Level.Placement p : level.placements) {
            if (foundGrid.contains(p.word)) continue;
            for (int i = 0; i < p.word.length(); i++) {
                int r = p.rowAt(i), c = p.colAt(i);
                if (!revealed[r][c]) candidates.add(new int[]{r, c});
            }
        }
        if (candidates.isEmpty()) return null;
        int[] cell = candidates.get(hintRandom.nextInt(candidates.size()));
        revealed[cell[0]][cell[1]] = true;
        hintsUsed++;
        for (Level.Placement p : level.placements) {
            if (!foundGrid.contains(p.word) && fullyRevealed(p)) markFound(p);
        }
        return cell;
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
