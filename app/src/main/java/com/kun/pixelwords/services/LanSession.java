package com.kun.pixelwords.services;

import com.kun.pixelwords.lan.LanMatch;

/** Carries the live race from the lobby screen to the game screen; a connection can't go in an Intent. */
public final class LanSession {

    private static LanMatch current;

    private LanSession() {}

    public static void set(LanMatch match) {
        current = match;
    }

    public static LanMatch get() {
        return current;
    }

    /** Ends the race's connection, if there is one. */
    public static void close() {
        if (current != null) current.close();
        current = null;
    }
}
