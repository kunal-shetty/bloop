package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A week in the middle of each season when fish hide under the letters. Where they hide comes from the
 * level and the event, so nothing about a board is saved, only how many fish the player has caught.
 */
public final class FishHunt {

    private FishHunt() {}

    private static final int FIRST_DAY = 12;
    private static final int DAYS = 7;
    private static final long SALT = 0x4649_5348_4649_5348L;

    /** Fish to catch for each prize, in order. */
    private static final int[] GOALS = {10, 25, 50, 80, 120};

    public static int prizes() {
        return GOALS.length;
    }

    public static int goal(int prize) {
        return GOALS[prize];
    }

    public static Reward prize(int index) {
        switch (index) {
            case 0: return Reward.coins(50);
            case 1: return Reward.hints(2);
            case 2: return Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 2);
            case 3: return Reward.coins(200);
            default: return Reward.item(Shop.Item.FISH_HAT);
        }
    }

    public static boolean isLive(long epochDay) {
        int day = Season.dayOf(epochDay);
        return day >= FIRST_DAY && day < FIRST_DAY + DAYS;
    }

    /** Days left in the hunt, or days until it starts if it hasn't. */
    public static int daysToChange(long epochDay) {
        int day = Season.dayOf(epochDay);
        if (day < FIRST_DAY) return FIRST_DAY - day;
        if (day < FIRST_DAY + DAYS) return FIRST_DAY + DAYS - day;
        return Season.DAYS - day + FIRST_DAY;
    }

    /** The cells hiding fish on a board, as {row, col}, for an event. */
    public static List<int[]> fishOn(Level level, long event) {
        List<int[]> cells = new ArrayList<>();
        char[][] grid = level.grid();
        for (int r = 0; r < level.rows; r++) {
            for (int c = 0; c < level.cols; c++) {
                if (grid[r][c] != '\0') cells.add(new int[]{r, c});
            }
        }
        Random rnd = new Random(level.fingerprint() ^ (SALT + event));
        int count = 2 + rnd.nextInt(2);
        Collections.shuffle(cells, rnd);
        return new ArrayList<>(cells.subList(0, Math.min(count, cells.size())));
    }

    /** Banks fish caught during a live hunt. */
    public static void add(Progress p, long epochDay, int fish) {
        if (!isLive(epochDay)) return;
        Season.rollOver(p, epochDay);
        if (p.fishEvent != p.seasonId) {
            p.fishEvent = p.seasonId;
            p.fishCount = 0;
            p.fishClaimed = 0;
        }
        p.fishCount += fish;
    }

    public static boolean isClaimed(Progress p, int prize) {
        return (p.fishClaimed & 1 << prize) != 0;
    }

    public static boolean isClaimable(Progress p, int prize) {
        return p.fishCount >= GOALS[prize] && !isClaimed(p, prize);
    }

    public static int claimableCount(Progress p) {
        int ready = 0;
        for (int i = 0; i < GOALS.length; i++) if (isClaimable(p, i)) ready++;
        return ready;
    }

    /** Pays a prize once. Returns what was paid, or null if it isn't ready. */
    public static Reward claim(Progress p, int prize) {
        if (!isClaimable(p, prize)) return null;
        p.fishClaimed |= 1 << prize;
        return prize(prize).grant(p, SALT + p.fishEvent + prize);
    }

    /** Pays anything still waiting at the end of a season and clears the catch. */
    static void settle(Progress p) {
        for (int i = 0; i < GOALS.length; i++) claim(p, i);
        p.fishCount = 0;
        p.fishClaimed = 0;
    }
}
