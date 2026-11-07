package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;

/**
 * A welcome-back present for a player who has been away for a few days. It gets bigger the longer they
 * were gone, up to a limit, and {@link Progress#lastSeenDay} makes sure one absence only pays once.
 */
public final class Comeback {

    private Comeback() {}

    public static final int MIN_DAYS_AWAY = 3;
    private static final int LONG_AWAY = 7;
    private static final int VERY_LONG_AWAY = 14;

    /** How many days the player has been gone, or 0 if there's nothing to welcome them back for. */
    public static int daysAway(Progress p, long today) {
        if (p.lastSeenDay == 0) return 0; // a first launch, or a save from before this existed
        long away = today - p.lastSeenDay;
        return away >= MIN_DAYS_AWAY ? (int) away : 0;
    }

    /** What an absence of this many days is worth. */
    public static List<Reward> gift(int daysAway) {
        List<Reward> out = new ArrayList<>();
        if (daysAway < MIN_DAYS_AWAY) return out;
        if (daysAway >= VERY_LONG_AWAY) {
            out.add(Reward.coins(400));
            out.add(Reward.hints(3));
            out.add(Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1));
        } else if (daysAway >= LONG_AWAY) {
            out.add(Reward.coins(200));
            out.add(Reward.hints(2));
        } else {
            out.add(Reward.coins(100));
            out.add(Reward.hints(1));
        }
        return out;
    }

    /** Pays for the absence, if there was one, and returns what was paid. */
    public static List<Reward> claim(Progress p, long today) {
        List<Reward> paid = new ArrayList<>();
        for (Reward r : gift(daysAway(p, today))) paid.add(r.grant(p, today + paid.size()));
        see(p, today);
        return paid;
    }

    /** Notes that the player was here today. True if that's new, so the caller knows to save. Time only moves forward. */
    public static boolean see(Progress p, long today) {
        if (today <= p.lastSeenDay) return false;
        p.lastSeenDay = today;
        return true;
    }
}
