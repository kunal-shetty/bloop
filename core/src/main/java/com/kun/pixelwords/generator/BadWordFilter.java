package com.kun.pixelwords.generator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Blocks offensive words from ever entering the game dictionary.
 *
 * Matching is exact (plus a few common inflections), NOT substring based, so innocent words such as
 * "class" or "analysis" are not blocked just because they contain a short bad fragment
 * (the classic "Scunthorpe problem"). Multi-word phrases and entries containing digits or
 * punctuation in the source list are ignored because the game only deals in single a-z words.
 */
public final class BadWordFilter {

    private static final String[] SUFFIXES = {"", "s", "es", "ed", "er", "ers", "ing", "y", "ies"};

    private final Set<String> blocked = new HashSet<>();

    public BadWordFilter(Collection<String> terms) {
        if (terms == null) return;
        for (String raw : terms) {
            if (raw == null) continue;
            String t = raw.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty() || !t.matches("[a-z]+")) continue;
            for (String suffix : SUFFIXES) {
                blocked.add(t + suffix);
            }
        }
    }

    public static BadWordFilter empty() {
        return new BadWordFilter(null);
    }

    public boolean isBlocked(String word) {
        return blocked.contains(word.toLowerCase(Locale.ROOT));
    }

    public int size() {
        return blocked.size();
    }
}
