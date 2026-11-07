package com.kun.pixelwords.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Everything that is saved for a player, locally and in the cloud. Serialised as simple
 * "key=value" lines so core has no JSON dependency and saves stay human-readable.
 *
 * Levels are never stored, only the level NUMBER: levels are regenerated deterministically.
 */
public final class Progress {

    public static final int FORMAT_VERSION = 1;

    private static final String STARS_KEY = "stars";
    private static final String HOME_KEY = "home";
    private static final String PAID_KEY = "paid";
    private static final String ALBUM_KEY = "page";
    private static final String DAILY_LOG_KEY = "dlog";
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
    /** The day of the cat's last coin gift, and how many it gave that day. */
    public long catGiftDay = 0;
    public int catGiftsToday = 0;
    /** The day of the last coin video, and how many were watched that day. */
    public long coinVideoDay = 0;
    public int coinVideosToday = 0;
    /** Coins bought with real money, by purchase. See Purchases. */
    public final Map<Long, Integer> paidCoins = new TreeMap<>();
    /** Furniture bought, as a bit per Home.Item id. */
    public long homeOwned = 0;
    /** Room themes bought, as a bit per Home.Theme id, and the one on show. */
    public long themesOwned = 0;
    public int homeTheme = 0;
    /** Bonus words waiting in the jar, and the day and count of its last openings. See PiggyJar. */
    public int jarWords = 0;
    public long jarDay = 0;
    public int jarOpensToday = 0;
    /** Power-ups held. See PowerUps. */
    public int revealTokens = 0;
    public int pickTokens = 0;
    public int timeTokens = 0;
    /** The weekend board in progress and its found words, and the last weekend one was paid. See MegaBoard. */
    public long megaWeek = 0;
    public long megaFound = 0;
    public long megaSolvedWeek = 0;
    /** Best score in Nine Lives. */
    public int bestLives = 0;
    /** Cat breeds owned, as a bit per Shop.Breed id, and the one in use. */
    public long breedsOwned = 0;
    public int breed = 0;
    /** The last day spun, and spins since the last legendary. See DailySpin. */
    public long spinDay = 0;
    public int spinsDry = 0;
    /** The trip under way, and the start of the last one paid. See Expedition. */
    public int expPlace = 0;
    public long expStart = 0;
    public long expEnd = 0;
    public long expClaimed = 0;
    /** Stickers, one number per Album.Page with a bit per word, and pages paid. */
    public final long[] album = new long[Album.Page.values().length];
    public long albumClaimed = 0;
    /** The season under way. See Season. */
    public long seasonId = 0;
    public int seasonXp = 0;
    public long seasonClaimedFree = 0;
    public long seasonClaimedPremium = 0;
    public boolean seasonPremium = false;
    /** The Fish Hunt the catch belongs to, the catch, and prizes paid. See FishHunt. */
    public long fishEvent = 0;
    public int fishCount = 0;
    public int fishClaimed = 0;
    /** Streak rewards paid, as a bit per StreakMilestone. */
    public long streakPaid = 0;
    /** The last day the game was opened. See Comeback. */
    public long lastSeenDay = 0;
    /** Bond with the cat, the day and count of its last treats, and whether its top prize was paid. See Bond. */
    public int bondXp = 0;
    public long treatDay = 0;
    public int treatsToday = 0;
    public boolean bondCollarPaid = false;
    /** Dailies solved, as 64 days to a number. See DailyArchive. */
    public final Map<Long, Long> dailyLog = new TreeMap<>();
    /** The past daily the player has paid to open, or 0. */
    public long archiveOpenDay = 0;
    /** Gift codes used, as a bit per RedeemCode id. */
    public long redeemedCodes = 0;
    /** What sits in each Home.Slot and where, packed by Home. */
    public final int[] homePlaced = new int[Home.Slot.values().length];
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

    private void readDailyLog(Map<String, Long> m) {
        for (Map.Entry<String, Long> e : m.entrySet()) {
            if (!e.getKey().startsWith(DAILY_LOG_KEY)) continue;
            try {
                dailyLog.put(Long.parseLong(e.getKey().substring(DAILY_LOG_KEY.length())), e.getValue());
            } catch (NumberFormatException ignored) {
                // a corrupt key loses only its own days
            }
        }
    }

    private void putPaidCoins(StringBuilder sb) {
        for (Map.Entry<Long, Integer> grant : paidCoins.entrySet()) {
            put(sb, PAID_KEY + Long.toHexString(grant.getKey()), grant.getValue());
        }
    }

    private void readPaidCoins(Map<String, Long> m) {
        for (Map.Entry<String, Long> e : m.entrySet()) {
            if (!e.getKey().startsWith(PAID_KEY) || e.getValue() <= 0) continue;
            try {
                paidCoins.put(Long.parseUnsignedLong(e.getKey().substring(PAID_KEY.length()), 16), e.getValue().intValue());
            } catch (NumberFormatException ignored) {
                // a corrupt key loses only its own ledger entry; the coins stay in the balance
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
        put(sb, "giftDay", catGiftDay);
        put(sb, "gifts", catGiftsToday);
        put(sb, "videoDay", coinVideoDay);
        put(sb, "videos", coinVideosToday);
        putPaidCoins(sb);
        put(sb, "homeOwned", homeOwned);
        put(sb, "themes", themesOwned);
        put(sb, "homeTheme", homeTheme);
        put(sb, "jarWords", jarWords);
        put(sb, "jarDay", jarDay);
        put(sb, "jarOpens", jarOpensToday);
        put(sb, "tokReveal", revealTokens);
        put(sb, "tokPick", pickTokens);
        put(sb, "tokTime", timeTokens);
        put(sb, "megaWeek", megaWeek);
        put(sb, "megaFound", megaFound);
        put(sb, "megaSolved", megaSolvedWeek);
        put(sb, "bestLives", bestLives);
        put(sb, "breeds", breedsOwned);
        put(sb, "breed", breed);
        put(sb, "spinDay", spinDay);
        put(sb, "spinsDry", spinsDry);
        put(sb, "expPlace", expPlace);
        put(sb, "expStart", expStart);
        put(sb, "expEnd", expEnd);
        put(sb, "expClaimed", expClaimed);
        for (int i = 0; i < album.length; i++) put(sb, ALBUM_KEY + i, album[i]);
        put(sb, "albumClaimed", albumClaimed);
        put(sb, "seasonId", seasonId);
        put(sb, "seasonXp", seasonXp);
        put(sb, "seasonFree", seasonClaimedFree);
        put(sb, "seasonPrem", seasonClaimedPremium);
        put(sb, "seasonPass", seasonPremium ? 1 : 0);
        put(sb, "fishEvent", fishEvent);
        put(sb, "fishCount", fishCount);
        put(sb, "fishClaimed", fishClaimed);
        put(sb, "streakPaid", streakPaid);
        put(sb, "lastSeen", lastSeenDay);
        put(sb, "bondXp", bondXp);
        put(sb, "treatDay", treatDay);
        put(sb, "treats", treatsToday);
        put(sb, "bondCollar", bondCollarPaid ? 1 : 0);
        for (Map.Entry<Long, Long> block : dailyLog.entrySet()) put(sb, DAILY_LOG_KEY + block.getKey(), block.getValue());
        put(sb, "archiveOpen", archiveOpenDay);
        put(sb, "codes", redeemedCodes);
        for (int i = 0; i < homePlaced.length; i++) put(sb, HOME_KEY + i, homePlaced[i]);
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
        p.catGiftDay = get(m, "giftDay", 0);
        p.catGiftsToday = (int) Math.max(0, get(m, "gifts", 0));
        p.coinVideoDay = get(m, "videoDay", 0);
        p.coinVideosToday = (int) Math.max(0, get(m, "videos", 0));
        p.readPaidCoins(m);
        p.homeOwned = get(m, "homeOwned", 0);
        p.themesOwned = get(m, "themes", 0);
        p.homeTheme = (int) get(m, "homeTheme", 0);
        p.jarWords = (int) Math.max(0, get(m, "jarWords", 0));
        p.jarDay = get(m, "jarDay", 0);
        p.jarOpensToday = (int) Math.max(0, get(m, "jarOpens", 0));
        p.revealTokens = (int) Math.max(0, get(m, "tokReveal", 0));
        p.pickTokens = (int) Math.max(0, get(m, "tokPick", 0));
        p.timeTokens = (int) Math.max(0, get(m, "tokTime", 0));
        p.megaWeek = get(m, "megaWeek", 0);
        p.megaFound = get(m, "megaFound", 0);
        p.megaSolvedWeek = get(m, "megaSolved", 0);
        p.bestLives = (int) Math.max(0, get(m, "bestLives", 0));
        p.breedsOwned = get(m, "breeds", 0);
        p.breed = (int) get(m, "breed", 0);
        p.spinDay = get(m, "spinDay", 0);
        p.spinsDry = (int) Math.max(0, get(m, "spinsDry", 0));
        p.expPlace = (int) get(m, "expPlace", 0);
        p.expStart = get(m, "expStart", 0);
        p.expEnd = get(m, "expEnd", 0);
        p.expClaimed = get(m, "expClaimed", 0);
        for (int i = 0; i < p.album.length; i++) p.album[i] = get(m, ALBUM_KEY + i, 0);
        p.albumClaimed = get(m, "albumClaimed", 0);
        p.seasonId = get(m, "seasonId", 0);
        p.seasonXp = (int) Math.max(0, get(m, "seasonXp", 0));
        p.seasonClaimedFree = get(m, "seasonFree", 0);
        p.seasonClaimedPremium = get(m, "seasonPrem", 0);
        p.seasonPremium = get(m, "seasonPass", 0) == 1;
        p.fishEvent = get(m, "fishEvent", 0);
        p.fishCount = (int) Math.max(0, get(m, "fishCount", 0));
        p.fishClaimed = (int) get(m, "fishClaimed", 0);
        p.streakPaid = get(m, "streakPaid", 0);
        p.lastSeenDay = get(m, "lastSeen", 0);
        p.bondXp = (int) Math.max(0, get(m, "bondXp", 0));
        p.treatDay = get(m, "treatDay", 0);
        p.treatsToday = (int) Math.max(0, get(m, "treats", 0));
        p.bondCollarPaid = get(m, "bondCollar", 0) == 1;
        p.readDailyLog(m);
        p.archiveOpenDay = get(m, "archiveOpen", 0);
        p.redeemedCodes = get(m, "codes", 0);
        for (int i = 0; i < p.homePlaced.length; i++) p.homePlaced[i] = (int) get(m, HOME_KEY + i, 0);
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
     * Purchases are sticky: once ads are removed on any device they stay removed, and coins bought on
     * either device are added to the newer balance if it doesn't already include them.
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
        out.homeOwned = a.homeOwned | b.homeOwned; // a purchase on any device is kept
        System.arraycopy(newer.homePlaced, 0, out.homePlaced, 0, out.homePlaced.length);
        out.catGiftDay = Math.max(a.catGiftDay, b.catGiftDay);
        out.catGiftsToday = Math.max(a.catGiftDay == out.catGiftDay ? a.catGiftsToday : 0,
                b.catGiftDay == out.catGiftDay ? b.catGiftsToday : 0);
        out.coinVideoDay = Math.max(a.coinVideoDay, b.coinVideoDay);
        out.coinVideosToday = Math.max(a.coinVideoDay == out.coinVideoDay ? a.coinVideosToday : 0,
                b.coinVideoDay == out.coinVideoDay ? b.coinVideosToday : 0);
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
        out.paidCoins.putAll(newer.paidCoins);
        Purchases.absorb(out, older);
        out.hintTokens = newer.hintTokens;
        mergeContentUpdate(out, a, b, newer);

        StreakLogic.State ns = newer.streak;
        StreakLogic.State os = older.streak;
        StreakLogic.State pick = ns.lastDay >= os.lastDay ? ns : os;
        out.streak = new StreakLogic.State(pick.streak, Math.max(ns.best, os.best), pick.lastDay, pick.freezes);
        out.updatedAt = Math.max(a.updatedAt, b.updatedAt);
        return out;
    }

    /** The rule for each field: owned things and claims add up, counters take the highest, the rest follow the newer save. */
    private static void mergeContentUpdate(Progress out, Progress a, Progress b, Progress newer) {
        out.themesOwned = a.themesOwned | b.themesOwned;
        out.homeTheme = newer.homeTheme;
        out.jarWords = newer.jarWords;
        out.jarDay = Math.max(a.jarDay, b.jarDay);
        out.jarOpensToday = Math.max(a.jarDay == out.jarDay ? a.jarOpensToday : 0,
                b.jarDay == out.jarDay ? b.jarOpensToday : 0);
        out.revealTokens = newer.revealTokens;
        out.pickTokens = newer.pickTokens;
        out.timeTokens = newer.timeTokens;
        Progress mega = a.megaWeek != b.megaWeek ? (a.megaWeek > b.megaWeek ? a : b) : newer;
        out.megaWeek = mega.megaWeek;
        out.megaFound = a.megaWeek == b.megaWeek ? a.megaFound | b.megaFound : mega.megaFound;
        out.megaSolvedWeek = Math.max(a.megaSolvedWeek, b.megaSolvedWeek);
        out.bestLives = Math.max(a.bestLives, b.bestLives);
        out.breedsOwned = a.breedsOwned | b.breedsOwned;
        out.breed = newer.breed;
        out.spinDay = Math.max(a.spinDay, b.spinDay);
        out.spinsDry = newer.spinsDry;
        Progress trip = a.expStart != b.expStart ? (a.expStart > b.expStart ? a : b) : newer;
        out.expClaimed = Math.max(a.expClaimed, b.expClaimed);
        out.expStart = trip.expStart;
        out.expEnd = a.expStart == b.expStart ? Math.min(a.expEnd, b.expEnd) : trip.expEnd;
        out.expPlace = trip.expStart > out.expClaimed ? trip.expPlace : 0;
        for (int i = 0; i < out.album.length; i++) out.album[i] = a.album[i] | b.album[i];
        out.albumClaimed = a.albumClaimed | b.albumClaimed;
        boolean sameSeason = a.seasonId == b.seasonId;
        Progress season = !sameSeason ? (a.seasonId > b.seasonId ? a : b) : newer;
        out.seasonId = season.seasonId;
        out.seasonXp = sameSeason ? Math.max(a.seasonXp, b.seasonXp) : season.seasonXp;
        out.seasonClaimedFree = sameSeason ? a.seasonClaimedFree | b.seasonClaimedFree : season.seasonClaimedFree;
        out.seasonClaimedPremium = sameSeason ? a.seasonClaimedPremium | b.seasonClaimedPremium : season.seasonClaimedPremium;
        out.seasonPremium = sameSeason ? a.seasonPremium || b.seasonPremium : season.seasonPremium;
        boolean sameHunt = a.fishEvent == b.fishEvent;
        Progress hunt = !sameHunt ? (a.fishEvent > b.fishEvent ? a : b) : newer;
        out.fishEvent = hunt.fishEvent;
        out.fishCount = sameHunt ? Math.max(a.fishCount, b.fishCount) : hunt.fishCount;
        out.fishClaimed = sameHunt ? a.fishClaimed | b.fishClaimed : hunt.fishClaimed;
        out.streakPaid = a.streakPaid | b.streakPaid;
        out.lastSeenDay = Math.max(a.lastSeenDay, b.lastSeenDay);
        out.bondXp = Math.max(a.bondXp, b.bondXp);
        out.treatDay = Math.max(a.treatDay, b.treatDay);
        out.treatsToday = Math.max(a.treatDay == out.treatDay ? a.treatsToday : 0,
                b.treatDay == out.treatDay ? b.treatsToday : 0);
        out.bondCollarPaid = a.bondCollarPaid || b.bondCollarPaid;
        out.dailyLog.putAll(a.dailyLog);
        for (Map.Entry<Long, Long> block : b.dailyLog.entrySet()) out.dailyLog.merge(block.getKey(), block.getValue(), (x, y) -> x | y);
        out.archiveOpenDay = newer.archiveOpenDay;
        out.redeemedCodes = a.redeemedCodes | b.redeemedCodes; // a code is only ever paid once
    }

    private static void put(StringBuilder sb, String k, long v) {
        sb.append(k).append('=').append(v).append('\n');
    }

    private static long get(Map<String, Long> m, String k, long def) {
        Long v = m.get(k);
        return v == null ? def : v;
    }
}
