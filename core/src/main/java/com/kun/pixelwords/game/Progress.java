package com.kun.pixelwords.game;

import java.util.Arrays;
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

    private static final String STARS_KEY = "stars";
    private static final int STARS_PER_CHUNK = 30;
    private static final int MAX_STARS_STORED = 3;
    /** Far beyond any real save; stops a corrupt key from allocating a huge array. */
    private static final int MAX_STAR_CHUNKS = 10_000;

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
    /** How many finished chapters have paid their reward. Chapters pay in order. */
    public int chaptersRewarded = 0;
    /** The week the quests below belong to, and the stats when it began. See Quest. */
    public long questWeek = 0;
    public int questBaseWords = 0;
    public int questBaseLevel = 0;
    public int questBaseDuels = 0;
    public int questBaseDailies = 0;
    /** This week's quest rewards already paid, as a bit per quest. */
    public int questClaimed = 0;
    /** Best stars per level, indexed by level - 1. */
    private byte[] levelStars = new byte[0];
    /** Shop.Item id worn in each slot; 0 = nothing. */
    public int wearHead = 0;
    public int wearFace = 0;
    public int wearNeck = 0;
    public StreakLogic.State streak = StreakLogic.State.fresh();
    /** Wall-clock millis of the last change; used to resolve cloud-save conflicts. */
    public long updatedAt = 0;

    /** Best stars earned on a level, or 0 if none are recorded. */
    public int starsFor(int level) {
        return level >= 1 && level <= levelStars.length ? levelStars[level - 1] : 0;
    }

    /** Keeps the better of the old and new result, so a replay can only ever improve a level. */
    public void recordStars(int level, int stars) {
        if (level < 1 || stars <= starsFor(level)) return;
        if (level > levelStars.length) levelStars = Arrays.copyOf(levelStars, level);
        levelStars[level - 1] = (byte) Math.min(stars, MAX_STARS_STORED);
    }

    /** Stars go out as numbers of 30 levels at 2 bits each, so a long save stays a few short lines. */
    private void putStars(StringBuilder sb) {
        for (int chunk = 0; chunk * STARS_PER_CHUNK < levelStars.length; chunk++) {
            long packed = 0;
            for (int i = 0; i < STARS_PER_CHUNK; i++) {
                packed |= (long) starsFor(chunk * STARS_PER_CHUNK + i + 1) << (i * 2);
            }
            if (packed != 0) put(sb, STARS_KEY + chunk, packed);
        }
    }

    private void readStars(Map<String, Long> m) {
        for (Map.Entry<String, Long> e : m.entrySet()) {
            if (!e.getKey().startsWith(STARS_KEY)) continue;
            int chunk;
            try {
                chunk = Integer.parseInt(e.getKey().substring(STARS_KEY.length()));
            } catch (NumberFormatException ignored) {
                continue; // a corrupt key loses only its own levels
            }
            if (chunk < 0 || chunk > MAX_STAR_CHUNKS) continue;
            for (int i = 0; i < STARS_PER_CHUNK; i++) {
                recordStars(chunk * STARS_PER_CHUNK + i + 1, (int) (e.getValue() >>> (i * 2)) & 3);
            }
        }
    }

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
        put(sb, "chapters", chaptersRewarded);
        put(sb, "qWeek", questWeek);
        put(sb, "qWords", questBaseWords);
        put(sb, "qLevel", questBaseLevel);
        put(sb, "qDuels", questBaseDuels);
        put(sb, "qDailies", questBaseDailies);
        put(sb, "qClaimed", questClaimed);
        putStars(sb);
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
        p.chaptersRewarded = (int) Math.max(0, get(m, "chapters", 0));
        p.questWeek = get(m, "qWeek", 0);
        p.questBaseWords = (int) get(m, "qWords", 0);
        p.questBaseLevel = (int) get(m, "qLevel", 0);
        p.questBaseDuels = (int) get(m, "qDuels", 0);
        p.questBaseDailies = (int) get(m, "qDailies", 0);
        p.questClaimed = (int) get(m, "qClaimed", 0);
        p.readStars(m);
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
        out.chaptersRewarded = Math.max(a.chaptersRewarded, b.chaptersRewarded);
        Progress quests = a.questWeek != b.questWeek ? (a.questWeek > b.questWeek ? a : b) : newer;
        out.questWeek = quests.questWeek;
        out.questBaseWords = quests.questBaseWords;
        out.questBaseLevel = quests.questBaseLevel;
        out.questBaseDuels = quests.questBaseDuels;
        out.questBaseDailies = quests.questBaseDailies;
        out.questClaimed = a.questWeek == b.questWeek ? a.questClaimed | b.questClaimed : quests.questClaimed;
        for (int level = 1; level <= Math.max(a.levelStars.length, b.levelStars.length); level++) {
            out.recordStars(level, Math.max(a.starsFor(level), b.starsFor(level)));
        }
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
