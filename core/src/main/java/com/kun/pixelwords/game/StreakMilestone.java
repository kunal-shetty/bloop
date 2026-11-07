package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rewards for reaching a long streak. They go by the player's best streak, so one who reached a
 * milestone before these existed still gets it, and losing a streak can't take anything back.
 * Each is a bit in the save: add new ones with a new id, and never renumber.
 */
public enum StreakMilestone {

    DAYS_3(0, 3, Reward.coins(50)),
    DAYS_7(1, 7, Reward.coins(100), Reward.hints(2)),
    DAYS_14(2, 14, Reward.coins(200), Reward.sticker()),
    DAYS_30(3, 30, Reward.coins(400), Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 2)),
    DAYS_60(4, 60, Reward.coins(600), Reward.powerUp(PowerUps.Kind.PICK_LETTER, 3), Reward.sticker()),
    DAYS_100(5, 100, Reward.coins(1000), Reward.item(Shop.Item.GOLD_SHADES)),
    DAYS_200(6, 200, Reward.coins(2000), Reward.item(Shop.Item.GALAXY_CROWN)),
    DAYS_365(7, 365, Reward.coins(5000), Reward.breed(Shop.Breed.GALAXY));

    private final int id;
    public final int days;
    private final Reward[] rewards;

    StreakMilestone(int id, int days, Reward... rewards) {
        this.id = id;
        this.days = days;
        this.rewards = rewards;
    }

    public List<Reward> rewards() {
        return Arrays.asList(rewards);
    }

    /** The rewards in a few words, for showing before they're won. */
    public String summary() {
        StringBuilder s = new StringBuilder();
        for (Reward r : rewards) s.append(s.length() == 0 ? "" : " + ").append(r.label());
        return s.toString();
    }

    public boolean isPaid(Progress p) {
        return (p.streakPaid & 1L << id) != 0;
    }

    /** Pays every milestone reached and not yet paid, and returns what the player actually got. */
    public static List<Reward> payDue(Progress p, long seed) {
        List<Reward> paid = new ArrayList<>();
        for (StreakMilestone m : values()) {
            if (m.isPaid(p) || p.streak.best < m.days) continue;
            p.streakPaid |= 1L << m.id;
            for (Reward r : m.rewards) paid.add(r.grant(p, seed + paid.size()));
        }
        return paid;
    }

    /** The next milestone to reach, or null once they've all been won. */
    public static StreakMilestone next(Progress p) {
        for (StreakMilestone m : values()) if (p.streak.best < m.days) return m;
        return null;
    }
}
