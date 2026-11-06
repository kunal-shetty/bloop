package com.kun.pixelwords.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything that is saved for a player, locally and in the cloud. Serialised as simple
 * "key=value" lines so core has no JSON dependency and saves stay human-readable.
 *
 * Levels are never stored, only the level NUMBER: levels are regenerated deterministically.
 */
public final class Progress {

    public static final int FORMAT_VERSION = 1;

    /** The next campaign level to play (1-based). */
    public int level = 1;
    public int coins = 0;
    public int hintTokens = 3;
    public int totalWordsFound = 0;
    public int dailiesCompleted = 0;
    public boolean adsRemoved = false;
    public int bestTimed = 0;
    public int duelWins = 0;
    public int duelsPlayed = 0;
    /** Shop items bought, as a bit per Shop.Item id. */
    public long ownedItems = 0;
    /** Achievement rewards already paid, as a bit per Achievement id. */
    public long claimedAchievements = 0;
    /** Shop.Item id worn in each slot; 0 = nothing. */
    public int wearHead = 0;
    public int wearFace = 0;
    public int wearNeck = 0;
    public StreakLogic.State streak = StreakLogic.State.fresh();
    /** Wall-clock millis of the last change; used to resolve cloud-save conflicts. */
    public long updatedAt = 0;

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        put(sb, "v", FORMAT_VERSION);
        put(sb, "level", level);
        put(sb, "coins", coins);
        put(sb, "hints", hintTokens);
        put(sb, "words", totalWordsFound);
        put(sb, "dailies", dailiesCompleted);
        put(sb, "noads", adsRemoved ? 1 : 0);
        put(sb, "bestTimed", bestTimed);
        put(sb, "duelWins", duelWins);
        put(sb, "duelsPlayed", duelsPlayed);
        put(sb, "owned", ownedItems);
        put(sb, "claimed", claimedAchievements);
        put(sb, "wearHead", wearHead);
        put(sb, "wearFace", wearFace);
        put(sb, "wearNeck", wearNeck);
        put(sb, "streak", streak.streak);
        put(sb, "bestStreak", streak.best);
        put(sb, "lastDay", streak.lastDay);
        put(sb, "freezes", streak.freezes);
        put(sb, "updatedAt", updatedAt);
        return sb.toString();
    }

    public static Progress parse(String text) {
        Progress p = new Progress();
        if (text == null || text.isEmpty()) return p;
        Map<String, Long> m = new HashMap<>();
        for (String line : text.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            try {
                m.put(line.substring(0, eq).trim(), Long.parseLong(line.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // skip corrupt line; keep defaults for that field
            }
        }
        p.level = (int) Math.max(1, get(m, "level", 1));
        p.coins = (int) Math.max(0, get(m, "coins", 0));
        p.hintTokens = (int) Math.max(0, get(m, "hints", 3));
        p.totalWordsFound = (int) Math.max(0, get(m, "words", 0));
        p.dailiesCompleted = (int) Math.max(0, get(m, "dailies", 0));
        p.adsRemoved = get(m, "noads", 0) == 1;
        p.bestTimed = (int) Math.max(0, get(m, "bestTimed", 0));
        p.duelWins = (int) Math.max(0, get(m, "duelWins", 0));
        p.duelsPlayed = (int) Math.max(0, get(m, "duelsPlayed", 0));
        p.ownedItems = get(m, "owned", 0);
        p.claimedAchievements = get(m, "claimed", 0);
        p.wearHead = (int) get(m, "wearHead", 0);
        p.wearFace = (int) get(m, "wearFace", 0);
        p.wearNeck = (int) get(m, "wearNeck", 0);
        p.streak = new StreakLogic.State(
                (int) get(m, "streak", 0), (int) get(m, "bestStreak", 0),
                get(m, "lastDay", -1), (int) get(m, "freezes", 0));
        p.updatedAt = get(m, "updatedAt", 0);
        return p;
    }

    /**
     * Conflict resolution between this device and the cloud copy. Monotonic stats take the maximum so
     * progress is never lost; the "current state" fields (coins, hints, streak) follow whichever save is newer.
     * Purchases are sticky: once ads are removed on any device they stay removed.
     */
    public static Progress merge(Progress a, Progress b) {
        Progress newer = a.updatedAt >= b.updatedAt ? a : b;
        Progress older = newer == a ? b : a;

        Progress out = new Progress();
        out.level = Math.max(a.level, b.level);
        out.totalWordsFound = Math.max(a.totalWordsFound, b.totalWordsFound);
        out.dailiesCompleted = Math.max(a.dailiesCompleted, b.dailiesCompleted);
        out.adsRemoved = a.adsRemoved || b.adsRemoved;
        out.bestTimed = Math.max(a.bestTimed, b.bestTimed);
        out.duelWins = Math.max(a.duelWins, b.duelWins);
        out.duelsPlayed = Math.max(a.duelsPlayed, b.duelsPlayed);
        out.ownedItems = a.ownedItems | b.ownedItems; // a purchase on any device is kept
        out.claimedAchievements = a.claimedAchievements | b.claimedAchievements; // a reward is only ever paid once
        out.wearHead = newer.wearHead;
        out.wearFace = newer.wearFace;
        out.wearNeck = newer.wearNeck;
        out.coins = newer.coins;
        out.hintTokens = newer.hintTokens;

        StreakLogic.State ns = newer.streak;
        StreakLogic.State os = older.streak;
        StreakLogic.State pick = ns.lastDay >= os.lastDay ? ns : os;
        out.streak = new StreakLogic.State(pick.streak, Math.max(ns.best, os.best), pick.lastDay, pick.freezes);
        out.updatedAt = Math.max(a.updatedAt, b.updatedAt);
        return out;
    }

    private static void put(StringBuilder sb, String k, long v) {
        sb.append(k).append('=').append(v).append('\n');
    }

    private static long get(Map<String, Long> m, String k, long def) {
        Long v = m.get(k);
        return v == null ? def : v;
    }
}
