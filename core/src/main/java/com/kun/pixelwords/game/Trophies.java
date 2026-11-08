package com.kun.pixelwords.game;

/**
 * A trophy for every boss beaten. Each chapter ends with its boss, so the trophies won follow from how
 * far the player is in the Journey and need no saving of their own. Only the one on show in the room is
 * saved, as {@code trophyShown}: the chapter plus one, or zero for the newest.
 */
public final class Trophies {

    private Trophies() {}

    /** Bosses beaten: every chapter before the one being played. */
    public static int count(Progress p) {
        return Math.max(0, Journey.chapterOf(p.level));
    }

    public static boolean has(Progress p, int chapter) {
        return chapter >= 0 && chapter < count(p);
    }

    /** The chapter whose trophy stands in the room, or -1 before the first boss falls. */
    public static int shown(Progress p) {
        int pinned = p.trophyShown - 1;
        return has(p, pinned) ? pinned : count(p) - 1;
    }

    /** Puts a trophy the player has on show. */
    public static void show(Progress p, int chapter) {
        if (has(p, chapter)) p.trophyShown = chapter + 1;
    }
}
