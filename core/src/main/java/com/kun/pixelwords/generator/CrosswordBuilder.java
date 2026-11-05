package com.kun.pixelwords.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Greedy, seeded crossword layout.
 *
 * Words are added one at a time. A word may only be placed if it crosses an existing word at a
 * shared letter, never runs alongside another word (which would create accidental two-letter
 * words), and stays inside the size limits. Because every placement obeys those rules, the finished
 * grid contains exactly the placed words and nothing else, and is always fully connected.
 *
 * Only deterministic collections (ArrayList iteration) are used where order matters, so the same
 * seed yields the same layout on the JVM and on Android's ART.
 */
final class CrosswordBuilder {

    private final int maxRows;
    private final int maxCols;
    private final Map<Long, Character> cells = new HashMap<>();
    private final Set<Long> usedH = new HashSet<>();
    private final Set<Long> usedV = new HashSet<>();
    private final List<Level.Placement> placed = new ArrayList<>();
    private int minR, maxR, minC, maxC;

    CrosswordBuilder(int maxRows, int maxCols) {
        this.maxRows = maxRows;
        this.maxCols = maxCols;
    }

    int placedCount() {
        return placed.size();
    }

    /** Attempts to add a word. Returns true if it was placed. */
    boolean tryPlace(String word, Random rnd) {
        if (placed.isEmpty()) {
            if (word.length() > maxCols) return false;
            commit(new Level.Placement(word, 0, 0, true));
            return true;
        }

        Level.Placement best = null;
        long bestScore = Long.MIN_VALUE;

        for (Level.Placement host : placed) {
            for (int h = 0; h < host.word.length(); h++) {
                int r = host.rowAt(h);
                int c = host.colAt(h);
                long key = pack(r, c);
                boolean inH = usedH.contains(key);
                boolean inV = usedV.contains(key);
                if (inH && inV) continue; // already a crossing cell
                boolean newHorizontal = inV; // cross perpendicular to the word that owns this cell

                char ch = cells.get(key);
                for (int i = 0; i < word.length(); i++) {
                    if (word.charAt(i) != ch) continue;
                    int r0 = newHorizontal ? r : r - i;
                    int c0 = newHorizontal ? c - i : c;
                    int crossings = validCrossings(word, r0, c0, newHorizontal);
                    if (crossings < 1) continue;

                    int nMinR = Math.min(minR, newHorizontal ? r0 : r0);
                    int nMaxR = Math.max(maxR, newHorizontal ? r0 : r0 + word.length() - 1);
                    int nMinC = Math.min(minC, c0);
                    int nMaxC = Math.max(maxC, newHorizontal ? c0 + word.length() - 1 : c0);
                    int rowsAfter = nMaxR - nMinR + 1;
                    int colsAfter = nMaxC - nMinC + 1;
                    if (rowsAfter > maxRows || colsAfter > maxCols) continue;

                    long score = crossings * 1000L - (long) rowsAfter * colsAfter * 10L + rnd.nextInt(10);
                    if (score > bestScore) {
                        bestScore = score;
                        best = new Level.Placement(word, r0, c0, newHorizontal);
                    }
                }
            }
        }

        if (best == null) return false;
        commit(best);
        return true;
    }

    /** Placements normalised so the top-left of the bounding box is (0,0). */
    List<Level.Placement> normalizedPlacements() {
        List<Level.Placement> out = new ArrayList<>();
        for (Level.Placement p : placed) {
            out.add(new Level.Placement(p.word, p.row - minR, p.col - minC, p.horizontal));
        }
        return out;
    }

    int rows() {
        return placed.isEmpty() ? 0 : maxR - minR + 1;
    }

    int cols() {
        return placed.isEmpty() ? 0 : maxC - minC + 1;
    }

    /** Returns the number of crossings if placement is legal, or 0 if it is not. */
    private int validCrossings(String word, int r0, int c0, boolean horizontal) {
        int dr = horizontal ? 0 : 1;
        int dc = horizontal ? 1 : 0;
        int len = word.length();

        if (cells.containsKey(pack(r0 - dr, c0 - dc))) return 0;
        if (cells.containsKey(pack(r0 + dr * len, c0 + dc * len))) return 0;

        int crossings = 0;
        for (int k = 0; k < len; k++) {
            int r = r0 + dr * k;
            int c = c0 + dc * k;
            long key = pack(r, c);
            Character existing = cells.get(key);
            if (existing != null) {
                if (existing != word.charAt(k)) return 0;
                if (horizontal ? usedH.contains(key) : usedV.contains(key)) return 0;
                crossings++;
            } else {
                // A fresh cell must not touch anything sideways, or it would form an accidental word.
                if (cells.containsKey(pack(r + dc, c + dr))) return 0;
                if (cells.containsKey(pack(r - dc, c - dr))) return 0;
            }
        }
        return crossings;
    }

    private void commit(Level.Placement p) {
        for (int i = 0; i < p.word.length(); i++) {
            int r = p.rowAt(i);
            int c = p.colAt(i);
            long key = pack(r, c);
            cells.put(key, p.word.charAt(i));
            (p.horizontal ? usedH : usedV).add(key);
        }
        if (placed.isEmpty()) {
            minR = p.row;
            maxR = p.rowAt(p.word.length() - 1);
            minC = p.col;
            maxC = p.colAt(p.word.length() - 1);
        } else {
            minR = Math.min(minR, p.row);
            maxR = Math.max(maxR, p.rowAt(p.word.length() - 1));
            minC = Math.min(minC, p.col);
            maxC = Math.max(maxC, p.colAt(p.word.length() - 1));
        }
        placed.add(p);
    }

    private static long pack(int r, int c) {
        return ((long) r << 32) ^ (c & 0xffffffffL);
    }
}
