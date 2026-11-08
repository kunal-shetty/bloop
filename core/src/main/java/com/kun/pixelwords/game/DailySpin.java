package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.SeedUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One free spin a day, with the odds on show. The result comes from the day and the player, so spinning
 * again after a reinstall gives the same prize. A legendary is guaranteed after a long dry run.
 */
public final class DailySpin {

    private DailySpin() {}

    private static final long SALT = 0x5350_494E_5350_494EL;
    /** The prize is out of this many. */
    private static final int ODDS_TOTAL = 1000;
    /** The next spin after this many without a legendary is always one. */
    public static final int LEGENDARY_AFTER = 60;
    /** Paid once the player owns every legendary. */
    private static final int ALL_LEGENDARIES_COINS = 1000;

    /** One slice of the wheel. */
    public static final class Segment {
        public final Reward reward;
        /** Chance out of 1000. */
        public final int odds;
        public final boolean legendary;

        Segment(Reward reward, int odds, boolean legendary) {
            this.reward = reward;
            this.odds = odds;
            this.legendary = legendary;
        }

        /** The chance as text, such as "1%". */
        public String oddsLabel() {
            return odds % 10 == 0 ? odds / 10 + "%" : odds / 10 + "." + odds % 10 + "%";
        }
    }

    private static final List<Segment> SEGMENTS = Arrays.asList(
            new Segment(Reward.coins(20), 300, false),
            new Segment(Reward.hints(1), 180, false),
            new Segment(Reward.coins(50), 240, false),
            new Segment(Reward.powerUp(PowerUps.Kind.PICK_LETTER, 1), 100, false),
            new Segment(Reward.coins(150), 60, false),
            new Segment(Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1), 70, false),
            new Segment(Reward.hints(2), 40, false),
            new Segment(null, 10, true));

    private static final List<Reward> LEGENDARIES = Arrays.asList(
            Reward.breed(Shop.Breed.GALAXY),
            Reward.item(Shop.Item.GALAXY_CROWN),
            Reward.item(Shop.Item.GOLD_SHADES),
            Reward.item(Shop.Item.PHOENIX_CAPE));

    public static List<Segment> segments() {
        return SEGMENTS;
    }

    /** The result of a spin. */
    public static final class Result {
        /** Which slice the wheel stops on. */
        public final int segment;
        /** What the player actually got. */
        public final Reward paid;

        Result(int segment, Reward paid) {
            this.segment = segment;
            this.paid = paid;
        }

        public boolean isLegendary() {
            return SEGMENTS.get(segment).legendary;
        }
    }

    public static boolean canSpin(Progress p, long today) {
        return today > p.spinDay;
    }

    /** Spins and pays. Returns null if today's spin is used. */
    public static Result spin(Progress p, long today, long playerHash) {
        if (!canSpin(p, today)) return null;
        long seed = SeedUtil.mix(SeedUtil.mix(SALT, today), playerHash);
        int segment = p.spinsDry + 1 >= LEGENDARY_AFTER ? SEGMENTS.size() - 1 : pick(seed);
        Segment s = SEGMENTS.get(segment);
        p.spinDay = today;
        p.spinsDry = s.legendary ? 0 : p.spinsDry + 1;
        Reward paid = s.legendary ? legendary(p, seed).grant(p, seed) : s.reward.grant(p, seed);
        return new Result(segment, paid);
    }

    private static int pick(long seed) {
        int roll = (int) Math.floorMod(seed, (long) ODDS_TOTAL);
        for (int i = 0; i < SEGMENTS.size(); i++) {
            roll -= SEGMENTS.get(i).odds;
            if (roll < 0) return i;
        }
        return 0;
    }

    /** A legendary the player doesn't have yet, or a big coin prize once they own them all. */
    private static Reward legendary(Progress p, long seed) {
        List<Reward> left = new ArrayList<>();
        Progress trial = new Progress();
        for (Reward r : LEGENDARIES) {
            trial.ownedItems = p.ownedItems;
            trial.breedsOwned = p.breedsOwned;
            if (r.grant(trial, seed) == r) left.add(r);
        }
        if (left.isEmpty()) return Reward.coins(ALL_LEGENDARIES_COINS);
        return left.get((int) Math.floorMod(seed >>> 16, (long) left.size()));
    }
}
