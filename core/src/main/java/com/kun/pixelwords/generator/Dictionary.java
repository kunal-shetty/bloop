package com.kun.pixelwords.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Word dictionary used for validating guesses and generating levels.
 *
 * <ul>
 *   <li><b>valid</b>: every accepted word (3-7 letters, a-z only, not offensive). Anything in here
 *       counts as a bonus word if the player finds it.</li>
 *   <li><b>common</b>: a subset of valid words that people actually know. Only these are placed in
 *       the crossword grid so players are not forced to find obscure Scrabble words.</li>
 * </ul>
 *
 * All words are stored lower-case. All lists are sorted so level generation is deterministic.
 */
public final class Dictionary {

    public static final int MIN_LEN = 3;
    public static final int MAX_LEN = 8;

    private final Set<String> valid;
    private final List<String> validSorted;
    private final Set<String> common;
    private final List<String> commonSorted;

    private Dictionary(TreeSet<String> validWords, List<String> commonWords) {
        this.validSorted = Collections.unmodifiableList(new ArrayList<>(validWords));
        this.valid = new HashSet<>(validWords);
        TreeSet<String> c = new TreeSet<>(commonWords);
        this.commonSorted = Collections.unmodifiableList(new ArrayList<>(c));
        this.common = new HashSet<>(c);
    }

    /**
     * @param words  full word list, one word per line (e.g. ENABLE)
     * @param common common-words list, one per line; entries not in {@code words} are dropped
     * @param bad    offensive terms, one per line (may be null)
     */
    public static Dictionary load(InputStream words, InputStream common, InputStream bad) throws IOException {
        BadWordFilter filter = bad == null ? BadWordFilter.empty() : new BadWordFilter(readLines(bad));

        TreeSet<String> validWords = new TreeSet<>();
        for (String line : readLines(words)) {
            String w = line.trim().toLowerCase(Locale.ROOT);
            if (isPlayable(w) && !filter.isBlocked(w)) validWords.add(w);
        }

        LinkedHashSet<String> commonWords = new LinkedHashSet<>();
        if (common != null) {
            for (String line : readLines(common)) {
                String w = line.trim().toLowerCase(Locale.ROOT);
                if (isPlayable(w) && validWords.contains(w) && !filter.isBlocked(w)) commonWords.add(w);
            }
        }
        return new Dictionary(validWords, new ArrayList<>(commonWords));
    }

    static boolean isPlayable(String w) {
        int n = w.length();
        if (n < MIN_LEN || n > MAX_LEN) return false;
        for (int i = 0; i < n; i++) {
            char c = w.charAt(i);
            if (c < 'a' || c > 'z') return false;
        }
        return true;
    }

    public boolean isWord(String w) {
        return valid.contains(w.toLowerCase(Locale.ROOT));
    }

    public boolean isCommon(String w) {
        return common.contains(w.toLowerCase(Locale.ROOT));
    }

    /** All valid words, alphabetically. */
    public List<String> validWords() {
        return validSorted;
    }

    /** All common words, alphabetically. */
    public List<String> commonWords() {
        return commonSorted;
    }

    /** Every valid word (length >= 3) that can be spelled using each given letter at most as often as it appears. */
    public List<String> subWords(String letters) {
        int[] counts = letterCounts(letters);
        int max = letters.length();
        List<String> out = new ArrayList<>();
        for (String w : validSorted) {
            if (w.length() <= max && canBuild(w, counts)) out.add(w);
        }
        return out;
    }

    public static int[] letterCounts(String letters) {
        int[] counts = new int[26];
        for (int i = 0; i < letters.length(); i++) {
            char c = Character.toLowerCase(letters.charAt(i));
            if (c >= 'a' && c <= 'z') counts[c - 'a']++;
        }
        return counts;
    }

    public static boolean canBuild(String word, int[] counts) {
        int[] used = new int[26];
        for (int i = 0; i < word.length(); i++) {
            int k = word.charAt(i) - 'a';
            if (k < 0 || k >= 26 || ++used[k] > counts[k]) return false;
        }
        return true;
    }

    private static List<String> readLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }
        return lines;
    }
}
