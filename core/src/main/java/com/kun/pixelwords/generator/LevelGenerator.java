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
    private static final long MEGA_SALT = 0x5057_4D45_4741_0004L;
    private static final long LIVES_SALT = 0x5057_4C49_5645_0005L;
    private static final long TOWER_SALT = 0x5057_544F_5752_0006L;
    private static final long RUSH_SALT = 0x5057_5255_5348_0007L;
    /** The Word Tower's steps, two floors each: wheel letters, words, shortest word. The last repeats, growing. */
    private static final int[][] TOWER_STEPS = {
            {4, 3, 3}, {5, 4, 3}, {5, 3, 4}, {6, 4, 4}, {6, 3, 5}, {7, 4, 5}, {8, 3, 5}};
    private static final int TOWER_MAX_WORDS = 6;
    private static final int MAX_WHEEL_LETTERS = 8;
    /** Few wheels spell more long common words than this, and chasing more wastes every generation attempt. */
    /** Levels in a Journey chapter; matches Journey.LEVELS_PER_CHAPTER. */
    public static final int CHAPTER_LEVELS = 20;
    /**
     * Each chapter climbs from easy to hard. By slot (1 = first level, 20 = boss): wheel letters and
     * words in the grid. Three quick levels open it, then 6 letters, then 7, then 8 near the boss, with
     * the word count growing faster and faster.
     */
    private static final int[] CHAPTER_LETTERS = {4, 4, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8};
    private static final int[] CHAPTER_WORDS = {3, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 13, 13, 14};
    /** The first chapters stop short of the full climb, so new players aren't thrown at 8-letter boards. */
    private static final int[] FIRST_CHAPTER_MAX_LETTERS = {6, 7};
    private static final int[] FIRST_CHAPTER_MAX_WORDS = {8, 12};
    /** More than this from 8 letters can take a phone seconds to lay out, so hard levels stop here. */
    private static final int MAX_WORDS = 14;

    public static final int MAX_ROWS = 10;
    public static final int MAX_COLS = 10;
    /** Only weekend boards go past the usual limit, so no other level changes shape. */
    public static final int MEGA_MAX_GRID = 12;
    private static final int MEGA_WORDS = 18;
    private static final int LIVES_MAX_LETTERS = 8;
    private static final int LIVES_MAX_WORDS = 12;
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

    /** The weekend board for a week number: everyone gets the same one. */
    public Level forMega(long week) {
        long seed = SeedUtil.mix(MEGA_SALT, week);
        return generate("W" + week, Level.Kind.MEGA, seed, new Spec(MAX_WHEEL_LETTERS, MEGA_WORDS, MEGA_MAX_GRID));
    }

    /** A board in a Nine Lives run. {@code depth} is how many boards the run has cleared. */
    public Level forLives(long runSeed, int depth) {
        int letters = Math.min(LIVES_MAX_LETTERS, 4 + depth / 3);
        int words = Math.min(LIVES_MAX_WORDS, 3 + depth);
        int grid = Math.min(MAX_ROWS, 6 + depth / 3);
        long seed = SeedUtil.mix(SeedUtil.mix(LIVES_SALT, runSeed), depth);
        return generate("L" + depth, Level.Kind.LIVES, seed, new Spec(letters, words, grid));
    }

    /** A floor of the Word Tower. {@code floor} counts from 0. */
    public Level forTower(long runSeed, int floor) {
        long seed = SeedUtil.mix(SeedUtil.mix(TOWER_SALT, runSeed), floor);
        return generate("T" + floor, Level.Kind.TOWER, seed, specForTower(floor));
    }

    /** The words get longer every two floors, and the top step keeps adding words. */
    public static Spec specForTower(int floor) {
        int step = Math.max(0, floor) / 2;
        int[] s = TOWER_STEPS[Math.min(step, TOWER_STEPS.length - 1)];
        int extra = Math.max(0, step - (TOWER_STEPS.length - 1)) / 2;
        int letters = s[0];
        return new Spec(letters, Math.min(TOWER_MAX_WORDS, s[1] + extra), Math.min(MAX_ROWS, letters + 2), s[2]);
    }

    /** A board in Boss Rush: as hard as the boss's own level, but a fresh board each round. */
    public Level forRush(long runSeed, int round, int bossLevel) {
        long seed = SeedUtil.mix(SeedUtil.mix(RUSH_SALT, runSeed), round);
        return generate("R" + round, Level.Kind.RUSH, seed, specForHard(Math.max(FixedLevels.COUNT + 1, bossLevel)));
    }

    /**
     * Difficulty for a generated campaign level (levelNumber is 1-based, >= 4 in practice). It starts
     * easy at the beginning of every chapter and climbs steeply towards the boss; see CHAPTER_LETTERS.
     */
    public static Spec specForLevel(int n) {
        int chapter = (n - 1) / CHAPTER_LEVELS, slot = (n - 1) % CHAPTER_LEVELS;
        int letters = CHAPTER_LETTERS[slot], words = CHAPTER_WORDS[slot];
        if (chapter < FIRST_CHAPTER_MAX_LETTERS.length) {
            letters = Math.min(letters, FIRST_CHAPTER_MAX_LETTERS[chapter]);
            words = Math.min(words, FIRST_CHAPTER_MAX_WORDS[chapter]);
        }
        int grid = Math.min(MAX_ROWS, words >= 12 ? MAX_ROWS : letters + 2);
        return new Spec(letters, words, grid);
    }

    /** One step past the normal curve at this level, so a hard level is tough but never out of reach. */
    public static Spec specForHard(int n) {
        Spec base = specForLevel(n);
        int letters = Math.min(MAX_WHEEL_LETTERS, base.letterCount + 1);
        int words = Math.min(base.targetWords + 2, MAX_WORDS);
        // Long words only where the wheel can still make enough of them.
        int shortest = letters >= 7 && words <= 10 ? 5 : 4;
        return new Spec(letters, words, Math.min(MAX_ROWS, base.maxGrid + 1), shortest);
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
        int limit = kind == Level.Kind.MEGA ? MEGA_MAX_GRID : Math.min(MAX_ROWS, MAX_COLS);
        int size = Math.min(spec.maxGrid, limit);
        CrosswordBuilder builder = new CrosswordBuilder(size, size);
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
