package com.kun.pixelwords.generator;

import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.CatMood;
import com.kun.pixelwords.game.DuelBot;
import com.kun.pixelwords.game.GameSession;
import com.kun.pixelwords.game.Home;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.Shop;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Quest;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.ShareCard;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.lan.LanClient;
import com.kun.pixelwords.lan.LanHost;
import com.kun.pixelwords.lan.LanMatch;
import com.kun.pixelwords.lan.LanPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Dependency-free self-test (plain main, no JUnit) so it runs anywhere with only a JDK:
 *   ./gradlew :generator:selfTest
 * Exits non-zero if any check fails.
 */
public final class GeneratorSelfTest {

    private static int failures = 0;
    private static int checks = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        long t0 = System.currentTimeMillis();
        Dictionary dict = loadDictionary();
        LevelGenerator gen = new LevelGenerator(dict);
        System.out.println("Dictionary loaded in " + (System.currentTimeMillis() - t0) + " ms: "
                + dict.validWords().size() + " valid, " + dict.commonWords().size() + " common");

        testDictionary(dict);
        testFixedLevels(gen, dict);
        testProgression(gen, dict, 4, 250);
        testDaily(gen, dict, 20400, 60);
        testDuel(gen, dict);
        testGameSession(gen);
        testStreaks();
        testProgress();
        testDuelBot(gen);
        testShop();
        testShareCard();
        testAchievements();
        testAchievementRewards();
        testJourney(gen, dict);
        testQuests();
        testCatMood();
        testHome();
        testLan();
        testCurve(gen);

        System.out.println();
        printLevel(gen.forProgression(3));
        printLevel(gen.forProgression(20));
        printLevel(gen.forProgression(120));
        printLevel(gen.forProgression(300));
        printLevel(gen.forDaily(20400));

        System.out.println("\n" + checks + " checks, " + failures + " failures, "
                + (System.currentTimeMillis() - t0) + " ms total");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ tests

    private static void testAchievements() {
        Progress p = new Progress();
        check(Achievement.unlocked(p).isEmpty(), "a new player has no achievements");

        Set<Achievement> before = Achievement.unlocked(p);
        p.level = 11;
        p.duelWins = 1;
        List<Achievement> fresh = Achievement.newlyUnlocked(before, p);
        check(fresh.size() == 2 && fresh.contains(Achievement.LEVELS_10) && fresh.contains(Achievement.DUEL_1),
                "clearing level 10 and a first duel win unlock exactly those two");
        check(Achievement.newlyUnlocked(Achievement.unlocked(p), p).isEmpty(), "nothing is announced twice");

        p.level = 10;
        check(!Achievement.LEVELS_10.isUnlocked(p), "being on level 10 has only cleared 9");
        check(Achievement.LEVELS_10.progress(p) == 9, "progress counts cleared levels");
        p.level = 500;
        check(Achievement.LEVELS_10.progress(p) == 10, "progress is capped at the goal");

        check(!Achievement.ALL_ITEMS.isUnlocked(p), "no accessories owned yet");
        p.coins = 100_000;
        for (Shop.Item item : Shop.Item.values()) {
            check(!Achievement.ALL_ITEMS.isUnlocked(p), "the last accessory is still needed");
            Shop.buy(p, item);
        }
        check(Achievement.ALL_ITEMS.isUnlocked(p), "owning every accessory unlocks the collector goal");

        p.streak = StreakLogic.onDailyCompleted(StreakLogic.State.fresh(), 100);
        for (long day = 101; day < 107; day++) p.streak = StreakLogic.onDailyCompleted(p.streak, day);
        check(Achievement.STREAK_7.isUnlocked(p), "seven days in a row reaches the streak goal");
        check(Achievement.STREAK_7.isUnlocked(Progress.parse(p.serialize())), "achievements survive a save and load");
    }

    private static void testJourney(LevelGenerator gen, Dictionary dict) {
        check(Journey.chapterOf(1) == 0 && Journey.chapterOf(20) == 0 && Journey.chapterOf(21) == 1, "chapters are 20 levels");
        check(Journey.slotOf(1) == 1 && Journey.slotOf(20) == 20 && Journey.slotOf(21) == 1, "slots restart each chapter");
        check(Journey.firstLevel(2) == 41 && Journey.lastLevel(2) == 60, "chapter bounds");

        for (int chapter = 0; chapter < 40; chapter++) {
            int hard = 0;
            for (int level = Journey.firstLevel(chapter); level <= Journey.lastLevel(chapter); level++) {
                if (!Journey.isHard(level)) continue;
                hard++;
                int slot = Journey.slotOf(level);
                check(slot >= 6 && slot < 20, "hard levels avoid the start and end of a chapter");
                Level h = gen.forHard(level), same = gen.forHard(level), normal = gen.forProgression(level);
                check(h.fingerprint() == same.fingerprint(), "hard levels are deterministic");
                check(h.letters.length() >= normal.letters.length(), "a hard level never has a smaller wheel");
                int shortest = 99;
                for (Level.Placement p : h.placements) {
                    shortest = Math.min(shortest, p.word.length());
                    check(dict.isCommon(p.word), "hard grid words are still common words");
                }
                check(shortest >= 4, "hard levels have no three-letter words");
            }
            check(hard >= 2 && hard <= 3, "each chapter has two or three hard levels");
        }

        Progress p = new Progress();
        p.level = 41;
        check(Journey.chaptersToReward(p) == 2, "two finished chapters are owed");
        int coinsBefore = p.coins, hintsBefore = p.hintTokens;
        check(Journey.payChapterRewards(p) == 2, "both chapters are paid");
        check(p.coins == coinsBefore + 2 * Scoring.COINS_PER_CHAPTER, "chapter rewards pay coins");
        check(p.hintTokens == hintsBefore + 2 * Scoring.HINTS_PER_CHAPTER, "chapter rewards pay hints");
        check(Journey.payChapterRewards(p) == 0, "a chapter only pays once");

        p.recordStars(3, 2);
        p.recordStars(3, 1);
        check(p.starsFor(3) == 2, "a worse replay keeps the better stars");
        p.recordStars(3, 3);
        check(p.starsFor(3) == 3, "a better replay improves the stars");
        p.recordStars(31, 1);
        p.recordStars(95, 3);
        check(Journey.starsShown(p, 5) == 1, "a level cleared before stars existed shows one star");
        check(Journey.starsShown(p, 41) == 0, "the current level has no stars yet");

        Progress loaded = Progress.parse(p.serialize());
        check(loaded.starsFor(3) == 3 && loaded.starsFor(31) == 1 && loaded.starsFor(95) == 3, "stars survive a save and load");
        check(loaded.starsFor(4) == 0, "unplayed levels stay empty");
        check(loaded.chaptersRewarded == 2, "chapter rewards survive a save and load");

        Progress other = new Progress();
        other.recordStars(3, 1);
        other.recordStars(4, 2);
        other.updatedAt = p.updatedAt + 1;
        Progress merged = Progress.merge(p, other);
        check(merged.starsFor(3) == 3 && merged.starsFor(4) == 2 && merged.starsFor(95) == 3, "a merge keeps the best stars from both");
        check(merged.chaptersRewarded == 2, "a merge can't pay a chapter twice");
    }

    /** Counts what a phone's game screen would hear. */
    private static final class LanProbe implements LanMatch.Listener {
        final Semaphore players = new Semaphore(0), started = new Semaphore(0), scores = new Semaphore(0);
        final Semaphore ended = new Semaphore(0), lost = new Semaphore(0);
        volatile String reason = "";

        @Override public void onPlayersChanged() { players.release(); }
        @Override public void onRaceStarted() { started.release(); }
        @Override public void onScoresChanged() { scores.release(); }
        @Override public void onRaceEnded() { ended.release(); }
        @Override public void onDisconnected(String why) {
            reason = why;
            lost.release();
        }
    }

    private static boolean heard(Semaphore s) throws InterruptedException {
        return s.tryAcquire(3, TimeUnit.SECONDS);
    }

    private static LanClient join(LanHost host, String id, String name, LanProbe probe) throws IOException {
        LanClient c = new LanClient(new LanPlayer(id, name, "1234"));
        c.setListener(probe);
        c.connect(InetAddress.getLoopbackAddress(), host.port());
        return c;
    }

    private static void waitForPlayers(LanMatch m, int count) throws InterruptedException {
        for (int i = 0; i < 300 && m.players().size() != count; i++) Thread.sleep(10);
    }

    private static void testLan() throws IOException, InterruptedException {
        check(LanPlayer.cleanName("  Kunal\t|x\n ").equals("Kunalx"), "names lose anything that could break a message");
        check(LanPlayer.cleanName("").equals("Player") && LanPlayer.cleanName(null).equals("Player"), "an empty name gets a default");
        check(LanPlayer.cleanName("abcdefghijklmnopqrst").length() == LanPlayer.MAX_NAME_LENGTH, "long names are cut");
        check(new LanPlayer("a", "Kunal", "4821").label().equals("Kunal#4821"), "labels show the name and tag");

        LanProbe hostProbe = new LanProbe(), aProbe = new LanProbe(), bProbe = new LanProbe();
        LanHost host = new LanHost(new LanPlayer("host", "Host", "1111"));
        host.setListener(hostProbe);
        check(!host.startRace(LanMatch.RACE_SECONDS), "a race needs someone to play against");

        LanClient a = join(host, "a", "Asha", aProbe);
        LanClient b = join(host, "b", "Ben", bProbe);
        waitForPlayers(host, 3);
        waitForPlayers(a, 3);
        waitForPlayers(b, 3);
        check(host.players().size() == 3 && a.players().size() == 3 && b.players().size() == 3, "everyone sees the whole room");

        LanProbe sameProbe = new LanProbe();
        LanClient same = join(host, "a", "Copy", sameProbe);
        check(heard(sameProbe.lost) && sameProbe.reason.contains("already"), "the same player can't join twice");
        same.close();

        LanClient d = join(host, "d", "Dev", new LanProbe());
        waitForPlayers(host, 4);
        LanProbe fullProbe = new LanProbe();
        LanClient full = join(host, "e", "Eve", fullProbe);
        check(heard(fullProbe.lost) && fullProbe.reason.contains("full"), "a fifth player is turned away");
        full.close();
        d.close();
        waitForPlayers(host, 3);
        check(host.players().size() == 3, "a player who leaves the lobby is removed");
        waitForPlayers(a, 3);
        check(a.players().size() == 3, "everyone sees them go");

        check(host.startRace(LanMatch.RACE_SECONDS), "the host can start");
        check(heard(aProbe.started) && heard(bProbe.started), "everyone starts together");
        check(a.seed() == host.seed() && b.seed() == host.seed(), "everyone gets the same board");
        check(a.seconds() == LanMatch.RACE_SECONDS, "everyone gets the same time limit");
        check(!host.startRace(LanMatch.RACE_SECONDS), "a race can't start twice");

        LanProbe lateProbe = new LanProbe();
        LanClient late = join(host, "z", "Zed", lateProbe);
        check(heard(lateProbe.lost) && lateProbe.reason.contains("started"), "nobody can join a race in progress");
        late.close();

        a.sendScore(50);
        host.sendScore(30);
        check(heard(bProbe.scores) && heard(bProbe.scores), "scores reach the other players");
        Thread.sleep(50);
        List<LanPlayer> ranks = LanPlayer.ranked(b.players());
        check(ranks.get(0).id.equals("a") && ranks.get(0).score == 50, "the leader is ranked first");
        check(ranks.get(1).id.equals("host") && ranks.get(2).id.equals("b"), "the rest follow in score order");

        b.finish(70, true);
        check(heard(hostProbe.ended) && heard(aProbe.ended) && heard(bProbe.ended), "clearing the board ends the race for everyone");
        check(LanPlayer.ranked(host.players()).get(0).id.equals("b"), "the final score counts");
        check(!heard2(aProbe.lost), "a normal ending isn't a lost connection");
        a.close();
        b.close();
        host.close();

        testLanEndings();
    }

    private static boolean heard2(Semaphore s) throws InterruptedException {
        return s.tryAcquire(200, TimeUnit.MILLISECONDS);
    }

    private static void testLanEndings() throws IOException, InterruptedException {
        LanProbe one = new LanProbe(), two = new LanProbe();
        LanHost host = new LanHost(new LanPlayer("h", "Host", "1111"));
        host.setListener(one);
        LanClient c = join(host, "c", "Cat", two);
        waitForPlayers(host, 2);
        host.startRace(LanMatch.RACE_SECONDS);
        heard(two.started);
        host.finish(10, false);
        check(!heard2(two.ended), "the race waits for players still going");
        c.finish(20, false);
        check(heard(one.ended) && heard(two.ended), "the race ends when everyone has finished");
        c.close();
        host.close();

        LanProbe lostHost = new LanProbe();
        LanHost leaving = new LanHost(new LanPlayer("h", "Host", "1111"));
        LanClient waiting = join(leaving, "c", "Cat", lostHost);
        waitForPlayers(leaving, 2);
        leaving.close();
        check(heard(lostHost.lost), "players are told when the host leaves");
        waiting.close();

        LanProbe dropper = new LanProbe();
        LanHost dropHost = new LanHost(new LanPlayer("h", "Host", "1111"));
        dropHost.setListener(dropper);
        LanClient gone = join(dropHost, "c", "Cat", new LanProbe());
        waitForPlayers(dropHost, 2);
        dropHost.startRace(LanMatch.RACE_SECONDS);
        dropHost.finish(10, false);
        gone.close();
        check(heard(dropper.ended), "a dropped player can't hang the race");
        check(dropHost.players().size() == 2, "someone who leaves a race stays in the results");
        dropHost.close();

        LanProbe timed = new LanProbe(), stalled = new LanProbe();
        LanHost strict = new LanHost(new LanPlayer("h", "Host", "1111"), 100);
        strict.setListener(timed);
        LanClient silent = join(strict, "c", "Cat", stalled);
        waitForPlayers(strict, 2);
        strict.startRace(0);
        check(heard(timed.ended) && heard(stalled.ended), "the deadline ends a race nobody finished");
        silent.close();
        strict.close();

        Socket stranger = new Socket(InetAddress.getLoopbackAddress(), new LanHostProbe().port);
        stranger.getOutputStream().write("GET / HTTP/1.1\n".getBytes());
        stranger.getOutputStream().flush();
        check(stranger.getInputStream().read() >= 0, "a stranger is answered with a refusal, not a crash");
        stranger.close();
    }

    /** A live room for a stranger to knock on. */
    private static final class LanHostProbe {
        final int port;

        LanHostProbe() throws IOException {
            port = new LanHost(new LanPlayer("h", "Host", "1111")).port();
        }
    }

    private static void testHome() {
        Progress p = new Progress();
        for (Home.Slot slot : Home.Slot.values()) {
            check(Home.itemsIn(slot).size() == 3, "every slot has three items to choose from");
            check(Home.placed(p, slot) == null, "a new room is empty");
            check(slot.x + slot.width <= Home.ROOM_SIZE && slot.y + slot.height <= Home.ROOM_SIZE, "slots start inside the room");
        }
        check(!Home.buy(p, Home.Item.BALL), "furniture costs coins");

        p.coins = 1000;
        check(Home.buy(p, Home.Item.BALL), "buying works with enough coins");
        check(p.coins == 1000 - Home.Item.BALL.price, "buying spends the price");
        check(Home.placed(p, Home.Slot.TOY) == Home.Item.BALL, "a new item goes straight in the room");
        check(Home.x(p, Home.Slot.TOY) == Home.Slot.TOY.x && Home.y(p, Home.Slot.TOY) == Home.Slot.TOY.y, "it starts in its slot's spot");
        check(!Home.buy(p, Home.Item.BALL), "an item can't be bought twice");
        check(p.coins == 1000 - Home.Item.BALL.price, "a refused purchase spends nothing");

        Home.move(p, Home.Slot.TOY, 30, 40);
        check(Home.x(p, Home.Slot.TOY) == 30 && Home.y(p, Home.Slot.TOY) == 40, "dragging moves an item");
        Home.move(p, Home.Slot.TOY, -50, 500);
        check(Home.x(p, Home.Slot.TOY) == 0, "items stop at the left wall");
        check(Home.y(p, Home.Slot.TOY) == Home.ROOM_SIZE - Home.Slot.TOY.height, "items stop at the floor's edge");

        Home.move(p, Home.Slot.TOY, 100, 3);
        Progress loaded = Progress.parse(p.serialize());
        check(Home.owns(loaded, Home.Item.BALL), "furniture survives a save and load");
        check(Home.placed(loaded, Home.Slot.TOY) == Home.Item.BALL, "placement survives a save and load");
        check(Home.x(loaded, Home.Slot.TOY) == 100 && Home.y(loaded, Home.Slot.TOY) == 3, "position survives a save and load");

        Home.remove(p, Home.Slot.TOY);
        check(Home.placed(p, Home.Slot.TOY) == null && Home.owns(p, Home.Item.BALL), "taking an item out keeps it owned");
        Home.place(p, Home.Item.BALL);
        check(Home.x(p, Home.Slot.TOY) == Home.Slot.TOY.x, "putting it back returns it to its spot");
        Home.place(p, Home.Item.FISH);
        check(Home.placed(p, Home.Slot.TOY) == Home.Item.BALL, "unowned furniture can't be placed");

        Progress other = new Progress();
        other.coins = 1000;
        Home.buy(other, Home.Item.FERN);
        other.updatedAt = p.updatedAt + 1;
        Progress merged = Progress.merge(p, other);
        check(Home.owns(merged, Home.Item.BALL) && Home.owns(merged, Home.Item.FERN), "purchases on both devices survive a merge");
        check(Home.placed(merged, Home.Slot.PLANT) == Home.Item.FERN, "a merge keeps the newer room layout");
    }

    private static void testCatMood() {
        long day = 20763;
        Progress p = new Progress();
        Random rnd = new Random(1);
        int offered = 0;
        for (int i = 0; i < 2000; i++) {
            int coins = CatMood.rollGift(p, day, rnd);
            if (coins == 0) continue;
            offered++;
            check(coins >= CatMood.MIN_GIFT_COINS && coins <= CatMood.MAX_GIFT_COINS, "gifts stay within range");
            if (p.catGiftsToday < CatMood.MAX_GIFTS_PER_DAY) CatMood.collectGift(p, day, coins);
            check(p.catGiftsToday <= CatMood.MAX_GIFTS_PER_DAY, "the daily gift limit holds");
        }
        check(offered == CatMood.MAX_GIFTS_PER_DAY, "the cat stops offering at the daily limit");

        int tomorrow = 0;
        for (int i = 0; i < 200; i++) if (CatMood.rollGift(p, day + 1, rnd) > 0) tomorrow++;
        check(tomorrow > 0, "gifts come back the next day");
        CatMood.collectGift(p, day + 1, 7);
        check(p.catGiftsToday == 1 && p.catGiftDay == day + 1, "a new day restarts the count");

        Progress loaded = Progress.parse(p.serialize());
        check(loaded.catGiftsToday == 1 && loaded.catGiftDay == day + 1, "gift limits survive a save and load");

        Progress older = new Progress();
        older.catGiftDay = day;
        older.catGiftsToday = 3;
        Progress merged = Progress.merge(p, older);
        check(merged.catGiftDay == day + 1 && merged.catGiftsToday == 1, "a merge keeps the latest gift day");

        Progress lapsed = new Progress();
        lapsed.streak = new StreakLogic.State(0, 5, day - 9, 0);
        boolean missed = false;
        for (int i = 0; i < 100; i++) missed |= CatMood.line(lapsed, day, rnd).contains("missed");
        check(missed, "the cat notices a lost streak");

        Progress streaking = new Progress();
        streaking.streak = new StreakLogic.State(4, 4, day - 1, 0);
        boolean nudged = false;
        for (int i = 0; i < 100; i++) nudged |= CatMood.line(streaking, day, rnd).contains("4-day streak");
        check(nudged, "the cat nudges a live streak");
        check(!CatMood.line(new Progress(), day, rnd).isEmpty(), "a new player still gets a line");
    }

    private static void testQuests() {
        check(Quest.weekOf(20759) == Quest.weekOf(20765), "Monday to Sunday is one week"); // 2026-11-02 to 11-08
        check(Quest.weekOf(20766) == Quest.weekOf(20765) + 1, "a new week starts on Monday");
        check(Quest.daysLeft(20759) == 7 && Quest.daysLeft(20765) == 1, "days left counts today");

        for (long week = 2000; week < 2300; week++) {
            List<Quest> quests = Quest.forWeek(week);
            Set<Quest.Kind> kinds = new HashSet<>();
            for (Quest q : quests) kinds.add(q.kind);
            check(quests.size() == 3 && kinds.size() == 3, "three different quests each week");
            check(quests.get(0).title().equals(Quest.forWeek(week).get(0).title()), "quests are deterministic");
        }

        Progress p = new Progress();
        p.level = 10;
        p.totalWordsFound = 500;
        p.duelWins = 7;
        p.dailiesCompleted = 9;
        long monday = 20759, week = Quest.weekOf(monday);
        check(Quest.rollOver(p, monday), "a new week rolls over");
        check(!Quest.rollOver(p, monday + 3), "the same week doesn't");
        for (Quest q : Quest.forWeek(week)) check(q.progress(p) == 0, "every quest starts the week at zero");
        check(Quest.claimable(p).isEmpty(), "nothing to claim yet");

        Quest first = Quest.forWeek(week).get(0);
        check(!first.claim(p), "an unfinished quest pays nothing");
        p.level += 100;
        p.totalWordsFound += 1000;
        p.duelWins += 100;
        p.dailiesCompleted += 100;
        check(first.progress(p) == first.goal, "quest progress is capped at the goal");
        check(Quest.claimable(p).size() == 3, "all three are ready");
        int before = p.coins;
        check(first.claim(p), "a finished quest can be claimed");
        check(p.coins == before + first.coins, "the reward is paid");
        check(!first.claim(p), "a quest can't be claimed twice");

        Progress loaded = Progress.parse(p.serialize());
        check(first.isClaimed(loaded) && loaded.questWeek == week, "quests survive a save and load");
        check(first.progress(loaded) == first.goal, "quest progress survives a save and load");

        Progress otherDevice = Progress.parse(p.serialize());
        otherDevice.questClaimed = 0;
        Quest.forWeek(week).get(1).claim(otherDevice);
        otherDevice.updatedAt = p.updatedAt + 1;
        Progress merged = Progress.merge(p, otherDevice);
        check(first.isClaimed(merged) && Quest.forWeek(week).get(1).isClaimed(merged), "claims on both devices survive a merge");

        Progress lastWeek = Progress.parse(p.serialize());
        Quest.rollOver(p, monday + 7);
        check(Quest.claimable(p).isEmpty() && p.questClaimed == 0, "a new week starts fresh");
        p.updatedAt = 1;
        lastWeek.updatedAt = 2;
        check(Progress.merge(p, lastWeek).questWeek == week + 1, "a merge keeps the later week, even from an older save");
    }

    private static void testAchievementRewards() {
        Progress p = new Progress();
        check(!Achievement.DUEL_1.claim(p), "a locked achievement pays nothing");
        check(p.coins == 0, "a failed claim leaves coins alone");

        p.duelWins = 1;
        check(Achievement.claimable(p).size() == 1, "an unlocked achievement is ready to claim");
        check(Achievement.DUEL_1.claim(p), "claiming an unlocked achievement works");
        check(p.coins == Achievement.Tier.BRONZE.coins, "the reward matches the tier");
        check(!Achievement.DUEL_1.claim(p), "a reward can't be claimed twice");
        check(p.coins == Achievement.Tier.BRONZE.coins, "a second claim pays nothing");
        check(Achievement.claimable(p).isEmpty(), "nothing is left to claim");

        Progress loaded = Progress.parse(p.serialize());
        check(Achievement.DUEL_1.isClaimed(loaded), "claims survive a save and load");
        check(Achievement.claimable(loaded).isEmpty(), "a reloaded save can't claim again");

        Progress old = Progress.parse("level=40\nduels=0\n");
        check(old.claimedAchievements == 0, "a save from before rewards has nothing claimed");
        check(Achievement.LEVELS_10.isClaimable(old), "old saves can still claim what they earned");

        Progress other = new Progress();
        other.duelWins = 1;
        other.updatedAt = p.updatedAt + 1;
        Progress merged = Progress.merge(p, other);
        check(Achievement.DUEL_1.isClaimed(merged), "a claim on either device survives a cloud merge");

        Set<Integer> bits = new HashSet<>();
        Progress every = new Progress();
        every.level = 1000;
        every.totalWordsFound = 100_000;
        every.dailiesCompleted = 1000;
        every.bestTimed = 100_000;
        every.duelWins = 1000;
        every.streak = new StreakLogic.State(1000, 1000, 0, 0);
        every.coins = 100_000;
        for (Shop.Item item : Shop.Item.values()) Shop.buy(every, item);
        for (Achievement a : Achievement.values()) {
            long before = every.claimedAchievements;
            check(a.claim(every), a + " can be claimed");
            check(bits.add(Long.numberOfTrailingZeros(every.claimedAchievements ^ before)), a + " has its own id");
        }
    }

    private static void testShareCard() {
        check(ShareCard.dailyNumber(20763) == 1, "launch day is daily #1");
        check(ShareCard.dailyNumber(20765) == 3, "daily number counts up by day");
        check(ShareCard.dailyNumber(20000) == 1, "a clock set before launch still gets a valid number");

        String perfect = ShareCard.daily(20764, 3, 12, 0, 240);
        check(perfect.startsWith("Bloop Daily #2\n"), "share card leads with the puzzle number");
        check(perfect.contains("⭐⭐⭐  No hints!"), "share card shows stars and no hints");
        check(perfect.contains("12-day streak"), "share card shows the streak");
        check(perfect.endsWith("Score 240"), "share card ends with the score");
        check(ShareCard.daily(20764, 2, 1, 1, 90).contains("⭐⭐  1 hint\n"), "one hint is singular");
        check(ShareCard.daily(20764, 1, 1, 3, 90).contains("⭐  3 hints\n"), "several hints are plural");
    }

    private static void testDictionary(Dictionary dict) {
        String[] mustHave = {"class", "assist", "pass", "hello", "cat", "plate", "scunthorpe".substring(0, 0) + "grass"};
        for (String w : mustHave) check(dict.isWord(w), "innocent word kept: " + w);
        String[] mustNotHave = {"shit", "fuck", "cunt", "whore", "nigger", "bitch"};
        for (String w : mustNotHave) check(!dict.isWord(w), "offensive word removed: " + w);
        check(!dict.isWord("zzzzz"), "garbage rejected");
        check(!dict.isWord("at"), "2-letter words rejected");
        check(!dict.isWord("abcdefgh"), "8-letter words rejected");
    }

    private static void testFixedLevels(LevelGenerator gen, Dictionary dict) {
        for (int n = 1; n <= FixedLevels.COUNT; n++) {
            Level l = gen.forProgression(n);
            check(l.kind == Level.Kind.FIXED, "level " + n + " is FIXED");
            validate(l, dict, false);
        }
    }

    private static void testProgression(LevelGenerator gen, Dictionary dict, int from, int to) {
        long total = 0;
        int count = 0;
        for (int n = from; n <= to; n++) {
            Level a = gen.forProgression(n);
            Level b = gen.forProgression(n);
            check(a.fingerprint() == b.fingerprint(), "level " + n + " deterministic");
            validate(a, dict, true);
            LevelGenerator.Spec spec = LevelGenerator.specForLevel(n);
            check(a.letters.length() == spec.letterCount, "level " + n + " letter count " + a.letters.length());
            total += a.placements.size();
            count++;
        }
        System.out.println("Progression " + from + ".." + to + ": avg " + (total / (double) count) + " grid words per level");
    }

    private static void testDaily(LevelGenerator gen, Dictionary dict, long startDay, int days) {
        Set<Long> prints = new HashSet<>();
        for (long d = startDay; d < startDay + days; d++) {
            Level a = gen.forDaily(d);
            check(a.fingerprint() == gen.forDaily(d).fingerprint(), "daily " + d + " deterministic");
            check(a.kind == Level.Kind.DAILY, "daily " + d + " kind");
            validate(a, dict, true);
            prints.add(a.fingerprint());
        }
        check(prints.size() == days, "daily puzzles are all different (" + prints.size() + "/" + days + ")");
    }

    private static void testDuel(LevelGenerator gen, Dictionary dict) {
        for (long seed : new long[]{1L, 42L, 0xCAFEBABEL, -7L, Long.MAX_VALUE}) {
            Level a = gen.forDuel(seed);
            check(a.fingerprint() == gen.forDuel(seed).fingerprint(), "duel " + seed + " deterministic");
            validate(a, dict, true);
        }
    }


    // ------------------------------------------------------------------ game logic

    private static void testGameSession(LevelGenerator gen) {
        // Level 1 (ACT / CAT) by guessing.
        GameSession s = new GameSession(gen.forProgression(1), 7);
        check(s.submit("at") == GameSession.Result.TOO_SHORT, "2 letters too short");
        check(s.submit("zzz") == GameSession.Result.NOT_A_WORD, "garbage not a word");
        check(s.submit("CAT") == GameSession.Result.FOUND_GRID, "grid word found (case-insensitive)");
        check(s.submit("cat") == GameSession.Result.ALREADY_FOUND, "repeat grid word");
        check(!s.isComplete(), "not complete after one word");
        check(s.submit("act") == GameSession.Result.FOUND_GRID, "second grid word");
        check(s.isComplete(), "complete after all grid words");
        check(s.gridPoints() == 60, "grid points 30+30 = " + s.gridPoints());

        // Bonus words earn coins once.
        Level three = gen.forProgression(3);
        GameSession b = new GameSession(three, 7);
        check(!three.bonusWords.isEmpty(), "level 3 has a bonus word");
        String bonus = three.bonusWords.get(0);
        check(b.submit(bonus) == GameSession.Result.FOUND_BONUS, "bonus word found");
        check(b.submit(bonus) == GameSession.Result.ALREADY_FOUND, "bonus word only once");
        check(b.coinsEarned() == 2 && b.bonusPoints() == 5 * bonus.length(), "bonus coins/points");

        // Other real words are recognised, but pay nothing.
        check(!three.extraWords.isEmpty(), "level 3 has extra words");
        int coinsBefore = b.coinsEarned();
        check(b.submit(three.extraWords.get(0)) == GameSession.Result.NOT_IN_PUZZLE, "extra word is not called wrong");
        check(b.coinsEarned() == coinsBefore, "extra word pays nothing");

        // Hints on level 1 after finding CAT: two hints complete ACT through crossing logic.
        GameSession h = new GameSession(gen.forProgression(1), 3);
        h.submit("cat");
        check(h.revealHint() != null, "hint 1");
        check(h.revealHint() != null, "hint 2");
        check(h.isComplete(), "hints completed the remaining word");
        check(h.revealHint() == null, "no hint left when everything is revealed");
        check(h.hintsUsed() == 2, "hint count " + h.hintsUsed());
        check(h.score(0) >= 0, "score never negative");

        // Hints alone can solve any generated level.
        for (int n = 4; n <= 40; n++) {
            Level l = gen.forProgression(n);
            GameSession g = new GameSession(l, n);
            int guard = 0;
            while (!g.isComplete() && guard++ < 200) {
                if (g.revealHint() == null) break;
            }
            check(g.isComplete(), "level " + n + " solvable purely by hints");
        }
    }

    private static void testStreaks() {
        StreakLogic.State s = StreakLogic.State.fresh();
        s = StreakLogic.onDailyCompleted(s, 100);
        check(s.streak == 1, "first daily starts streak");
        check(StreakLogic.onDailyCompleted(s, 100) == s, "same day is idempotent");
        for (long d = 101; d <= 106; d++) s = StreakLogic.onDailyCompleted(s, d);
        check(s.streak == 7 && s.freezes == 1, "7-day streak earns a freeze (streak " + s.streak + ", freezes " + s.freezes + ")");
        check(StreakLogic.displayStreak(s, 107) == 7, "streak alive the next day");
        check(StreakLogic.atRisk(s, 107), "at risk when today not yet done");

        // Miss exactly one day (107), play 108: freeze is consumed and the streak continues.
        StreakLogic.State f = StreakLogic.onDailyCompleted(s, 108);
        check(f.streak == 8 && f.freezes == 0, "freeze saves a one-day gap");
        // Without a freeze the same gap resets.
        StreakLogic.State noFreeze = new StreakLogic.State(5, 5, 100, 0);
        check(StreakLogic.onDailyCompleted(noFreeze, 102).streak == 1, "gap without freeze resets");
        check(StreakLogic.onDailyCompleted(s, 110).streak == 1, "two missed days reset even with a freeze");
        check(StreakLogic.displayStreak(s, 110) == 0, "dead streak displays 0");
        check(f.best == 8, "best streak tracked");

        StreakLogic.State capped = new StreakLogic.State(13, 13, 200, StreakLogic.MAX_FREEZES);
        check(StreakLogic.onDailyCompleted(capped, 201).freezes == StreakLogic.MAX_FREEZES, "freezes capped");
    }

    private static void testProgress() {
        Progress p = new Progress();
        p.level = 42; p.coins = 310; p.hintTokens = 5; p.totalWordsFound = 999; p.dailiesCompleted = 12;
        p.adsRemoved = true; p.updatedAt = 1_700_000_000_000L;
        p.streak = new StreakLogic.State(9, 21, 20400, 2);
        Progress q = Progress.parse(p.serialize());
        check(q.serialize().equals(p.serialize()), "progress round-trips");
        check(Progress.parse("garbage\nlevel=abc\n").level == 1, "corrupt save falls back to defaults");

        Progress a = new Progress(); a.level = 50; a.coins = 10; a.updatedAt = 100; a.streak = new StreakLogic.State(3, 9, 500, 0);
        Progress b = new Progress(); b.level = 30; b.coins = 99; b.updatedAt = 200; b.adsRemoved = true;
        b.streak = new StreakLogic.State(1, 4, 400, 1);
        Progress m = Progress.merge(a, b);
        check(m.level == 50, "merge keeps highest level");
        check(m.coins == 99, "merge takes coins from the newer save");
        check(m.adsRemoved, "purchases are sticky");
        check(m.streak.lastDay == 500 && m.streak.streak == 3 && m.streak.best == 9, "merge keeps the more recent streak and best of both");
        check(Progress.merge(a, b).serialize().equals(Progress.merge(b, a).serialize()), "merge is order-independent");
    }

    private static void testDuelBot(LevelGenerator gen) {
        Level l = gen.forDuel(1234L);
        java.util.List<DuelBot.Event> x = DuelBot.script(l, 1234L, 0.6, 90);
        java.util.List<DuelBot.Event> y = DuelBot.script(l, 1234L, 0.6, 90);
        check(x.size() == y.size() && !x.isEmpty(), "bot script deterministic and non-empty");
        int last = -1;
        boolean sorted = true;
        for (int i = 0; i < x.size(); i++) {
            sorted &= x.get(i).atSecond >= last && x.get(i).atSecond < 90 && x.get(i).word.equals(y.get(i).word);
            last = x.get(i).atSecond;
        }
        check(sorted, "bot events ordered and inside the time limit");
        check(DuelBot.scoreAt(x, 0) <= DuelBot.scoreAt(x, 45) && DuelBot.scoreAt(x, 45) <= DuelBot.scoreAt(x, 90), "bot score monotonic");
        check(DuelBot.script(l, 1234L, 1.0, 90).size() >= DuelBot.script(l, 1234L, 0.0, 90).size(), "higher skill finds at least as many words");
    }


    private static void testShop() {
        Progress p = new Progress();
        p.coins = 100;
        Shop.Item bell = Shop.Item.BELL, crown = Shop.Item.CROWN;
        check(!Shop.buy(p, crown), "cannot buy what you cannot afford");
        check(p.coins == 100 && !Shop.owns(p, crown), "failed purchase changes nothing");
        check(Shop.buy(p, bell), "can buy an affordable item");
        check(p.coins == 100 - bell.price && Shop.owns(p, bell), "purchase charges coins and grants the item");
        check(Shop.isWearing(p, bell), "a new purchase is worn straight away");
        check(!Shop.buy(p, bell) && p.coins == 100 - bell.price, "cannot pay twice for the same item");
        Shop.wear(p, crown);
        check(Shop.wearing(p, Shop.Slot.HEAD) == null, "cannot wear an item you do not own");

        Progress round = Progress.parse(p.serialize());
        check(Shop.owns(round, bell) && Shop.isWearing(round, bell), "shop state survives a save");
        Progress other = new Progress();
        other.coins = 500;
        check(Shop.buy(other, crown), "second device buys another item");
        Progress merged = Progress.merge(p, other);
        check(Shop.owns(merged, bell) && Shop.owns(merged, crown), "purchases from both devices are kept on merge");

        Shop.takeOff(round, Shop.Slot.NECK);
        check(Shop.wearing(round, Shop.Slot.NECK) == null && Shop.owns(round, bell), "taking off keeps ownership");

        java.util.Set<Integer> ids = new java.util.HashSet<>();
        java.io.File dir = new java.io.File("../app/src/main/assets/cat");
        for (Shop.Item i : Shop.Item.values()) {
            check(ids.add(i.id), "unique shop id: " + i);
            check(i.id > 0 && i.id < 63, "shop id fits the save bitmask: " + i);
            if (dir.isDirectory()) check(new java.io.File(dir, "acc_" + i.key + ".png").isFile(), "art exists for " + i);
        }
    }

    /** Word length, word count and grid limit must only ever climb. */
    private static void testCurve(LevelGenerator gen) {
        LevelGenerator.Spec prev = LevelGenerator.specForLevel(4);
        for (int n = 5; n <= 400; n++) {
            LevelGenerator.Spec s = LevelGenerator.specForLevel(n);
            check(s.letterCount >= prev.letterCount && s.maxGrid >= prev.maxGrid, "difficulty never drops at level " + n);
            check(s.letterCount > prev.letterCount || s.targetWords >= prev.targetWords, "word count only drops when words get longer, level " + n);
            prev = s;
        }
        check(LevelGenerator.specForLevel(400).letterCount == 8 && LevelGenerator.specForLevel(400).maxGrid == 10, "curve reaches 8 letters on a 10x10 grid");
        for (int n : new int[]{200, 300, 400}) {
            Level l = gen.forProgression(n);
            check(l.letters.length() == 8, "late level uses 8 letters: " + n);
            check(l.placements.size() >= 8, "late level has plenty of words: " + n + " has " + l.placements.size());
        }
    }

    // ------------------------------------------------------------------ validation

    /** Verifies every structural rule of a level. */
    private static void validate(Level l, Dictionary dict, boolean requireCommon) {
        String tag = l.id + ": ";
        int[] counts = Dictionary.letterCounts(l.letters);

        check(l.letters.length() >= 3 && l.letters.length() <= Dictionary.MAX_LEN, tag + "wheel size");
        check(l.rows <= LevelGenerator.MAX_ROWS && l.cols <= LevelGenerator.MAX_COLS || l.kind == Level.Kind.FIXED,
                tag + "grid size " + l.rows + "x" + l.cols);
        check(l.placements.size() >= 2, tag + "has >= 2 words");

        Set<String> seen = new HashSet<>();
        for (Level.Placement p : l.placements) {
            check(dict.isWord(p.word), tag + "grid word in dictionary: " + p.word);
            check(Dictionary.canBuild(p.word, counts), tag + "grid word buildable from wheel: " + p.word);
            check(seen.add(p.word), tag + "no duplicate grid word: " + p.word);
            if (requireCommon) check(dict.isCommon(p.word), tag + "grid word is common: " + p.word);
            int endR = p.rowAt(p.word.length() - 1);
            int endC = p.colAt(p.word.length() - 1);
            check(p.row >= 0 && p.col >= 0 && endR < l.rows && endC < l.cols, tag + "in bounds: " + p);
        }

        // Grid must contain exactly the placed words: every run of 2+ letters is a placement.
        char[][] g = l.grid();
        int runs = 0;
        for (int r = 0; r < l.rows; r++) {
            for (int c = 0; c < l.cols; c++) {
                if (g[r][c] == '\0') continue;
                if ((c == 0 || g[r][c - 1] == '\0') && c + 1 < l.cols && g[r][c + 1] != '\0') {
                    runs++;
                    check(hasPlacement(l, r, c, true), tag + "horizontal run at " + r + "," + c + " is a placed word");
                }
                if ((r == 0 || g[r - 1][c] == '\0') && r + 1 < l.rows && g[r + 1][c] != '\0') {
                    runs++;
                    check(hasPlacement(l, r, c, false), tag + "vertical run at " + r + "," + c + " is a placed word");
                }
            }
        }
        check(runs == l.placements.size(), tag + "no accidental words (" + runs + " runs vs " + l.placements.size() + " words)");

        // Fully connected via shared cells.
        check(connected(l), tag + "grid is connected");

        // Overlapping cells agree on letters (grid() would silently overwrite otherwise).
        for (Level.Placement p : l.placements) {
            for (int i = 0; i < p.word.length(); i++) {
                check(g[p.rowAt(i)][p.colAt(i)] == p.word.charAt(i), tag + "letter agreement in " + p);
            }
        }

        // Every valid buildable word is either in the grid, a bonus word, or a no-reward extra.
        List<String> expected = new ArrayList<>(dict.subWords(l.letters));
        for (Level.Placement p : l.placements) expected.remove(p.word);
        List<String> accounted = new ArrayList<>(l.bonusWords);
        accounted.addAll(l.extraWords);
        java.util.Collections.sort(accounted);
        java.util.Collections.sort(expected);
        check(expected.equals(accounted), tag + "every other word is a bonus or an extra");
        check(l.bonusWords.size() <= LevelGenerator.MAX_BONUS, tag + "bonus words capped, has " + l.bonusWords.size());
        for (String w : l.bonusWords) check(dict.isCommon(w), tag + "bonus word is a common word: " + w);
    }

    private static boolean hasPlacement(Level l, int r, int c, boolean horizontal) {
        for (Level.Placement p : l.placements) {
            if (p.row == r && p.col == c && p.horizontal == horizontal) return true;
        }
        return false;
    }

    private static boolean connected(Level l) {
        int n = l.placements.size();
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(0);
        seen[0] = true;
        int reached = 1;
        while (!q.isEmpty()) {
            int a = q.poll();
            for (int b = 0; b < n; b++) {
                if (!seen[b] && share(l.placements.get(a), l.placements.get(b))) {
                    seen[b] = true;
                    reached++;
                    q.add(b);
                }
            }
        }
        return reached == n;
    }

    private static boolean share(Level.Placement a, Level.Placement b) {
        for (int i = 0; i < a.word.length(); i++) {
            for (int j = 0; j < b.word.length(); j++) {
                if (a.rowAt(i) == b.rowAt(j) && a.colAt(i) == b.colAt(j)) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + message);
        }
    }

    private static Dictionary loadDictionary() throws IOException {
        ClassLoader cl = GeneratorSelfTest.class.getClassLoader();
        try (InputStream words = cl.getResourceAsStream("enable1.txt");
             InputStream common = cl.getResourceAsStream("common.txt");
             InputStream bad = cl.getResourceAsStream("badwords.txt")) {
            if (words == null || common == null || bad == null) throw new IOException("word list resources missing");
            return Dictionary.load(words, common, bad);
        }
    }

    private static void printLevel(Level l) {
        System.out.println("== " + l.id + " (" + l.kind + ") wheel: " + l.letters.toUpperCase()
                + "  grid " + l.rows + "x" + l.cols + "  words: " + l.placements.size()
                + "  bonus: " + l.bonusWords.size() + "  extra: " + l.extraWords.size());
        char[][] g = l.grid();
        for (char[] row : g) {
            StringBuilder sb = new StringBuilder("   ");
            for (char c : row) sb.append(c == '\0' ? " ." : " " + Character.toUpperCase(c));
            System.out.println(sb);
        }
        StringBuilder words = new StringBuilder("   ");
        for (Level.Placement p : l.placements) words.append(p.word).append(' ');
        System.out.println(words);
    }
}
