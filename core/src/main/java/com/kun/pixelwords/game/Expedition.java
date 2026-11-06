package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.SeedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The cat goes off on a trip and comes back with souvenirs. What it brings is fixed when it leaves.
 * {@link Progress#expClaimed} remembers the last trip paid, so a cloud merge can't bring back a trip
 * that was already collected.
 */
public final class Expedition {

    private Expedition() {}

    private static final long HOUR = 60 * 60 * 1000L;
    private static final long SALT = 0x5452_4950_5452_4950L;
    private static final int COINS_PER_HOUR_TO_RUSH = 20;

    public enum Place {
        BEACH(1, "Sunny Beach", "sparkle", 1, 30, 60, 0),
        FOREST(2, "Whisper Forest", "cloud", 4, 90, 150, 35),
        MOUNTAIN(3, "Moon Mountain", "star", 8, 200, 300, 70);

        public final int id;
        public final String title;
        public final String icon;
        public final int hours;
        private final int minCoins, maxCoins, stickerChance;

        Place(int id, String title, String icon, int hours, int minCoins, int maxCoins, int stickerChance) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.hours = hours;
            this.minCoins = minCoins;
            this.maxCoins = maxCoins;
            this.stickerChance = stickerChance;
        }

        /** The coin range, for showing before the cat leaves. */
        public String coinRange() {
            return minCoins + "-" + maxCoins;
        }
    }

    /** Where the cat is, or null if it's at home. */
    public static Place place(Progress p) {
        for (Place place : Place.values()) if (place.id == p.expPlace) return place;
        return null;
    }

    public static boolean isAway(Progress p) {
        return place(p) != null;
    }

    /** Sends the cat off. False if it's already on a trip. */
    public static boolean start(Progress p, Place place, long nowMillis) {
        if (isAway(p)) return false;
        p.expPlace = place.id;
        p.expStart = nowMillis;
        p.expEnd = nowMillis + place.hours * HOUR;
        return true;
    }

    public static boolean isBack(Progress p, long nowMillis) {
        return isAway(p) && nowMillis >= p.expEnd;
    }

    public static long millisLeft(Progress p, long nowMillis) {
        return isAway(p) ? Math.max(0, p.expEnd - nowMillis) : 0;
    }

    /** Coins to bring the cat home now, by the hours left, rounded up. */
    public static int rushCost(Progress p, long nowMillis) {
        long left = millisLeft(p, nowMillis);
        return (int) ((left + HOUR - 1) / HOUR) * COINS_PER_HOUR_TO_RUSH;
    }

    /** Pays to bring the cat home now. False if it's already home or the player can't afford it. */
    public static boolean rush(Progress p, long nowMillis) {
        if (!isAway(p) || isBack(p, nowMillis)) return false;
        int cost = rushCost(p, nowMillis);
        if (p.coins < cost) return false;
        p.coins -= cost;
        p.expEnd = nowMillis;
        return true;
    }

    /** What a trip brings back, decided by when it left. */
    private static List<Reward> souvenirs(Place place, long start) {
        Random rnd = new Random(SeedUtil.mix(SALT, start));
        List<Reward> out = new ArrayList<>();
        out.add(Reward.coins(place.minCoins + rnd.nextInt(place.maxCoins - place.minCoins + 1)));
        if (rnd.nextInt(100) < place.stickerChance) out.add(Reward.sticker());
        if (place == Place.MOUNTAIN) out.add(Reward.hints(1));
        return out;
    }

    /** Collects a finished trip, and returns what was paid. Empty if the cat isn't back. */
    public static List<Reward> collect(Progress p, long nowMillis) {
        List<Reward> paid = new ArrayList<>();
        if (!isBack(p, nowMillis) || p.expStart <= p.expClaimed) return paid;
        for (Reward r : souvenirs(place(p), p.expStart)) paid.add(r.grant(p, p.expStart + paid.size()));
        p.expClaimed = p.expStart;
        p.expPlace = 0;
        return paid;
    }
}
