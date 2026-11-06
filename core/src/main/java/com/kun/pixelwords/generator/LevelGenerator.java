package com.kun.pixelwords.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministic level factory. The same (kind, number/date/seed) always produces the same level on
 * every device, so cloud save only needs to store a level number, daily puzzles need no server, and
 * both players in a word duel get an identical board from a shared seed.
 */
public final class LevelGenerator {

    /** Bump if the algorithm ever changes in a way that alters output, so old saves can be handled. */
    public static final int ALGORITHM_VERSION = 2;

    private static final long PROGRESSION_SALT = 0x5057_4F52_4453_0001L;
    private static final long DAILY_SALT = 0x5057_4441_494C_0002L;
    private static final long HARD_SALT = 0x5057_4841_5244_0003L;
    private static final int MAX_WHEEL_LETTERS = 8;
    /** Few wheels spell more long common words than this, and chasing more wastes every generation attempt. */
    private static final int MAX_HARD_WORDS = 9;

    public static final int MAX_ROWS = 10;
    public static final int MAX_COLS = 10;
    /** Most words in a level that can pay coins, however many the letters could spell. */
    public static final int MAX_BONUS = 5;

    /** How hard a generated level should be. */
    public static final class Spec {
        public final int letterCount;
        public final int targetWords;
        /** The grid may not grow beyond this; bigger levels get a roomier limit. */
        public final int maxGrid;
        /** Shortest word allowed in the grid. */
        public final int minWordLength;

        public Spec(int letterCount, int targetWords, int maxGrid) {
            this(letterCount, targetWords, maxGrid, Dictionary.MIN_LEN);
        }

        public Spec(int letterCount, int targetWords, int maxGrid, int minWordLength) {
            this.letterCount = letterCount;
            this.targetWords = targetWords;
            this.maxGrid = maxGrid;
            this.minWordLength = minWordLength;
        }

        /** Bonus words stay a rare treat: about one for every three words in the grid. */
        int bonusCap() {
            return Math.min(MAX_BONUS, 1 + targetWords / 3);
        }
    }

    private final Dictionary dict;
    /** Common words by length (index = length), alphabetical. */
    private final List<List<String>> commonByLength = new ArrayList<>();

    public LevelGenerator(Dictionary dict) {
        this.dict = dict;
        for (int i = 0; i <= Dictionary.MAX_LEN; i++) commonByLength.add(new ArrayList<String>());
        for (String w : dict.commonWords()) commonByLength.get(w.length()).add(w);
    }

    public Dictionary dictionary() {
        return dict;
    }

    // ---------------------------------------------------------------- public factories

    /** Main campaign. Levels 1..3 are hand-made, the rest are generated. */
    public Level forProgression(int levelNumber) {
        if (levelNumber < 1) throw new IllegalArgumentException("levelNumber must be >= 1");
        if (levelNumber <= FixedLevels.COUNT) return FixedLevels.get(levelNumber, dict);
        long seed = SeedUtil.mix(PROGRESSION_SALT, levelNumber);
        return generate("P" + levelNumber, Level.Kind.PROGRESSION, seed, specForLevel(levelNumber));
    }

    /** A hard version of a campaign level: a bigger wheel, more words, and no short easy ones. */
    public Level forHard(int levelNumber) {
        if (levelNumber <= FixedLevels.COUNT) throw new IllegalArgumentException("Opening levels are never hard");
        long seed = SeedUtil.mix(HARD_SALT, levelNumber);
        return generate("H" + levelNumber, Level.Kind.PROGRESSION, seed, specForHard(levelNumber));
    }

    /** Daily puzzle for a given day number (days since 1970-01-01; use LocalDate.toEpochDay()). Harder at weekends. */
    public Level forDaily(long epochDay) {
        int weekday = (int) Math.floorMod(epochDay + 3, 7L); // 0 = Monday (1970-01-01 was a Thursday)
        Spec spec;
        if (weekday <= 2) spec = new Spec(5, 7, 8);
        else if (weekday <= 4) spec = new Spec(6, 9, 9);
        else spec = new Spec(7, 11, 10);
        long seed = SeedUtil.mix(DAILY_SALT, epochDay);
        return generate("D" + epochDay, Level.Kind.DAILY, seed, spec);
    }

    /** Word-duel board. Both players call this with the same shared match seed. */
    public Level forDuel(long matchSeed) {
        return generate("M" + Long.toHexString(matchSeed), Level.Kind.DUEL, matchSeed, new Spec(6, 8, 9));
    }

    /**
     * Difficulty curve for generated campaign levels (levelNumber is 1-based, >= 4 in practice).
     * Word length, word count and grid size all climb slowly, one step at a time.
     */
    public static Spec specForLevel(int n) {
        if (n <= 15) return new Spec(4, 3 + (n - 4) / 4, 6);                 // 4 letters, 3..5 words, 6x6
        if (n <= 45) return new Spec(5, 4 + (n - 16) / 8, 7);                // 5 letters, 4..7 words, 7x7
        if (n <= 95) return new Spec(6, 6 + (n - 46) / 12, 8);               // 6 letters, 6..10 words, 8x8
        if (n <= 170) return new Spec(7, 8 + (n - 96) / 15, 9);              // 7 letters, 8..12 words, 9x9
        return new Spec(8, Math.min(16, 10 + (n - 171) / 25), 10);           // 8 letters, 10..16 words, 10x10
    }

    /** One step past the normal curve at this level, so a hard level is tough but never out of reach. */
    public static Spec specForHard(int n) {
        Spec base = specForLevel(n);
        int letters = Math.min(MAX_WHEEL_LETTERS, base.letterCount + 1);
        int words = Math.min(base.targetWords + 2, MAX_HARD_WORDS);
        return new Spec(letters, words, Math.min(MAX_ROWS, base.maxGrid + 1), letters >= 7 ? 5 : 4);
    }

    // ---------------------------------------------------------------- core

    public Level generate(String id, Level.Kind kind, long seed, Spec spec) {
        Random rnd = new Random(seed);
        List<String> pool = commonByLength.get(spec.letterCount);
        if (pool.isEmpty()) throw new IllegalStateException("No common words of length " + spec.letterCount);

        Level bestSoFar = null;
        int minWords = Math.max(3, spec.targetWords - 1);

        for (int attempt = 0; attempt < 400; attempt++) {
            String seedWord = pool.get(rnd.nextInt(pool.size()));
            List<String> subs = dict.subWords(seedWord);

            List<String> commonSubs = new ArrayList<>();
            for (String w : subs) if (dict.isCommon(w) && w.length() >= spec.minWordLength) commonSubs.add(w);
            if (commonSubs.size() < minWords) continue;

            Level candidate = layout(id, kind, seedWord, subs, commonSubs, spec, rnd);
            if (candidate == null) continue;
            if (candidate.placements.size() >= minWords) return candidate;
            if (bestSoFar == null || candidate.placements.size() > bestSoFar.placements.size()) {
                bestSoFar = candidate;
            }
        }
        if (bestSoFar != null) return bestSoFar;
        throw new IllegalStateException("Could not generate a level for seed " + seed);
    }

    private Level layout(String id, Level.Kind kind, String seedWord, List<String> subs,
                         List<String> commonSubs, Spec spec, Random rnd) {
        // Longest words first; shuffle within each length so levels vary but stay deterministic.
        List<String> order = new ArrayList<>();
        for (int len = Dictionary.MAX_LEN; len >= Dictionary.MIN_LEN; len--) {
            List<String> group = new ArrayList<>();
            for (String w : commonSubs) if (w.length() == len) group.add(w);
            Collections.shuffle(group, rnd);
            order.addAll(group);
        }

        int target = spec.targetWords;
        CrosswordBuilder builder = new CrosswordBuilder(Math.min(spec.maxGrid, MAX_ROWS), Math.min(spec.maxGrid, MAX_COLS));
        List<String> placedWords = new ArrayList<>();
        for (String w : order) {
            if (builder.placedCount() >= target) break;
            if (builder.tryPlace(w, rnd)) placedWords.add(w);
        }
        if (builder.placedCount() < 2) return null;

        List<String> others = new ArrayList<>(subs);
        others.removeAll(placedWords);
        List<String> bonus = new ArrayList<>(), extra = new ArrayList<>();
        splitBonus(dict, others, spec.bonusCap(), rnd, bonus, extra);

        return new Level(id, kind, shuffledLetters(seedWord, rnd),
                builder.rows(), builder.cols(), builder.normalizedPlacements(), bonus, extra);
    }

    /** Picks up to {@code cap} common words as paying bonus words; everything else goes to {@code extra}. */
    static void splitBonus(Dictionary dict, List<String> others, int cap, Random rnd,
                           List<String> bonus, List<String> extra) {
        List<String> candidates = new ArrayList<>();
        for (String w : others) if (dict.isCommon(w)) candidates.add(w);
        Collections.shuffle(candidates, rnd);
        for (int i = 0; i < candidates.size() && i < cap; i++) bonus.add(candidates.get(i));
        for (String w : others) if (!bonus.contains(w)) extra.add(w);
    }

    /** Wheel order: shuffled, but never identical to the answer word. */
    private static String shuffledLetters(String word, Random rnd) {
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) chars.add(word.charAt(i));
        String result = word;
        for (int tries = 0; tries < 20 && result.equals(word); tries++) {
            Collections.shuffle(chars, rnd);
            StringBuilder sb = new StringBuilder();
            for (char c : chars) sb.append(c);
            result = sb.toString();
        }
        return result;
    }
}
