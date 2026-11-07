package com.kun.pixelwords.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** What the grid's words mean, read from the file tools/gen_defs.py writes. Works offline. */
public final class Definitions {

    /** One word's meaning. {@code baseWord} is the word itself unless this is an inflection, like cats for cat. */
    public static final class Entry {
        public final String word;
        public final String partOfSpeech;
        public final String baseWord;
        public final String meaning;

        private Entry(String word, String partOfSpeech, String baseWord, String meaning) {
            this.word = word;
            this.partOfSpeech = partOfSpeech;
            this.baseWord = baseWord;
            this.meaning = meaning;
        }

        public boolean isInflected() {
            return !baseWord.equals(word);
        }
    }

    private static final int FIELDS = 4;

    private final Map<String, Entry> entries = new HashMap<>();

    private Definitions() {}

    /** A malformed line is skipped, which costs that one word its meaning and nothing else. */
    public static Definitions load(InputStream in) throws IOException {
        Definitions d = new Definitions();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                String[] f = line.split("\t");
                if (f.length == FIELDS) d.entries.put(f[0], new Entry(f[0], f[1], f[2], f[3]));
            }
        }
        return d;
    }

    /** The meaning of a word in any case, or null if there isn't one. */
    public Entry find(String word) {
        return entries.get(word.toLowerCase(Locale.ROOT));
    }

    public int size() {
        return entries.size();
    }
}
