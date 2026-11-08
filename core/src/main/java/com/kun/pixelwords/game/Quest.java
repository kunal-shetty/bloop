package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One of the three goals for a week. The quests come from the week number, so every device gets the
 * same ones and nothing about them is saved. Progress is lifetime stats minus a snapshot taken when the
 * week began, so replays, which add nothing to lifetime stats, can't complete a quest.
 */
public final class Quest {

    public static final int PER_WEEK = 3;

    private static final long WEEK_SALT = 0x5155_4553_5453_0001L;
    private static final int DAYS_PER_WEEK = 7;
    /** 1970-01-01 was a Thursday; this shift makes weeks start on Monday. */
    private static final int MONDAY_SHIFT = 3;
    private static final int[] COINS = {40, 60, 80};

    public enum Kind {
        WORDS("Find %d words", "book", 40, 60, 80),
        LEVELS("Clear %d new levels", "star", 4, 6, 8),
        DUELS("Win %d duels", "swords", 2, 3, 4),
        DAILIES("Solve %d dailies", "calendar", 3, 4, 5);

        final String title;
        final String icon;
        final int[] goals;

        Kind(String title, String icon, int... goals) {
            this.title = title;
            this.icon = icon;
            this.goals = goals;
        }
    }

    public final Kind kind;
    public final int goal;
    public final int coins;
    private final int bit;

    private Quest(int index, Kind kind, int difficulty) {
        this.kind = kind;
        this.goal = kind.goals[difficulty];
        this.coins = COINS[difficulty];
        this.bit = 1 << index;
    }

    public String title() {
        return String.format(java.util.Locale.ROOT, kind.title, goal);
    }

    public String icon() {
        return kind.icon;
    }

    public static long weekOf(long epochDay) {
        return Math.floorDiv(epochDay + MONDAY_SHIFT, (long) DAYS_PER_WEEK);
    }

    /** Days until the quests change, counting today. */
    public static int daysLeft(long epochDay) {
        return DAYS_PER_WEEK - (int) Math.floorMod(epochDay + MONDAY_SHIFT, (long) DAYS_PER_WEEK);
    }

    /** This week's three quests: all different kinds, one of each difficulty. */
    public static List<Quest> forWeek(long week) {
        Random rnd = new Random(WEEK_SALT + week);
        List<Kind> kinds = new ArrayList<>(Arrays.asList(Kind.values()));
        Collections.shuffle(kinds, rnd);
        List<Quest> quests = new ArrayList<>();
        for (int i = 0; i < PER_WEEK; i++) quests.add(new Quest(i, kinds.get(i), i));
        return quests;
    }

    /** Starts a new week if one has begun, and returns true if the save changed. */
    public static boolean rollOver(Progress p, long epochDay) {
        long week = weekOf(epochDay);
        if (p.questWeek == week) return false;
        p.questWeek = week;
        p.questBaseWords = p.totalWordsFound;
        p.questBaseLevel = p.level;
        p.questBaseDuels = p.duelWins;
        p.questBaseDailies = p.dailiesCompleted;
        p.questClaimed = 0;
        return true;
    }

    /** How far the player has got this week, capped at the goal. */
    public int progress(Progress p) {
        return Math.max(0, Math.min(goal, gainedThisWeek(p)));
    }

    private int gainedThisWeek(Progress p) {
        switch (kind) {
            case WORDS: return p.totalWordsFound - p.questBaseWords;
            case LEVELS: return p.level - p.questBaseLevel;
            case DUELS: return p.duelWins - p.questBaseDuels;
            default: return p.dailiesCompleted - p.questBaseDailies;
        }
    }

    public boolean isDone(Progress p) {
        return progress(p) >= goal;
    }

    public boolean isClaimed(Progress p) {
        return (p.questClaimed & bit) != 0;
    }

    public boolean isClaimable(Progress p) {
        return isDone(p) && !isClaimed(p);
    }

    /** Pays the reward once. Returns false if it isn't finished or was already paid. */
    public boolean claim(Progress p) {
        if (!isClaimable(p)) return false;
        p.questClaimed |= bit;
        p.addCoins(coins);
        return true;
    }

    public static List<Quest> claimable(Progress p) {
        List<Quest> ready = new ArrayList<>();
        for (Quest q : forWeek(p.questWeek)) if (q.isClaimable(p)) ready.add(q);
        return ready;
    }
}
