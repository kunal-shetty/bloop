package com.kun.pixelwords.lan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One Wi-Fi race as seen from this phone, on either side of the connection. Messages are single lines
 * of tab-separated fields. Everything is sent from a background thread, so the game can call this from
 * the UI thread. Listener calls arrive on a background thread.
 */
public abstract class LanMatch {

    public interface Listener {
        void onPlayersChanged();

        void onRaceStarted();

        void onScoresChanged();

        void onRaceEnded();

        /** The connection is gone. Not called after close(). */
        void onDisconnected(String reason);
    }

    /** Bump when messages change, so phones on different versions refuse each other politely. */
    static final int PROTOCOL_VERSION = 1;

    public static final int MAX_PLAYERS = 4;
    public static final int RACE_SECONDS = 90;

    static final String HELLO = "HELLO", REJECT = "REJECT", PLAYERS = "PLAYERS", START = "START";
    static final String SCORE = "SCORE", DONE = "DONE", END = "END";
    static final String SEPARATOR = "\t";
    static final String PLAYER_SEPARATOR = "|";

    private static final Listener NO_LISTENER = new Listener() {
        @Override public void onPlayersChanged() {}
        @Override public void onRaceStarted() {}
        @Override public void onScoresChanged() {}
        @Override public void onRaceEnded() {}
        @Override public void onDisconnected(String reason) {}
    };

    final String localId;
    final Map<String, LanPlayer> players = new LinkedHashMap<>();
    final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lan-send");
        t.setDaemon(true);
        return t;
    });
    volatile Listener listener = NO_LISTENER;
    volatile boolean closed;
    volatile boolean raceEnded;
    volatile long seed;
    volatile int seconds;

    LanMatch(String localId) {
        this.localId = localId;
    }

    public void setListener(Listener l) {
        listener = l == null ? NO_LISTENER : l;
    }

    public long seed() {
        return seed;
    }

    public int seconds() {
        return seconds;
    }

    public boolean isHost() {
        return false;
    }

    public List<LanPlayer> players() {
        synchronized (players) {
            return new ArrayList<>(players.values());
        }
    }

    /** This phone's latest score, sent to everyone. */
    public abstract void sendScore(int points);

    /** This phone has stopped playing. Solving the board ends the race for everyone. */
    public abstract void finish(int points, boolean solved);

    /** Leaves the race for good. Safe to call more than once. */
    public abstract void close();

    final void setScore(String playerId, int points) {
        synchronized (players) {
            LanPlayer p = players.get(playerId);
            if (p != null) p.score = points;
        }
    }

    final void endRace() {
        if (raceEnded) return;
        raceEnded = true;
        listener.onRaceEnded();
    }

    static String line(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(SEPARATOR);
            sb.append(fields[i]);
        }
        return sb.toString();
    }

    static String[] fields(String line) {
        return line.split(SEPARATOR, -1);
    }
}
