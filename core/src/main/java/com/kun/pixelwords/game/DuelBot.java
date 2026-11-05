package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.Level;
import com.kun.pixelwords.generator.SeedUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Offline stand-in opponent for word duels. Produces a deterministic "script" of which grid word the
 * bot finds and when, so a duel can be played and tested with no server. Real opponents will later
 * replace this by submitting their own timeline for the same shared seed.
 */
public final class DuelBot {

    private DuelBot() {}

    public static final class Event {
        public final int atSecond;
        public final String word;

        Event(int atSecond, String word) {
            this.atSecond = atSecond;
            this.word = word;
        }
    }

    /**
     * @param skill 0.0 (slow, finds few words) .. 1.0 (fast, finds nearly all)
     */
    public static List<Event> script(Level level, long matchSeed, double skill, int durationSeconds) {
        Random rnd = new Random(SeedUtil.mix(matchSeed, 0xB07L));
        List<String> words = new ArrayList<>();
        for (Level.Placement p : level.placements) words.add(p.word);
        Collections.shuffle(words, rnd);

        double clamped = Math.max(0.0, Math.min(1.0, skill));
        int target = Math.max(1, (int) Math.round(words.size() * (0.4 + 0.6 * clamped)));
        double meanGap = durationSeconds * (1.0 - 0.55 * clamped) / (target + 1);

        List<Event> events = new ArrayList<>();
        double t = 0;
        for (int i = 0; i < target; i++) {
            t += meanGap * (0.5 + rnd.nextDouble());
            if (t >= durationSeconds) break;
            events.add(new Event((int) t, words.get(i)));
        }
        return events;
    }

    /** Points the bot has scored by {@code second} (grid words only). */
    public static int scoreAt(List<Event> script, int second) {
        int pts = 0;
        for (Event e : script) if (e.atSecond <= second) pts += Scoring.gridWordPoints(e.word);
        return pts;
    }
}
