package com.kun.pixelwords.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One puzzle: a wheel of letters, a crossword grid of target words, and a set of bonus words. */
public final class Level {

    public enum Kind { FIXED, PROGRESSION, DAILY, DUEL }

    /** A word placed in the grid. Row/col are 0-based from the top-left of the grid. */
    public static final class Placement {
        public final String word;
        public final int row;
        public final int col;
        public final boolean horizontal;

        public Placement(String word, int row, int col, boolean horizontal) {
            this.word = word;
            this.row = row;
            this.col = col;
            this.horizontal = horizontal;
        }

        public int rowAt(int i) {
            return horizontal ? row : row + i;
        }

        public int colAt(int i) {
            return horizontal ? col + i : col;
        }

        @Override
        public String toString() {
            return word + "@" + row + "," + col + (horizontal ? "H" : "V");
        }
    }

    public final String id;
    public final Kind kind;
    /** Letters in wheel order (already shuffled), lower-case. */
    public final String letters;
    public final int rows;
    public final int cols;
    public final List<Placement> placements;
    /** A small handful of common words outside the grid; finding one earns coins. */
    public final List<String> bonusWords;
    /** Every other real word the letters can spell: accepted as a real word, but worth nothing. */
    public final List<String> extraWords;

    public Level(String id, Kind kind, String letters, int rows, int cols,
                 List<Placement> placements, List<String> bonusWords, List<String> extraWords) {
        this.id = id;
        this.kind = kind;
        this.letters = letters;
        this.rows = rows;
        this.cols = cols;
        this.placements = Collections.unmodifiableList(new ArrayList<>(placements));
        this.bonusWords = Collections.unmodifiableList(new ArrayList<>(bonusWords));
        this.extraWords = Collections.unmodifiableList(new ArrayList<>(extraWords));
    }

    /** Grid as characters; '\0' marks an empty cell. */
    public char[][] grid() {
        char[][] g = new char[rows][cols];
        for (Placement p : placements) {
            for (int i = 0; i < p.word.length(); i++) {
                g[p.rowAt(i)][p.colAt(i)] = p.word.charAt(i);
            }
        }
        return g;
    }

    /** Stable 64-bit hash of everything that defines the level, for cross-platform determinism checks. */
    public long fingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append('|').append(letters).append('|').append(rows).append('x').append(cols);
        for (Placement p : placements) sb.append('|').append(p);
        sb.append('|').append(bonusWords.size());
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < sb.length(); i++) {
            h ^= sb.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
