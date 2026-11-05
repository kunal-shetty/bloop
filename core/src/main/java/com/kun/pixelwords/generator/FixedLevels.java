package com.kun.pixelwords.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-designed opening levels, drawn as ASCII grids ('.' = empty). Words are read straight off the
 * drawing (every horizontal or vertical run of 2+ letters), so what you see is exactly what the
 * player gets. The self-test verifies each word is in the dictionary and buildable from the wheel.
 */
final class FixedLevels {

    private FixedLevels() {}

    static final int COUNT = 3;

    static Level get(int levelNumber, Dictionary dict) {
        switch (levelNumber) {
            case 1:
                return parse("P1", "act", dict,
                        "a..",
                        "cat",
                        "t..");
            case 2:
                return parse("P2", "tsar", dict,
                        "star",
                        "a..a",
                        "t..t");
            case 3:
                return parse("P3", "plate", dict,
                        "l...t",
                        "e...a",
                        "a.p.p",
                        "plate",
                        "..l..");
            default:
                throw new IllegalArgumentException("No fixed level " + levelNumber);
        }
    }

    static Level parse(String id, String letters, Dictionary dict, String... art) {
        int rows = art.length;
        int cols = 0;
        for (String line : art) cols = Math.max(cols, line.length());

        char[][] g = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = c < art[r].length() ? art[r].charAt(c) : '.';
                g[r][c] = ch == '.' ? '\0' : ch;
            }
        }

        List<Level.Placement> placements = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                if (g[r][c] == '\0') { c++; continue; }
                int start = c;
                StringBuilder sb = new StringBuilder();
                while (c < cols && g[r][c] != '\0') sb.append(g[r][c++]);
                if (sb.length() >= 2) placements.add(new Level.Placement(sb.toString(), r, start, true));
            }
        }
        for (int c = 0; c < cols; c++) {
            int r = 0;
            while (r < rows) {
                if (g[r][c] == '\0') { r++; continue; }
                int start = r;
                StringBuilder sb = new StringBuilder();
                while (r < rows && g[r][c] != '\0') sb.append(g[r++][c]);
                if (sb.length() >= 2) placements.add(new Level.Placement(sb.toString(), start, c, false));
            }
        }

        List<String> others = new ArrayList<>(dict.subWords(letters));
        for (Level.Placement p : placements) others.remove(p.word);
        List<String> bonus = new ArrayList<>(), extra = new ArrayList<>();
        LevelGenerator.splitBonus(dict, others, 1, new java.util.Random(id.hashCode()), bonus, extra);
        return new Level(id, Level.Kind.FIXED, letters, rows, cols, placements, bonus, extra);
    }
}
