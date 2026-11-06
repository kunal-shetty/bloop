package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The twists on a tougher board, worked out from the level alone so every device agrees and nothing is
 * saved. Frozen words never cross each other, so each one always has an unfrozen word that can thaw it
 * and no board can be left unsolvable.
 */
public final class TileMods {

    /** A board with no twists. */
    public static final TileMods NONE = new TileMods(new HashSet<>(), new HashSet<>(), -1);

    public static final int GOLD_COINS = 5;
    public static final int SPARK_COINS = 20;
    /** A spark word pays only if it's among this many of the first words found. */
    public static final int SPARK_WINDOW = 4;

    private static final long SALT = 0x5449_4C45_4D4F_4453L;
    private static final int WORDS_PER_FROZEN = 4;
    private static final int GOLD_CELLS = 3;

    private final Set<Integer> gold;
    private final Set<Integer> frozen;
    private final int spark;

    private TileMods(Set<Integer> gold, Set<Integer> frozen, int spark) {
        this.gold = gold;
        this.frozen = frozen;
        this.spark = spark;
    }

    public static TileMods forLevel(Level level) {
        Random rnd = new Random(level.fingerprint() ^ SALT);
        int words = level.placements.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < words; i++) order.add(i);
        Collections.shuffle(order, rnd);

        Set<Integer> frozen = new HashSet<>();
        int wanted = words / WORDS_PER_FROZEN;
        for (int word : order) {
            if (frozen.size() >= wanted) break;
            if (canFreeze(level, word, frozen)) frozen.add(word);
        }

        int spark = -1;
        for (int word : order) {
            if (!frozen.contains(word)) {
                spark = word;
                break;
            }
        }
        return new TileMods(pickGold(level, rnd), frozen, spark);
    }

    /** A word can freeze if it crosses something, and nothing it crosses is frozen. */
    private static boolean canFreeze(Level level, int word, Set<Integer> frozen) {
        boolean crossed = false;
        for (int other = 0; other < level.placements.size(); other++) {
            if (other == word || !crosses(level.placements.get(word), level.placements.get(other))) continue;
            if (frozen.contains(other)) return false;
            crossed = true;
        }
        return crossed;
    }

    public static boolean crosses(Level.Placement a, Level.Placement b) {
        for (int i = 0; i < a.word.length(); i++) {
            for (int j = 0; j < b.word.length(); j++) {
                if (a.rowAt(i) == b.rowAt(j) && a.colAt(i) == b.colAt(j)) return true;
            }
        }
        return false;
    }

    private static Set<Integer> pickGold(Level level, Random rnd) {
        List<Integer> cells = new ArrayList<>();
        for (Level.Placement p : level.placements) {
            for (int i = 0; i < p.word.length(); i++) {
                int cell = cellOf(level, p.rowAt(i), p.colAt(i));
                if (!cells.contains(cell)) cells.add(cell);
            }
        }
        Collections.sort(cells);
        Collections.shuffle(cells, rnd);
        return new HashSet<>(cells.subList(0, Math.min(GOLD_CELLS, cells.size())));
    }

    private static int cellOf(Level level, int row, int col) {
        return row * level.cols + col;
    }

    public boolean isGold(Level level, int row, int col) {
        return gold.contains(cellOf(level, row, col));
    }

    /** True if the placement at this index starts the round frozen. */
    public boolean isFrozen(int word) {
        return frozen.contains(word);
    }

    public int frozenCount() {
        return frozen.size();
    }

    /** Index of the spark word, or -1 if the board has none. */
    public int sparkWord() {
        return spark;
    }
}
