package com.kun.pixelwords.generator;

import com.kun.pixelwords.game.Achievement;
import com.kun.pixelwords.game.AdPolicy;
import com.kun.pixelwords.game.Comeback;
import com.kun.pixelwords.game.Bond;
import com.kun.pixelwords.game.Boss;
import com.kun.pixelwords.game.BossFight;
import com.kun.pixelwords.game.CatMood;
import com.kun.pixelwords.game.DailyArchive;
import com.kun.pixelwords.game.DailySpin;
import com.kun.pixelwords.game.Definitions;
import com.kun.pixelwords.game.DuelBot;
import com.kun.pixelwords.game.FishHunt;
import com.kun.pixelwords.game.GameSession;
import com.kun.pixelwords.game.Home;
import com.kun.pixelwords.game.Journey;
import com.kun.pixelwords.game.MegaBoard;
import com.kun.pixelwords.game.NineLives;
import com.kun.pixelwords.game.PiggyJar;
import com.kun.pixelwords.game.PowerUps;
import com.kun.pixelwords.game.RedeemCode;
import com.kun.pixelwords.game.Region;
import com.kun.pixelwords.game.Reward;
import com.kun.pixelwords.game.Season;
import com.kun.pixelwords.game.BossRush;
import com.kun.pixelwords.game.Shop;
import com.kun.pixelwords.game.WordTower;
import com.kun.pixelwords.game.Trophies;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Purchases;
import com.kun.pixelwords.game.Quest;
import com.kun.pixelwords.game.Scoring;
import com.kun.pixelwords.game.ShareCard;
import com.kun.pixelwords.game.StreakLogic;
import com.kun.pixelwords.game.StreakMilestone;
import com.kun.pixelwords.game.TileMods;
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
        testNextGoal();
        testStreakRescue();
        testAchievementCatalogue();
        testProgress();
        testPurchases();
        testAdPolicy();
        testDuelBot(gen);
        testShop();
        testShareCard();
        testAchievements();
        testAchievementRewards();
        testJourney(gen, dict);
        testQuests();
        testCatMood();
        testHome();
        testLevelsUnchanged(gen);
        testBonusWordsAndJar(gen);
        testTileMods(gen);
        testPowerUps(gen);
        testMegaBoard(gen);
        testNineLives(gen);
        testBreedsAndThemes();
        testRewards();
        testDailySpin();
        testOldSaveExtras();
        testTrophies();
        testCoinCap();
        testWordTower(gen);
        testBossRush(gen);
        testSeason();
        testFishHunt(gen);
        testNewSaveFields();
        testStreakMilestones();
        testComeback();
        testBond();
        testDefinitions(gen);
        testDailyArchive();
        testBosses(gen);
        testRegions();
        testRedeemCodes();
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
        for (int i = 0; i < Achievement.ALL_ITEMS.goal; i++) {
            check(!Achievement.ALL_ITEMS.isUnlocked(p), "the last accessory is still needed");
            Shop.buy(p, Shop.Item.values()[i]);
        }
        check(Achievement.ALL_ITEMS.isUnlocked(p), "owning twelve accessories unlocks the collector goal");

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

    /** Polls until the condition holds or five seconds pass, so a slow machine can't fail a check by timing alone. */
    private static boolean waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) return false;
            Thread.sleep(5);
        }
        return true;
    }

    private static void waitForPlayers(LanMatch m, int count) throws InterruptedException {
        waitUntil(() -> m.players().size() == count);
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
        waitUntil(() -> {
            List<LanPlayer> r = LanPlayer.ranked(b.players());
            return r.size() == 3 && r.get(0).score == 50 && r.get(1).score == 30;
        });
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
            check(Home.itemsIn(slot).size() >= 3, "every slot has at least three items to choose from");
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
        for (int level = 1; level < every.level; level++) every.recordStars(level, 3);
        every.bestTower = 100;
        every.bestRush = 100;
        every.homeOwned = -1L;
        every.themesOwned = -1L;
        every.breedsOwned = -1L;
        every.redeemedCodes = 1;
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
        check(perfect.startsWith("Paws & Puzzles Daily #2\n"), "share card leads with the puzzle number");
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

        // Bonus words score once.
        Level three = gen.forProgression(3);
        GameSession b = new GameSession(three, 7);
        check(!three.bonusWords.isEmpty(), "level 3 has a bonus word");
        String bonus = three.bonusWords.get(0);
        check(b.submit(bonus) == GameSession.Result.FOUND_BONUS, "bonus word found");
        check(b.submit(bonus) == GameSession.Result.ALREADY_FOUND, "bonus word only once");
        check(b.coinsEarned() == 0 && b.bonusPoints() == 5 * bonus.length(), "bonus words score but pay through the jar");

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

    private static void testAchievementCatalogue() {
        java.util.Set<String> titles = new java.util.HashSet<>();
        java.util.Set<Integer> bits = new java.util.HashSet<>();
        boolean fits = true, sorted = true;
        for (Achievement a : Achievement.values()) {
            titles.add(a.title);
            fits &= a.category != null && a.goal > 0;
        }
        check(titles.size() == Achievement.values().length, "every achievement has its own title");
        check(fits, "every achievement has a category and a goal");
        check(Achievement.values().length > 40, "there are over forty achievements");
        for (Achievement.Category c : Achievement.Category.values()) {
            int n = 0;
            for (Achievement a : Achievement.values()) if (a.category == c) n++;
            check(n >= 4, c.label + " has several achievements");
        }
        Progress p = new Progress();
        p.level = 51;
        check(Achievement.LEVELS_50.isUnlocked(p) && !Achievement.LEVELS_100.isUnlocked(p), "levels count toward journey goals");
        check(Achievement.BOSS_1.isUnlocked(p) && !Achievement.BOSS_5.isUnlocked(p), "finished chapters count as bosses");
        p.recordStars(1, 3);
        p.recordStars(2, 3);
        check(Achievement.STARS_25.progress(p) == 2, "three-star levels are counted");
        p.bestTower = 6;
        check(Achievement.TOWER_5.isUnlocked(p) && !Achievement.TOWER_20.isUnlocked(p), "tower floors count");
        p.redeemedCodes = 1L << 3;
        check(Achievement.GIFT_CODE.isUnlocked(p), "redeeming a code counts");
        check(Achievement.GIFT_CODE.claim(p) && !Achievement.GIFT_CODE.claim(p), "a reward pays once");
    }

    private static void testStreakRescue() {
        StreakLogic.State broken = new StreakLogic.State(5, 5, 98, 0);
        check(StreakLogic.canRescue(broken, 100), "a 5-day streak missed one day with no freeze can be rescued");
        check(StreakLogic.displayStreak(broken, 100) == 0, "the broken streak reads as 0 before the rescue");
        StreakLogic.State saved = StreakLogic.withFreeze(broken);
        check(!StreakLogic.canRescue(saved, 100), "no second rescue once a freeze is held");
        check(StreakLogic.displayStreak(saved, 100) == 5, "the rescued streak is alive again");
        check(StreakLogic.onDailyCompleted(saved, 100).streak == 6, "solving the daily continues the streak");
        check(!StreakLogic.canRescue(new StreakLogic.State(2, 2, 98, 0), 100), "short streaks are not worth a rescue");
        check(!StreakLogic.canRescue(new StreakLogic.State(5, 5, 97, 0), 100), "two missed days cannot be rescued");
        check(StreakLogic.withFreeze(new StreakLogic.State(5, 5, 98, 3)).freezes == 3, "freezes stay capped");
    }

    private static void testNextGoal() {
        check(Journey.nextGoal(1).contains("19 more levels"), "level 1 counts 19 levels to the boss");
        check(Journey.nextGoal(19).startsWith("Next level is the boss"), "level 19 points at the boss");
        check(Journey.nextGoal(20).isEmpty(), "no goal line after a boss");
        check(Journey.nextGoal(21).contains("19 more levels"), "chapter 2 starts counting again");
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

    /** Levels are regenerated from their number, so any change here would rewrite every player's journey. */
    private static void testLevelsUnchanged(LevelGenerator gen) {
        long h = 17;
        for (int n = 1; n <= 200; n++) h = h * 31 + levelHash(gen.forProgression(n));
        for (int n = 4; n <= 240; n += 3) h = h * 31 + levelHash(gen.forHard(n));
        for (long d = 20400; d < 20430; d++) h = h * 31 + levelHash(gen.forDaily(d));
        for (long s = 1; s <= 30; s++) h = h * 31 + levelHash(gen.forDuel(s * 7919));
        // Changed on purpose on 2026-11-08: every chapter now climbs from easy to hard (LevelGenerator.specForLevel).
        check(h == -4102016199446747428L, "existing levels are unchanged: " + h);
    }

    private static long levelHash(Level l) {
        return l.fingerprint() * 31 + l.bonusWords.hashCode() * 31L + l.extraWords.hashCode();
    }

    private static void testBonusWordsAndJar(LevelGenerator gen) {
        Level three = gen.forProgression(3);
        check(!three.extraWords.isEmpty(), "level 3 has other real words");
        GameSession s = new GameSession(three, 7);
        String extra = three.extraWords.get(0);
        check(s.submit(extra) == GameSession.Result.FOUND_BONUS, "every real word is a bonus word");
        check(s.submit(extra) == GameSession.Result.ALREADY_FOUND, "a bonus word counts once");
        check(s.foundBonusWords().size() == 1, "bonus word is counted");
        check(s.bonusPoints() == 0, "words outside the picked bonus words don't change the score");
        check(s.submit("zzz") == GameSession.Result.NOT_A_WORD, "nonsense is still wrong");

        Progress p = new Progress();
        long day = 20500;
        PiggyJar.add(p, PiggyJar.CAPACITY - 1);
        check(!PiggyJar.isFull(p) && !PiggyJar.open(p, day), "a jar that isn't full stays shut");
        PiggyJar.add(p, 5);
        check(p.jarWords == PiggyJar.CAPACITY, "the jar stops at its capacity");
        for (int i = 0; i < PiggyJar.MAX_OPENS_PER_DAY; i++) {
            check(PiggyJar.open(p, day), "jar opens " + i);
            PiggyJar.add(p, PiggyJar.CAPACITY);
        }
        check(p.coins == PiggyJar.COINS * PiggyJar.MAX_OPENS_PER_DAY, "each open pays");
        check(!PiggyJar.open(p, day), "opens are limited each day");
        check(p.jarWords == PiggyJar.CAPACITY, "a jar that can't open keeps its words");
        check(PiggyJar.open(p, day + 1), "the limit resets the next day");
    }

    private static void testTileMods(LevelGenerator gen) {
        int frozenBoards = 0;
        for (int n = 20; n < 260; n += 7) {
            Level l = gen.forHard(n);
            TileMods mods = TileMods.forLevel(l);
            String id = "hard " + n;
            check(TileMods.forLevel(l).sparkWord() == mods.sparkWord(), id + ": twists are deterministic");
            check(mods.sparkWord() >= 0 && !mods.isFrozen(mods.sparkWord()), id + ": the spark word is never frozen");
            if (mods.frozenCount() > 0) frozenBoards++;
            for (int a = 0; a < l.placements.size(); a++) {
                if (!mods.isFrozen(a)) continue;
                boolean thawable = false;
                for (int b = 0; b < l.placements.size(); b++) {
                    if (a == b || !TileMods.crosses(l.placements.get(a), l.placements.get(b))) continue;
                    check(!mods.isFrozen(b), id + ": frozen words never cross");
                    thawable = true;
                }
                check(thawable, id + ": a frozen word has a word that thaws it");
            }
            check(solvableInAnyOrder(l, mods), id + ": can be solved by spelling words alone");
        }
        check(frozenBoards > 0, "some boards have frozen words");

        Level l = gen.forHard(40);
        TileMods mods = TileMods.forLevel(l);
        GameSession s = new GameSession(l, 1, mods);
        String spark = l.placements.get(mods.sparkWord()).word;
        check(s.sparkLive(), "the spark starts lit");
        check(s.submit(spark) == GameSession.Result.FOUND_GRID && s.sparkWon(), "spark word found in time");
        check(s.coinsEarned() >= TileMods.SPARK_COINS, "the spark pays");

        GameSession late = new GameSession(l, 1, mods);
        int spent = 0;
        for (int i = 0; i < l.placements.size() && spent < TileMods.SPARK_WINDOW; i++) {
            if (i == mods.sparkWord()) continue;
            if (late.submit(l.placements.get(i).word) == GameSession.Result.FOUND_GRID) spent++;
        }
        if (spent == TileMods.SPARK_WINDOW && !late.foundGridWords().contains(spark)) {
            check(!late.sparkLive(), "the spark goes out");
            late.submit(spark);
            check(!late.sparkWon(), "a late spark word pays nothing");
        }
        check(new GameSession(gen.forProgression(5), 1).coinsEarned() == 0, "plain boards have no twists");
    }

    /** Keeps spelling whatever isn't frozen. Proves frozen words can't deadlock a board. */
    private static boolean solvableInAnyOrder(Level l, TileMods mods) {
        GameSession s = new GameSession(l, 1, mods);
        for (int pass = 0; pass <= l.placements.size(); pass++) {
            for (int i = 0; i < l.placements.size(); i++) s.submit(l.placements.get(i).word);
        }
        return s.isComplete();
    }

    private static void testPowerUps(LevelGenerator gen) {
        Progress p = new Progress();
        check(!PowerUps.canUse(p, PowerUps.Kind.REVEAL_WORD) && !PowerUps.spend(p, PowerUps.Kind.REVEAL_WORD), "no token, no coins, no power-up");
        p.coins = 100;
        p.pickTokens = 1;
        check(PowerUps.spend(p, PowerUps.Kind.PICK_LETTER) && p.pickTokens == 0 && p.coins == 100, "a token is used before coins");
        check(PowerUps.spend(p, PowerUps.Kind.PICK_LETTER) && p.coins == 100 - PowerUps.Kind.PICK_LETTER.price, "then coins");

        Level l = gen.forProgression(30);
        GameSession s = new GameSession(l, 9);
        Level.Placement word = s.revealWord();
        check(word != null && s.foundGridWords().contains(word.word), "reveal word finds a word");
        check(s.hintsUsed() == PowerUps.Kind.REVEAL_WORD.hintCost, "reveal word counts as two hints");
        Level.Placement other = l.placements.get(0).word.equals(word.word) ? l.placements.get(1) : l.placements.get(0);
        int free = -1;
        for (int i = 0; i < other.word.length() && free < 0; i++) {
            if (!s.isRevealed(other.rowAt(i), other.colAt(i))) free = i;
        }
        int before = s.hintsUsed();
        check(s.revealCell(other.rowAt(free), other.colAt(free)) && s.hintsUsed() == before + 1, "pick a letter reveals that cell");
        check(!s.revealCell(other.rowAt(free), other.colAt(free)), "a showing cell can't be picked again");
        char[][] grid = l.grid();
        boolean foundEmpty = false;
        for (int r = 0; r < l.rows && !foundEmpty; r++) {
            for (int c = 0; c < l.cols && !foundEmpty; c++) {
                if (grid[r][c] == '\0') {
                    foundEmpty = true;
                    check(!s.revealCell(r, c), "an empty cell can't be picked");
                }
            }
        }
        check(s.revealWord() != null, "reveal word works again");
        int guard = 0;
        while (!s.isComplete() && guard++ < 50) s.revealWord();
        check(s.isComplete() && s.revealWord() == null, "reveal word stops when the board is done");
    }

    private static void testMegaBoard(LevelGenerator gen) {
        long t0 = System.currentTimeMillis();
        long slowest = 0;
        int fewest = Integer.MAX_VALUE;
        for (long week = 2900; week < 2920; week++) {
            long start = System.currentTimeMillis();
            Level l = gen.forMega(week);
            slowest = Math.max(slowest, System.currentTimeMillis() - start);
            fewest = Math.min(fewest, l.placements.size());
            check(l.rows <= LevelGenerator.MEGA_MAX_GRID && l.cols <= LevelGenerator.MEGA_MAX_GRID, "mega fits its limit");
            check(l.letters.length() == 8, "mega has eight letters");
            check(l.placements.size() < 63, "mega words fit the save's bitmask");
            check(gen.forMega(week).fingerprint() == l.fingerprint(), "mega is the same for everyone");
            check(solvableInAnyOrder(l, TileMods.forLevel(l)), "mega can be solved");
        }
        check(fewest >= 12, "mega boards feel big: fewest words " + fewest);
        System.out.println("Mega boards: fewest words " + fewest + ", slowest " + slowest + " ms, all "
                + (System.currentTimeMillis() - t0) + " ms");

        Level l = gen.forMega(2900);
        GameSession s = new GameSession(l, 1);
        s.submit(l.placements.get(0).word);
        s.submit(l.placements.get(3).word);
        GameSession resumed = new GameSession(l, 1);
        resumed.restore(s.foundMask());
        check(resumed.foundMask() == s.foundMask() && resumed.gridPoints() == s.gridPoints(), "a saved board picks up where it left off");

        TileMods mods = TileMods.forLevel(l);
        GameSession lit = new GameSession(l, 1, mods);
        lit.submit(l.placements.get(mods.sparkWord()).word);
        GameSession back = new GameSession(l, 1, mods);
        back.restore(lit.foundMask());
        check(lit.sparkWon() && back.sparkWon() && back.coinsEarned() == lit.coinsEarned(), "a won spark survives leaving the board");
        GameSession missed = new GameSession(l, 1, mods);
        int plain = 0;
        for (int i = 0; i < l.placements.size() && plain < TileMods.SPARK_WINDOW; i++) {
            if (i != mods.sparkWord() && missed.submit(l.placements.get(i).word) == GameSession.Result.FOUND_GRID) plain++;
        }
        GameSession returned = new GameSession(l, 1, mods);
        returned.restore(missed.foundMask());
        check(plain < TileMods.SPARK_WINDOW || !returned.sparkLive(), "leaving the board doesn't relight a missed spark");

        long friday = 20763, saturday = 20764; // 2026-11-06 and 07
        check(!MegaBoard.isOpen(friday) && MegaBoard.daysUntilOpen(friday) == 1, "shut on Friday");
        check(MegaBoard.isOpen(saturday) && MegaBoard.isOpen(saturday + 1), "open all weekend");
        check(!MegaBoard.isOpen(saturday + 2), "shut on Monday");
        Progress p = new Progress();
        MegaBoard.begin(p, saturday);
        p.megaFound = 5;
        MegaBoard.begin(p, saturday + 1);
        check(p.megaFound == 5, "the board carries across the weekend");
        check(MegaBoard.claim(p) && !MegaBoard.claim(p), "a weekend pays once");
        check(MegaBoard.isSolved(p, saturday + 1), "a solved board stays solved all weekend");
        check(p.coins == MegaBoard.COINS, "mega coins");
        MegaBoard.begin(p, saturday + 7);
        check(p.megaFound == 0 && !MegaBoard.isSolved(p, saturday + 7), "next weekend starts fresh");
        Progress late = new Progress();
        MegaBoard.begin(late, saturday);
        check(MegaBoard.claim(late) && !MegaBoard.isSolved(late, saturday + 7), "finishing on Monday leaves next weekend open");
    }

    private static void testNineLives(LevelGenerator gen) {
        for (int depth = 0; depth < 40; depth++) {
            Level l = gen.forLives(99, depth);
            check(l.rows <= LevelGenerator.MAX_ROWS && l.placements.size() >= 2, "lives board " + depth + " is playable");
            check(gen.forLives(99, depth).fingerprint() == l.fingerprint(), "lives boards come from the run seed");
        }
        NineLives run = new NineLives();
        check(run.lives() == NineLives.MAX_LIVES, "a run starts with nine lives");
        run.wrongWord();
        for (int i = 1; i <= NineLives.BOARDS_PER_LIFE; i++) {
            boolean won = run.boardCleared(100);
            check(won == (i == NineLives.BOARDS_PER_LIFE), "a life comes back every five boards");
        }
        check(run.lives() == NineLives.MAX_LIVES && run.boards() == 5 && run.score() == 500, "run totals");
        for (int i = 0; i < NineLives.BOARDS_PER_LIFE; i++) run.boardCleared(0);
        check(run.lives() == NineLives.MAX_LIVES, "lives never pass nine");
        for (int i = 0; i < NineLives.MAX_LIVES + 3; i++) run.wrongWord();
        check(run.isOver() && run.lives() == 0, "the run ends at no lives");

        Progress p = new Progress();
        check(!run.continueRun(p), "no coins, no continue");
        p.coins = NineLives.CONTINUE_COINS;
        check(run.continueRun(p) && p.coins == 0 && run.lives() == NineLives.CONTINUE_LIVES, "continue pays for lives");
        for (int i = 0; i < NineLives.CONTINUE_LIVES; i++) run.wrongWord();
        p.coins = NineLives.CONTINUE_COINS;
        check(!run.continueRun(p), "one continue a run");
        p.coins = 0;
        check(run.finish(p) && p.bestLives == run.score(), "a best score is recorded");
        check(p.coins == run.boards() * NineLives.COINS_PER_BOARD, "boards pay coins");
        check(!run.finish(p), "matching a best isn't a record");
    }

    private static void testBreedsAndThemes() {
        Progress p = new Progress();
        check(Shop.owns(p, Shop.Breed.ORANGE) && Shop.breed(p) == Shop.Breed.ORANGE, "every player has the first breed");
        check(!Shop.buy(p, Shop.Breed.TUXEDO), "too expensive");
        p.coins = 5000;
        check(Shop.buy(p, Shop.Breed.TUXEDO) && Shop.breed(p) == Shop.Breed.TUXEDO, "buying a breed uses it");
        check(!Shop.buy(p, Shop.Breed.TUXEDO), "a breed sells once");
        check(!Shop.buy(p, Shop.Breed.GALAXY), "the galaxy cat can't be bought");
        check(!Shop.buy(p, Shop.Item.GALAXY_CROWN), "exclusive items can't be bought");
        check(!Home.buy(p, Home.Item.ART_GALAXY), "season furniture can't be bought");
        p.breed = 99;
        check(Shop.breed(p) == Shop.Breed.ORANGE, "an unknown breed falls back");
        p.breed = Shop.Breed.SIAMESE.id;
        check(Shop.breed(p) == Shop.Breed.ORANGE, "a breed the player doesn't own falls back");

        check(Home.owns(p, Home.Theme.COZY) && Home.theme(p) == Home.Theme.COZY, "every player has the first room");
        check(Home.buy(p, Home.Theme.SPACE) && Home.theme(p) == Home.Theme.SPACE, "buying a room moves in");
        check(!Home.buy(p, Home.Theme.SPACE), "a room sells once");
        Home.use(p, Home.Theme.COZY);
        check(Home.theme(p) == Home.Theme.COZY, "rooms can be swapped");

        Set<Integer> ids = new HashSet<>();
        for (Shop.Item i : Shop.Item.values()) check(ids.add(i.id) && i.id <= Shop.MAX_ITEM_ID, "item id is unique and fits a bit: " + i);
        ids.clear();
        for (Home.Slot slot : Home.Slot.values()) {
            for (Home.Item i : Home.itemsIn(slot)) check(ids.add(i.ordinal()), "furniture is listed once: " + i);
        }
        check(ids.size() == Home.Item.values().length && Home.Item.values().length < 64, "furniture ids fit a bit");

        Progress old = new Progress();
        old.ownedItems = (1L << 13) - 2; // the first twelve accessories
        check(Achievement.ALL_ITEMS.isUnlocked(old), "new accessories don't take away the old achievement");
    }

    private static void testRewards() {
        Progress p = new Progress();
        check(Reward.coins(30).grant(p, 1).kind == Reward.Kind.COINS && p.coins == 30, "coins");
        Reward.hints(2).grant(p, 1);
        check(p.hintTokens == 5, "hints");
        Reward.powerUp(PowerUps.Kind.EXTRA_TIME, 2).grant(p, 1);
        check(p.timeTokens == 2, "power-ups");
        Reward crown = Reward.item(Shop.Item.GALAXY_CROWN);
        check(crown.grant(p, 1) == crown && Shop.owns(p, Shop.Item.GALAXY_CROWN), "items");
        int before = p.coins;
        Reward again = crown.grant(p, 1);
        check(again.kind == Reward.Kind.COINS && p.coins == before + Reward.DUPLICATE_COINS, "a duplicate item turns to coins");
        Reward cat = Reward.breed(Shop.Breed.GALAXY);
        check(cat.grant(p, 1) == cat && cat.grant(p, 1).kind == Reward.Kind.COINS, "breeds");
        Reward lamp = Reward.furniture(Home.Item.LAMP_STORM);
        check(lamp.grant(p, 1) == lamp && Home.owns(p, Home.Item.LAMP_STORM) && lamp.grant(p, 1).kind == Reward.Kind.COINS, "furniture");
    }

    private static void testDailySpin() {
        int total = 0;
        for (DailySpin.Segment s : DailySpin.segments()) total += s.odds;
        check(total == 1000, "the odds add up");
        check(DailySpin.segments().size() == 8, "eight slices");

        Progress p = new Progress();
        long day = 21000;
        DailySpin.Result first = DailySpin.spin(p, day, 42);
        check(first != null, "the first spin works");
        check(DailySpin.spin(p, day, 42) == null, "one spin a day");
        check(DailySpin.spin(p, day - 1, 42) == null, "moving the clock back gives no spin");
        Progress same = new Progress();
        check(DailySpin.spin(same, day, 42).segment == first.segment, "the result can't be rerolled");

        int[] landed = new int[8];
        int runs = 20000;
        for (int i = 0; i < runs; i++) {
            Progress q = new Progress();
            landed[DailySpin.spin(q, 30000 + i, i).segment]++;
        }
        for (int i = 0; i < 8; i++) {
            double wanted = DailySpin.segments().get(i).odds / 1000.0;
            double seen = landed[i] / (double) runs;
            check(Math.abs(seen - wanted) < 0.012, "slice " + i + " matches its odds: " + seen + " vs " + wanted);
        }

        Progress dry = new Progress();
        boolean sawLegendary = false;
        int legendaries = 0;
        for (int d = 1; d <= DailySpin.LEGENDARY_AFTER * 8; d++) {
            DailySpin.Result r = DailySpin.spin(dry, 40000 + d, 7);
            if (r.isLegendary()) legendaries++;
            check(dry.spinsDry < DailySpin.LEGENDARY_AFTER, "never more than sixty dry spins");
        }
        check(legendaries >= 4, "the guarantee pays out: " + legendaries);
        check(Shop.owns(dry, Shop.Breed.GALAXY) && Shop.owns(dry, Shop.Item.GALAXY_CROWN)
                && Shop.owns(dry, Shop.Item.GOLD_SHADES) && Shop.owns(dry, Shop.Item.PHOENIX_CAPE), "every legendary can be won");
        for (DailySpin.Segment s : DailySpin.segments()) {
            check(s.legendary || s.reward != null, "every slice has a prize");
            check(s.oddsLabel().endsWith("%"), "odds read as a percent");
        }
    }

    private static void testWordTower(LevelGenerator gen) {
        int lastMin = 0;
        for (int floor = 0; floor < 40; floor++) {
            LevelGenerator.Spec spec = LevelGenerator.specForTower(floor);
            check(spec.minWordLength >= lastMin, "tower words never get shorter: floor " + floor);
            lastMin = spec.minWordLength;
            for (long seed = 1; seed <= 3; seed++) {
                Level l = gen.forTower(seed, floor);
                check(l.placements.size() >= 3 && l.rows <= LevelGenerator.MAX_ROWS, "tower floor " + floor + " is playable");
                for (Level.Placement w : l.placements) check(w.word.length() >= spec.minWordLength, "floor " + floor + " has no short words: " + w.word);
            }
            check(gen.forTower(7, floor).fingerprint() == gen.forTower(7, floor).fingerprint(), "tower floors come from the run seed");
        }
        check(LevelGenerator.specForTower(30).minWordLength > LevelGenerator.specForTower(0).minWordLength, "the tower gets harder");

        WordTower t = new WordTower();
        check(t.floor() == 1 && t.lives() == WordTower.LIVES, "a climb starts on floor one");
        for (int i = 0; i < 10; i++) check(!t.boardCleared(50), "floors never give lives back");
        check(t.floor() == 11 && t.result() == 10, "floors climbed");
        for (int i = 0; i < WordTower.LIVES; i++) t.wrongWord();
        check(t.isOver(), "three slips and the climb is over");
        Progress p = new Progress();
        check(t.finish(p) && p.bestTower == 10 && p.coins == 10 * WordTower.COINS_PER_FLOOR, "a climb pays and records its height");
        check(Progress.parse(p.serialize()).bestTower == 10, "the best climb is saved");
    }

    private static void testBossRush(LevelGenerator gen) {
        Progress p = new Progress();
        p.level = Journey.LEVELS_PER_CHAPTER + 1;
        check(!BossRush.isOpen(p), "one boss isn't a rush");
        p.level = Journey.LEVELS_PER_CHAPTER * 5 + 1;
        check(BossRush.isOpen(p), "the rush opens after beating enough bosses");
        BossRush r = new BossRush(p, 42);
        check(r.bosses() == 5, "every beaten boss is in the rush");
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < r.bosses(); i++) {
            seen.add(r.chapter());
            check(Trophies.has(p, r.chapter()), "only beaten bosses come back");
            Level l = gen.forRush(42, i, Journey.lastLevel(r.chapter()));
            check(l.placements.size() >= 3, "rush board " + i + " is playable");
            r.boardCleared(100);
        }
        check(seen.size() == 5, "each boss comes once before any repeats");
        int first = new BossRush(p, 42).chapter();
        check(r.chapter() == first, "then the order starts over");
        for (int i = 0; i < BossRush.LIVES; i++) r.wrongWord();
        Progress q = Progress.parse(p.serialize());
        check(r.isOver() && r.finish(q) && q.bestRush == 5 && q.coins == 5 * BossRush.COINS_PER_BOSS, "a rush records bosses beaten");
        Progress merged = Progress.merge(p, q);
        check(merged.bestRush == 5, "the best rush survives a merge");
    }

    private static void testCoinCap() {
        Progress p = new Progress();
        p.coins = Progress.MAX_COINS - 10;
        p.addCoins(500);
        check(p.coins == Progress.MAX_COINS, "coins stop at the cap");
        Reward.coins(1000).grant(p, 1);
        check(p.coins == Progress.MAX_COINS, "rewards can't pass the cap");
        p.addCoins(-99);
        check(p.coins == Progress.MAX_COINS - 99, "spending still works at the cap");
        check(Progress.parse("coins=1000424\n").coins == Progress.MAX_COINS, "a save over the cap loads at the cap");
        p.coins = 2_000_000;
        check(Progress.parse(p.serialize()).coins == Progress.MAX_COINS, "and never saves over it");
    }

    private static void testTrophies() {
        Progress p = new Progress();
        check(Trophies.count(p) == 0 && Trophies.shown(p) == -1, "no trophies before the first boss");
        p.level = Journey.LEVELS_PER_CHAPTER;
        check(Trophies.count(p) == 0, "being on the boss level isn't beating it");
        p.level = Journey.LEVELS_PER_CHAPTER * 3 + 5;
        check(Trophies.count(p) == 3 && Trophies.shown(p) == 2, "a trophy per boss, and the newest is on show");
        Trophies.show(p, 0);
        check(Trophies.shown(p) == 0, "an older trophy can go on show");
        Trophies.show(p, 3);
        check(Trophies.shown(p) == 0, "a trophy not won yet can't");
        check(Trophies.shown(Progress.parse(p.serialize())) == 0, "the trophy on show is saved");
        p.trophyShown = 99;
        check(Trophies.shown(p) == 2, "a bad saved trophy falls back to the newest");
    }

    private static void testOldSaveExtras() {
        Progress p = Progress.parse("level=4\nexpPlace=2\nexpStart=500\npage0=7\nalbumClaimed=3\nowned=" + (1L << 41) + "\n");
        check(p.level == 4 && Shop.owns(p, Shop.Item.EXPLORER_HAT), "a save from before trips and the album were removed still loads");
        check(!p.serialize().contains("expPlace") && !p.serialize().contains("page0"), "their old fields are dropped on the next save");
        check(Shop.Item.EXPLORER_HAT.source == Shop.Source.COINS && Shop.Item.SCHOLAR_GLASSES.source == Shop.Source.COINS,
                "the old album prizes are in the shop now");
    }

    private static void testSeason() {
        long first = 20758;
        check(Season.indexOf(first) == 1 && Season.dayOf(first) == 0, "season one starts");
        check(Season.indexOf(first + 29) == 1 && Season.indexOf(first + 30) == 2, "seasons last 30 days");
        check(Season.daysLeft(first + 29) == 1, "last day of a season");
        check(Season.theme(1) == Season.Theme.FROST && Season.theme(7) == Season.Theme.FROST, "themes repeat");

        for (Season.Theme t : Season.Theme.values()) {
            check(t.accessory.source == Shop.Source.SEASON, t + " has an exclusive accessory");
            check(!Home.forSale(t.furniture), t + " has exclusive furniture");
        }
        for (long season = 1; season <= 6; season++) {
            for (int tier = 1; tier <= Season.TIERS; tier++) {
                check(Season.reward(season, tier, false) != null && Season.reward(season, tier, true) != null, "every tier pays");
            }
        }

        Progress p = new Progress();
        long day = first + 3;
        check(Season.rollOver(p, day) && !Season.rollOver(p, day), "a new season starts once");
        Season.addXp(p, day, Season.XP_PER_TIER * 2 + 5);
        check(Season.tiersReached(p) == 2 && Season.xpIntoTier(p) == 5, "xp becomes tiers");
        check(Season.claim(p, 3, false) == null, "a tier not reached doesn't pay");
        check(Season.claim(p, 2, true) == null, "premium needs unlocking");
        check(Season.claimableCount(p) == 2, "two free tiers ready");
        int coins = p.coins;
        check(Season.claim(p, 1, false) != null && p.coins > coins, "a tier pays");
        check(Season.claim(p, 1, false) == null, "a tier pays once");
        check(!Season.buyPremium(p), "premium costs coins");
        p.coins = Season.PREMIUM_COINS;
        check(Season.buyPremium(p) && p.coins == 0 && p.seasonPremium && !Season.buyPremium(p), "premium unlocks once");
        check(Season.claimableCount(p) == 3, "premium tiers open up");

        Season.addXp(p, day, Season.XP_PER_TIER * 100);
        check(Season.tiersReached(p) == Season.TIERS, "tiers stop at the top");
        Season.rollOver(p, first + 31);
        check(!Shop.owns(p, Shop.Item.ICE_CROWN) && !Home.owns(p, Home.Item.WINDOW_AURORA), "season exclusives stay locked");
        check(p.coins >= 500, "unclaimed rewards are paid when a season ends");
        check(Shop.Item.ICE_CROWN.source.locked() && Shop.Item.HEART_COLLAR.source.locked() && !Shop.Item.GALAXY_CROWN.source.locked(),
                "season and bond prizes are locked, spin prizes aren't");
        check(p.seasonId == 2 && p.seasonXp == 0 && !p.seasonPremium && p.seasonClaimedFree == 0, "the next season starts clean");

        Progress a = new Progress(), b = new Progress();
        a.seasonId = b.seasonId = 3;
        a.seasonXp = 500;
        b.seasonXp = 200;
        a.seasonClaimedFree = 2;
        b.seasonClaimedFree = 4;
        b.seasonPremium = true;
        a.updatedAt = 1;
        b.updatedAt = 2;
        Progress m = Progress.merge(a, b);
        check(m.seasonXp == 500 && m.seasonClaimedFree == 6 && m.seasonPremium, "one season merges upward");
        a.seasonId = 4;
        a.seasonXp = 10;
        a.seasonPremium = false;
        m = Progress.merge(a, b);
        check(m.seasonId == 4 && m.seasonXp == 10 && !m.seasonPremium, "a later season wins a merge");
    }

    private static void testFishHunt(LevelGenerator gen) {
        long first = 20758;
        check(!FishHunt.isLive(first + 11) && FishHunt.isLive(first + 12), "the hunt starts mid-season");
        check(FishHunt.isLive(first + 18) && !FishHunt.isLive(first + 19), "the hunt lasts a week");
        check(FishHunt.daysToChange(first + 11) == 1 && FishHunt.daysToChange(first + 18) == 1, "countdowns");
        check(FishHunt.daysToChange(first + 29) == 13, "the wait carries into the next season");

        Level l = gen.forProgression(50);
        List<int[]> fish = FishHunt.fishOn(l, 1);
        char[][] grid = l.grid();
        check(fish.size() >= 2 && fish.size() <= 3, "a board hides two or three fish");
        for (int[] cell : fish) check(grid[cell[0]][cell[1]] != '\0', "fish hide under letters");
        check(FishHunt.fishOn(l, 1).get(0)[0] == fish.get(0)[0], "fish stay put on a board");
        for (int i = 4; i < 200; i++) check(!FishHunt.fishOn(gen.forProgression(i % 40 + 1), 2).isEmpty(), "every board has fish");

        Progress p = new Progress();
        long day = first + 13;
        FishHunt.add(p, first, 5);
        check(p.fishCount == 0, "no fish outside the hunt");
        FishHunt.add(p, day, 12);
        check(p.fishCount == 12 && p.fishEvent == 1, "fish are banked");
        check(FishHunt.claimableCount(p) == 1, "one prize ready");
        check(FishHunt.claim(p, 0) != null && p.coins == 50, "a prize pays");
        check(FishHunt.claim(p, 0) == null && FishHunt.claim(p, 1) == null, "prizes pay once, and only when reached");
        FishHunt.add(p, day, 200);
        Season.rollOver(p, first + 31);
        check(Shop.owns(p, Shop.Item.FISH_HAT), "waiting prizes are paid when the season ends");
        check(p.fishCount == 0 && p.fishClaimed == 0, "the catch clears");
    }

    /** Every new field must survive a save, a cloud copy, and a merge in either order. */
    private static void testDefinitions(LevelGenerator gen) throws IOException {
        Definitions d;
        try (InputStream in = new java.io.FileInputStream("../app/src/main/assets/definitions.txt")) {
            d = Definitions.load(in);
        }
        check(d.size() > 5000, "the meanings file has loaded");
        check(d.find("stone") != null && !d.find("stone").isInflected(), "a plain word has a meaning");
        check(d.find("CATS") != null && d.find("cats").baseWord.equals("cat"), "an inflected word leads to its base word, in any case");
        check(d.find("and") != null && d.find("and").partOfSpeech.equals("conj"), "everyday words have hand-written meanings");
        check(d.find("zzzz") == null, "an unknown word has no meaning");

        Definitions broken = Definitions.load(new java.io.ByteArrayInputStream("bad line\nok\tnoun\tok\tfine\n".getBytes()));
        check(broken.size() == 1 && broken.find("ok") != null, "a broken line loses only itself");

        int words = 0, defined = 0;
        for (int level = 1; level <= 200; level++) {
            for (Level.Placement p : gen.forProgression(level).placements) {
                words++;
                if (d.find(p.word) != null) defined++;
            }
        }
        check(defined >= words * 0.98, "nearly every grid word has a meaning: " + defined + " of " + words);

        Set<String> bad = new HashSet<>();
        try (InputStream in = GeneratorSelfTest.class.getClassLoader().getResourceAsStream("badwords.txt");
             java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) bad.add(line.trim().toLowerCase());
        }
        boolean clean = true;
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("../app/src/main/assets/definitions.txt"))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                for (String w : line.split("\\t")[3].toLowerCase().split("[^a-z]+")) if (bad.contains(w)) clean = false;
            }
        }
        check(clean, "no meaning uses a word from the bad-word list");
    }

    /** Plays a whole fight, as badly as it can be played, and returns how many attacks landed. Fails if it ever gets stuck. */
    private static int fightOut(Level level, Boss.Attack[] attacks, int pace, long seed) {
        GameSession s = new GameSession(level, level.fingerprint());
        BossFight fight = new BossFight(attacks, pace, s);
        Random rnd = new Random(seed);
        int landed = 0, guard = 0;
        check(fight.health() == fight.maxHealth(), "a boss starts at full health");
        while (!s.isComplete() && guard++ < 500) {
            if (rnd.nextBoolean()) {
                s.submit("zzz");
                if (fight.onWrongWord() != null) landed++;
            }
            List<Level.Placement> reachable = new ArrayList<>();
            for (int i = 0; i < level.placements.size(); i++) {
                Level.Placement p = level.placements.get(i);
                if (s.foundGridWords().contains(p.word) || s.isFrozen(i)) continue;
                if (fight.spellable(p.word, fight.stolenLetter())) reachable.add(p);
            }
            check(!reachable.isEmpty(), "a word is always within reach of a boss fight");
            if (reachable.isEmpty()) return landed;
            int before = fight.health();
            check(s.submit(reachable.get(rnd.nextInt(reachable.size())).word) == GameSession.Result.FOUND_GRID, "a reachable word counts");
            check(fight.health() < before, "every word hurts the boss");
            if (fight.onBoardChanged() != null) landed++;
        }
        check(s.isComplete() && fight.health() == 0, "the fight always ends with the boss beaten");
        return landed;
    }

    private static void testRedeemCodes() {
        Progress p = new Progress();
        int hints = p.hintTokens;
        check(RedeemCode.redeem(p, "NOTACODE", 1).status == RedeemCode.Status.UNKNOWN && p.coins == 0, "a wrong code pays nothing");
        check(RedeemCode.redeem(p, "", 1).status == RedeemCode.Status.UNKNOWN, "an empty code pays nothing");
        RedeemCode.Result r = RedeemCode.redeem(p, " welcome-bloop ", 1);
        check(r.status == RedeemCode.Status.OK && r.rewards.size() == 2, "a code works, whatever its case and dashes");
        check(p.coins == 200 && p.hintTokens == hints + 2, "a code pays out");
        check(RedeemCode.redeem(p, "WELCOMEBLOOP", 2).status == RedeemCode.Status.ALREADY_USED && p.coins == 200, "a code only works once");
        check(RedeemCode.redeem(p, "MEOWMEOW", 2).status == RedeemCode.Status.OK && p.revealTokens == 1, "each code works separately");
        check(RedeemCode.redeem(p, "TUNATIME", 2).status == RedeemCode.Status.OK && p.coins == 200 + 150 + 300, "all the codes work");
        check(Progress.parse(p.serialize()).redeemedCodes == p.redeemedCodes, "used codes are saved");
        Progress a = new Progress(), b = new Progress();
        a.redeemedCodes = 1;
        b.redeemedCodes = 2;
        check(Progress.merge(a, b).redeemedCodes == 3, "a code used on either device stays used");
        check(RedeemCode.hash("meow-meow") == RedeemCode.hash("MEOWMEOW"), "the hash ignores case and dashes");
    }

    private static void testRegions() {
        check(Region.forChapter(0).name.equals("FROSTFALL") && Region.forChapter(3).name.equals("EMBER LEAVES"), "the first four regions stay");
        check(Region.forChapter(0).bossName().equals("Sir Frostwhisker"), "the first bosses keep their names");
        check(Region.forChapter(4).name.equals(Region.forChapter(4).name), "regions are the same every time");
        check(Region.forChapter(50).skyTop == Region.forChapter(50).skyTop, "a region's colours are the same every time");

        // Far beyond any real save, and past the point the word pairs run out and get numbered.
        int chapters = 20000;
        Set<String> names = new HashSet<>(), bosses = new HashSet<>(), looks = new HashSet<>();
        Set<Region.Weather> weather = new HashSet<>();
        for (int chapter = 0; chapter < chapters; chapter++) {
            Region r = Region.forChapter(chapter);
            names.add(r.name);
            bosses.add(r.bossName());
            looks.add(r.skyTop + "/" + r.skyBottom + "/" + r.hillFar + "/" + r.hillNear + "/" + r.moon + "/" + r.moonX + "/" + r.hillWave);
            weather.add(r.weather);
            if (chapter >= 4) {
                check(r.name.length() <= 24, "a region's name fits the map: " + r.name);
                Region before = Region.forChapter(chapter - 1);
                check(r.skyTop != before.skyTop && r.hillFar != before.hillFar, "no region looks like the one before it");
            }
        }
        check(names.size() == chapters, "no region name ever comes back: " + names.size());
        check(bosses.size() == chapters, "no boss name ever comes back: " + bosses.size());
        check(looks.size() == chapters, "no region ever looks the same: " + looks.size());

        Set<String> faces = new HashSet<>();
        Set<String> fightingStyles = new HashSet<>();
        for (int chapter = 4; chapter < chapters; chapter++) {
            Region r = Region.forChapter(chapter);
            int[] art = r.bossArt();
            check(art.length == Region.BOSS_ART_SIZE * Region.BOSS_ART_SIZE, "a boss's face is the right size");
            faces.add(java.util.Arrays.toString(art));
            check(r.bossPace() >= 2 && r.bossPace() <= 4 && !r.bossDetail().isEmpty(), "every boss has a pace and says what it does");
            Boss.Attack[] attacks = r.bossAttacks();
            check(attacks.length == 1 || attacks[0] != attacks[1], "a boss's two attacks differ");
            fightingStyles.add(java.util.Arrays.toString(attacks) + r.bossPace());
            if (chapter > 4) check(!java.util.Arrays.equals(art, Region.forChapter(chapter - 1).bossArt()), "no boss looks like the one before");
        }
        check(faces.size() == chapters - 4, "no boss face ever comes back: " + faces.size());
        check(fightingStyles.size() >= 30, "bosses fight in many ways: " + fightingStyles.size());
        check(weather.size() == Region.Weather.values().length, "every kind of weather turns up");
        check(Region.forChapter(5_000_000).name.length() > 3, "a region exists however far a player gets");
    }

    private static void testBosses(LevelGenerator gen) {
        check(Journey.isBoss(20) && Journey.isBoss(40) && !Journey.isBoss(19) && !Journey.isBoss(21), "the last level of each chapter is a boss");
        check(!Journey.isHard(20), "a boss isn't also a hard level");
        check(Journey.coinsForLevel(20) == Scoring.COINS_PER_BOSS, "a boss pays more");
        check(Journey.regionOf(20).bossName().equals("Sir Frostwhisker") && Journey.regionOf(40).bossName().equals("Madame Moth"), "each chapter has its boss");

        int[] landed = new int[Boss.Attack.values().length];
        for (int chapter = 0; chapter < 24; chapter++) {
            Level board = gen.forHard(Journey.lastLevel(chapter));
            Region region = Region.forChapter(chapter);
            for (long seed = 0; seed < 4; seed++) fightOut(board, region.bossAttacks(), region.bossPace(), seed);
            for (Boss.Attack a : Boss.Attack.values()) {
                for (long seed = 0; seed < 3; seed++) landed[a.ordinal()] += fightOut(board, new Boss.Attack[]{a}, BossFight.USUAL_PACE, seed);
            }
        }
        for (Boss.Attack a : Boss.Attack.values()) check(landed[a.ordinal()] > 20, a + " lands");

        Level board = gen.forHard(20);
        GameSession s = new GameSession(board, 1);
        BossFight fight = new BossFight(new Boss.Attack[]{Boss.Attack.FREEZE}, BossFight.USUAL_PACE, s);
        check(fight.onWrongWord() == Boss.Attack.FREEZE, "a wrong word sets off an attack");
        check(fight.onWrongWord() == null, "only one effect at a time");
        int ice = s.bossFrozenWord();
        check(ice >= 0 && s.submit(board.placements.get(ice).word) == GameSession.Result.FROZEN, "a frozen word can't be played");
        for (Level.Placement p : board.placements) {
            if (board.placements.indexOf(p) != ice) {
                s.submit(p.word);
                break;
            }
        }
        fight.onBoardChanged();
        check(s.bossFrozenWord() < 0, "finding another word thaws it");

        Progress p = new Progress();
        Reward prize = Journey.bossPrize(39).grant(p, 1);
        check(prize != null, "a boss has a prize");
    }

    private static void testDailyArchive() {
        long today = 20_000;
        Progress p = new Progress();
        check(DailyArchive.days(today).size() == DailyArchive.DAYS_SHOWN, "four weeks are on show");
        check(DailyArchive.days(today).get(DailyArchive.DAYS_SHOWN - 1) == today - 1, "the newest day shown is yesterday");
        check(DailyArchive.missed(p, today) == DailyArchive.DAYS_SHOWN, "a new player has them all to solve");

        long day = today - 3;
        check(!DailyArchive.canOpen(p, day, today) && !DailyArchive.open(p, day, today), "opening costs coins");
        p.coins = 120;
        check(!DailyArchive.canOpen(p, today, today), "today isn't in the archive");
        check(!DailyArchive.canOpen(p, today - DailyArchive.DAYS_SHOWN - 1, today), "days too far back can't be opened");
        check(DailyArchive.open(p, day, today) && p.coins == 70, "opening a day charges for it");
        check(DailyArchive.open(p, day, today) && p.coins == 70, "going back in costs nothing");
        check(DailyArchive.isOpen(p, day) && !DailyArchive.isSolved(p, day), "an opened day isn't solved yet");
        int coins = DailyArchive.solve(p, day);
        check(coins == DailyArchive.SOLVE_COINS && DailyArchive.isSolved(p, day), "solving pays and is remembered");
        check(!DailyArchive.isOpen(p, day), "a solved day is no longer open");
        check(DailyArchive.solve(p, day) == 0, "a day can't pay twice");
        check(!DailyArchive.canOpen(p, day, today), "a solved day can't be bought again");
        check(DailyArchive.missed(p, today) == DailyArchive.DAYS_SHOWN - 1, "a solved day leaves the count");
        check(DailyArchive.OPEN_COINS > DailyArchive.SOLVE_COINS, "catching up costs more than it pays");

        Progress q = new Progress();
        q.streak = new StreakLogic.State(3, 3, today - 1, 0);
        DailyArchive.backfill(q);
        check(DailyArchive.isSolved(q, today - 1) && DailyArchive.isSolved(q, today - 3) && !DailyArchive.isSolved(q, today - 4),
                "an old save's streak counts as solved");
        DailyArchive.markSolved(q, today - 10);
        q.streak = new StreakLogic.State(9, 9, today - 1, 0);
        DailyArchive.backfill(q);
        check(!DailyArchive.isSolved(q, today - 8), "the backfill only happens once");

        check(DailyArchive.isSolved(Progress.parse(p.serialize()), day), "solved days are saved");
        check(Progress.parse(p.serialize()).serialize().equals(p.serialize()), "the archive round-trips");
        Progress a = new Progress(), b = new Progress();
        DailyArchive.markSolved(a, 100);
        DailyArchive.markSolved(b, 101);
        DailyArchive.markSolved(b, 500);
        Progress m = Progress.merge(a, b);
        check(DailyArchive.isSolved(m, 100) && DailyArchive.isSolved(m, 101) && DailyArchive.isSolved(m, 500), "solved days add up across devices");
        check(Progress.merge(a, b).serialize().equals(Progress.merge(b, a).serialize()), "the log merges in any order");
        DailyArchive.markSolved(a, 127); // the top bit of a block makes a negative number in the save
        check(DailyArchive.isSolved(Progress.parse(a.serialize()), 127), "the last day of a block survives saving");
    }

    private static void testBond() {
        long day = 500;
        Progress p = new Progress();
        check(Bond.level(p) == 1 && Bond.progress(p) == 0f, "a new cat starts at level one");
        check(!Bond.canFeed(p, Bond.Treat.KIBBLE, day), "treats cost coins");
        check(Bond.feed(p, Bond.Treat.KIBBLE, day) == null && p.bondXp == 0, "no coins means no treat");
        check(!Bond.isHungry(p, day), "a cat that has never had a treat doesn't ask for one");

        p.coins = 1000;
        Bond.feed(p, Bond.Treat.KIBBLE, day);
        check(p.coins == 975 && p.bondXp == 10, "a treat costs coins and raises the bond");
        Bond.feed(p, Bond.Treat.FISH_BITES, day);
        Bond.feed(p, Bond.Treat.TUNA_FEAST, day);
        check(p.bondXp == 130 && Bond.level(p) == 2, "enough treats raise the level");
        check(Bond.treatsLeft(p, day) == 0 && !Bond.canFeed(p, Bond.Treat.KIBBLE, day), "three treats a day is the limit");
        int coins = p.coins;
        Bond.feed(p, Bond.Treat.KIBBLE, day);
        check(p.coins == coins && p.bondXp == 130, "a full cat costs nothing");
        check(Bond.isHungry(p, day + 1) && !Bond.isHungry(p, day), "the cat is hungry again the next day");
        check(Bond.treatsLeft(p, day + 1) == 3, "treats come back the next day");

        int last = 0;
        for (int level = 2; level <= Bond.MAX_LEVEL; level++) {
            check(Bond.xpFor(level) - Bond.xpFor(level - 1) > last, "every level takes longer than the one before");
            last = Bond.xpFor(level) - Bond.xpFor(level - 1);
        }
        p.bondXp = Bond.xpFor(3);
        check(Bond.level(p) == 3 && Bond.xpToNext(p) == Bond.xpFor(4) - Bond.xpFor(3), "levels start exactly on their threshold");
        check(Bond.giftBonus(p) == 5, "a closer cat brings more coins");
        p.bondXp = Bond.xpFor(Bond.EXTRA_TREAT_LEVEL);
        check(Bond.treatsPerDay(p) == 4, "a close cat takes one more treat a day");

        p.bondXp = Bond.xpFor(Bond.MAX_LEVEL) - 1;
        p.coins = 500;
        Reward prize = Bond.feed(p, Bond.Treat.KIBBLE, day + 5);
        check(prize != null && Shop.owns(p, Shop.Item.HEART_COLLAR), "the top level wins the Heart Collar");
        check(Bond.feed(p, Bond.Treat.KIBBLE, day + 5) == null, "the collar is only won once");
        check(Bond.level(p) == Bond.MAX_LEVEL && Bond.progress(p) == 1f && Bond.xpToNext(p) == 0, "the bar stays full at the top");
        Progress rich = new Progress();
        rich.coins = 100_000;
        check(!Shop.buy(rich, Shop.Item.HEART_COLLAR), "the collar can't be bought");

        Progress a = new Progress(), b = new Progress();
        a.bondXp = 50; a.treatDay = 9; a.treatsToday = 2;
        b.bondXp = 90; b.treatDay = 9; b.treatsToday = 1; b.bondCollarPaid = true;
        Progress m = Progress.merge(a, b);
        check(m.bondXp == 90 && m.treatsToday == 2 && m.bondCollarPaid, "the bond survives a cloud merge");
        check(Progress.parse(b.serialize()).serialize().equals(b.serialize()), "the bond is saved");
    }

    private static void testStreakMilestones() {
        Progress p = new Progress();
        check(StreakMilestone.payDue(p, 1).isEmpty(), "a new player has earned no streak rewards");
        check(StreakMilestone.next(p) == StreakMilestone.DAYS_3, "the first goal is three days");

        p.streak = new StreakLogic.State(3, 3, 100, 0);
        List<Reward> paid = StreakMilestone.payDue(p, 1);
        check(paid.size() == 1 && p.coins == 50, "three days pays once");
        check(StreakMilestone.payDue(p, 2).isEmpty() && p.coins == 50, "a milestone never pays twice");
        check(StreakMilestone.next(p) == StreakMilestone.DAYS_7, "the next goal moves on");

        p.streak = new StreakLogic.State(1, 30, 200, 0); // the streak broke, but the best one stands
        int hints = p.hintTokens;
        StreakMilestone.payDue(p, 3);
        check(p.coins == 50 + 100 + 200 + 400, "everything up to the best streak is paid, even after a break");
        check(p.hintTokens == hints + 2 + 3 && p.revealTokens == 2, "hints and power-ups are paid too");
        check(StreakMilestone.DAYS_7.isPaid(p) && !StreakMilestone.DAYS_60.isPaid(p), "paid milestones are remembered");

        Progress q = new Progress();
        q.streak = new StreakLogic.State(365, 365, 200, 0);
        Shop.give(q, Shop.Item.GOLD_SHADES);
        int before = q.coins;
        StreakMilestone.payDue(q, 4);
        check(Shop.breed(q) == Shop.Breed.ORANGE && Shop.owns(q, Shop.Breed.GALAXY), "a year's streak wins the Galaxy Cat");
        check(q.coins - before > 9_350, "a reward you already own turns into coins");
        check(StreakMilestone.next(q) == null, "there's nothing left after a year");

        Set<Integer> days = new HashSet<>();
        int last = 0;
        for (StreakMilestone m : StreakMilestone.values()) {
            days.add(m.days);
            check(m.days > last, "milestones climb");
            check(!m.summary().isEmpty(), "every milestone says what it pays");
            last = m.days;
        }
        check(days.size() == StreakMilestone.values().length, "every milestone has its own day");

        Progress a = Progress.parse(p.serialize());
        Progress b = Progress.parse(p.serialize());
        a.streakPaid = 1;
        b.streakPaid = 2;
        check(Progress.merge(a, b).streakPaid == 3, "paid milestones survive a cloud merge");
        check(Progress.parse(p.serialize()).streakPaid == p.streakPaid, "paid milestones are saved");
    }

    private static void testComeback() {
        Progress p = new Progress();
        check(Comeback.daysAway(p, 100) == 0, "a first launch is not a comeback");
        check(Comeback.see(p, 100) && p.lastSeenDay == 100, "the first day is noted");
        check(!Comeback.see(p, 100), "seeing the same day again needs no save");
        check(Comeback.daysAway(p, 102) == 0, "two days away is too short");
        check(Comeback.claim(p, 102).isEmpty() && p.coins == 0 && p.lastSeenDay == 102, "nothing is paid for a short gap");

        int hints = p.hintTokens;
        check(Comeback.daysAway(p, 105) == 3, "three days away counts");
        check(Comeback.claim(p, 105).size() == 2 && p.coins == 100 && p.hintTokens == hints + 1, "a short absence pays a small gift");
        check(Comeback.claim(p, 105).isEmpty() && p.coins == 100, "one absence can't pay twice");

        check(Comeback.gift(7).get(0).amount == 200, "a week away pays more");
        check(Comeback.gift(30).size() == 3, "a long absence adds a power-up");
        check(Comeback.gift(1000).get(0).amount == 400, "the gift stops growing");

        check(!Comeback.see(p, 50) && p.lastSeenDay == 105, "turning the clock back changes nothing");
        check(Comeback.daysAway(p, 50) == 0, "a clock in the past is not an absence");

        Progress a = new Progress(), b = new Progress();
        a.lastSeenDay = 10;
        b.lastSeenDay = 20;
        check(Progress.merge(a, b).lastSeenDay == 20, "playing on any device counts as being here");
        check(Progress.parse(b.serialize()).lastSeenDay == 20, "the last day seen is saved");
    }

    private static void testNewSaveFields() {
        Progress p = new Progress();
        p.themesOwned = 6; p.homeTheme = 2; p.jarWords = 12; p.jarDay = 20; p.jarOpensToday = 2;
        p.revealTokens = 1; p.pickTokens = 2; p.timeTokens = 3;
        p.megaWeek = 8; p.megaFound = 1L << 40; p.megaSolvedWeek = 7; p.bestLives = 900;
        p.breedsOwned = 6; p.breed = 2; p.spinDay = 21; p.spinsDry = 11;
        p.seasonId = 5; p.seasonXp = 321; p.seasonClaimedFree = 8; p.seasonClaimedPremium = 16; p.seasonPremium = true;
        p.fishEvent = 5; p.fishCount = 44; p.fishClaimed = 3;
        p.updatedAt = 9;
        check(Progress.parse(p.serialize()).serialize().equals(p.serialize()), "new fields round-trip");
        check(Progress.parse(new Progress().serialize()).jarWords == 0, "an empty save has an empty jar");
        Progress old = Progress.parse("level=9\ncoins=5\n");
        check(old.level == 9 && old.seasonId == 0 && Shop.breed(old) == Shop.Breed.ORANGE, "an old save loads");

        Progress q = Progress.parse(p.serialize());
        q.updatedAt = 3;
        q.themesOwned = 8; q.breedsOwned = 8; q.bestLives = 1000; q.megaFound = 1;
        Progress m = Progress.merge(p, q);
        check(m.themesOwned == 14 && m.breedsOwned == 14, "owned things add up");
        check(m.bestLives == 1000 && m.megaFound == (1L << 40 | 1), "bests and found words merge up");
        check(m.jarWords == 12 && m.revealTokens == 1, "balances follow the newer save");
        check(Progress.merge(p, q).serialize().equals(Progress.merge(q, p).serialize()), "the merge is order-independent");
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

    private static void testPurchases() {
        Progress p = new Progress();
        p.coins = 40;
        check(Purchases.credit(p, "token-a", 250) && p.coins == 290, "a purchase pays its coins");
        check(!Purchases.credit(p, "token-a", 250) && p.coins == 290, "a retried purchase pays nothing");
        check(Purchases.credit(p, "token-b", 800) && p.coins == 1090, "buying again pays again");
        check(Progress.parse(p.serialize()).serialize().equals(p.serialize()), "the purchase ledger round-trips");
        check(!Purchases.credit(Progress.parse(p.serialize()), "token-a", 250), "a reload still remembers a purchase");
        check(Progress.parse("paidzz=250\npaid1f=-5\ncoins=7\n").paidCoins.isEmpty(), "a corrupt ledger line is skipped");

        Progress older = new Progress();
        older.coins = 100; older.updatedAt = 100;
        Purchases.credit(older, "bought-here", 250);
        Progress newer = new Progress();
        newer.coins = 60; newer.updatedAt = 200;
        Progress merged = Progress.merge(older, newer);
        check(merged.coins == 310, "coins bought on the older device survive a merge");
        check(Progress.merge(merged, older).coins == 310, "merging again doesn't pay twice");
        Purchases.credit(newer, "bought-there", 800);
        check(Progress.merge(older, newer).coins == 1110, "purchases on both devices add up");

        Progress screen = new Progress();
        screen.coins = 5;
        Progress stored = new Progress();
        stored.coins = 5;
        stored.adsRemoved = true;
        Purchases.credit(stored, "cleared-mid-game", 2000);
        screen.coins -= 5; // the screen spends from its stale copy
        Purchases.absorb(screen, stored);
        Purchases.absorb(screen, stored);
        check(screen.coins == 2000 && screen.adsRemoved, "a stale screen keeps a purchase that cleared while it was open");

        for (Purchases.Pack pack : Purchases.Pack.values()) check(Purchases.Pack.byProduct(pack.productId) == pack, "packs resolve by product id");
        check(Purchases.Pack.byProduct(Purchases.REMOVE_ADS) == null, "remove_ads is not a coin pack");
    }

    private static void testAdPolicy() {
        long gap = AdPolicy.REWARDED_GRACE_MILLIS;
        Progress p = new Progress();
        p.level = AdPolicy.FIRST_AD_BREAK_LEVEL;
        check(!AdPolicy.adBreakDue(p, 99, gap), "new players see no ad breaks");
        p.level = AdPolicy.FIRST_AD_BREAK_LEVEL + 1;
        check(AdPolicy.WINS_PER_AD_BREAK == 5, "an ad break every five wins");
        check(AdPolicy.adBreakDue(p, AdPolicy.WINS_PER_AD_BREAK, gap), "an ad break is due after five wins");
        check(!AdPolicy.adBreakDue(p, AdPolicy.WINS_PER_AD_BREAK - 1, gap), "four wins aren't enough");
        check(!AdPolicy.adBreakDue(p, 99, gap - 1), "no ad break straight after a video the player chose to watch");
        p.adsRemoved = true;
        check(!AdPolicy.adBreakDue(p, 99, gap), "remove_ads stops ad breaks");

        long day = 20400;
        Progress v = new Progress();
        for (int i = 0; i < AdPolicy.COIN_VIDEOS_PER_DAY; i++) check(AdPolicy.payCoinVideo(v, day), "coin videos pay");
        check(v.coins == AdPolicy.COIN_VIDEOS_PER_DAY * Scoring.REWARDED_AD_COINS, "coin videos pay the set amount");
        check(AdPolicy.coinVideosLeft(v, day) == 0 && !AdPolicy.payCoinVideo(v, day), "coin videos stop for the day");
        check(AdPolicy.coinVideosLeft(v, day + 1) == AdPolicy.COIN_VIDEOS_PER_DAY, "coin videos come back tomorrow");
        check(AdPolicy.payCoinVideo(v, day + 1) && v.coinVideosToday == 1, "a new day counts from one");
        check(Progress.parse(v.serialize()).serialize().equals(v.serialize()), "coin video counts round-trip");
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

        Progress wide = new Progress();
        Shop.give(wide, Shop.Item.JETPACK);
        Progress farRound = Progress.parse(wide.serialize());
        check(Shop.owns(farRound, Shop.Item.JETPACK) && Shop.ownedCount(farRound) == 1, "owned items survive a save, whichever number holds them");
        Progress caped = Progress.parse("owned=" + (1L << Shop.Item.CAPE.id | 1L << Shop.Item.SCARF.id) + "\nwearNeck=" + Shop.Item.CAPE.id + "\n");
        check(Shop.wearing(caped, Shop.Slot.BACK) == Shop.Item.CAPE && Shop.wearing(caped, Shop.Slot.NECK) == null,
                "a cape worn as neckwear in an old save moves to the back");
        Shop.wear(caped, Shop.Item.SCARF);
        check(Shop.isWearing(caped, Shop.Item.SCARF) && Shop.isWearing(caped, Shop.Item.CAPE), "a scarf and a cape go together");

        Shop.takeOff(round, Shop.Slot.NECK);
        check(Shop.wearing(round, Shop.Slot.NECK) == null && Shop.owns(round, bell), "taking off keeps ownership");

        java.util.Set<Integer> ids = new java.util.HashSet<>();
        java.io.File dir = new java.io.File("../app/src/main/assets/cat");
        for (Shop.Item i : Shop.Item.values()) {
            check(ids.add(i.id), "unique shop id: " + i);
            check(i.id > 0 && i.id <= Shop.MAX_ITEM_ID, "shop id fits the save bitmask: " + i);
            if (dir.isDirectory()) check(new java.io.File(dir, "acc_" + i.key + ".png").isFile(), "art exists for " + i);
        }
        for (Shop.Breed b : Shop.Breed.values()) {
            if (b == Shop.Breed.ORANGE || !dir.isDirectory()) continue;
            check(new java.io.File(dir, "body_" + b.key + ".png").isFile(), "body art exists for " + b);
            for (int t = 0; t < 3; t++) check(new java.io.File(dir, "tail_" + t + "_" + b.key + ".png").isFile(), "tail art exists for " + b);
        }
        java.io.File home = new java.io.File("../app/src/main/assets/home");
        if (home.isDirectory()) {
            for (Home.Item i : Home.Item.values()) check(new java.io.File(home, i.key + ".png").isFile(), "art exists for " + i);
            for (Home.Theme t : Home.Theme.values()) check(new java.io.File(home, t.key + ".png").isFile(), "art exists for " + t);
        }
    }

    /** Every chapter starts easy and climbs to its boss; the first chapters are gentler. */
    private static void testCurve(LevelGenerator gen) {
        check(LevelGenerator.CHAPTER_LEVELS == Journey.LEVELS_PER_CHAPTER, "the curve knows how long a chapter is");
        for (int chapter = 0; chapter < 60; chapter++) {
            int first = Journey.firstLevel(chapter);
            LevelGenerator.Spec prev = null;
            for (int n = first; n <= Journey.lastLevel(chapter); n++) {
                LevelGenerator.Spec s = LevelGenerator.specForLevel(n);
                if (prev != null) {
                    check(s.letterCount >= prev.letterCount && s.targetWords >= prev.targetWords, "difficulty never drops inside a chapter, level " + n);
                }
                prev = s;
            }
            for (int n = first; n < first + 3; n++) check(LevelGenerator.specForLevel(n).letterCount <= 5, "a chapter opens with easy levels: " + n);
            if (chapter >= 2) {
                check(LevelGenerator.specForLevel(first + 3).letterCount == 6, "the fourth level of a chapter jumps to 6 letters");
                check(LevelGenerator.specForLevel(Journey.lastLevel(chapter)).letterCount == 8, "a chapter ends on 8 letters");
            }
        }
        check(LevelGenerator.specForLevel(Journey.lastLevel(0) - 1).letterCount <= 6, "the first chapter stays gentle");
        check(LevelGenerator.specForLevel(Journey.lastLevel(1) - 1).letterCount <= 7, "so does the second");
        long slowest = 0;
        for (int n : new int[]{41, 42, 43, 44, 55, 59, 60, 199, 200, 399, 400}) {
            long t0 = System.nanoTime();
            Level l = Journey.isHard(n) || Journey.isBoss(n) ? gen.forHard(n) : gen.forProgression(n);
            slowest = Math.max(slowest, System.nanoTime() - t0);
            check(l.placements.size() >= 3, "level " + n + " is playable: " + l.placements.size() + " words");
        }
        check(gen.forProgression(59).placements.size() >= 8, "late chapter levels have plenty of words");
        check(slowest < 2_000_000_000L, "every level builds in under two seconds: " + slowest / 1_000_000 + " ms");
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
