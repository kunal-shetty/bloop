package com.kun.pixelwords.game;

/**
 * How close the player and their cat are. Treats cost coins and raise the bond, a little each day, and
 * each level unlocks something. This is meant to be a steady use for coins, so it can't be rushed:
 * only a few treats a day, and later levels take longer.
 */
public final class Bond {

    private Bond() {}

    public enum Treat {
        KIBBLE("Kibble", "paw", 25, 10),
        FISH_BITES("Fish Bites", "fish", 60, 30),
        TUNA_FEAST("Tuna Feast", "fish", 150, 90);

        public final String title;
        public final String icon;
        public final int price;
        public final int xp;

        Treat(String title, String icon, int price, int xp) {
            this.title = title;
            this.icon = icon;
            this.price = price;
            this.xp = xp;
        }
    }

    /** One thing a bond level unlocks. */
    public static final class Perk {
        public final int level;
        public final String text;

        private Perk(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    public static final int MAX_LEVEL = 10;
    public static final int CHATTER_LEVEL = 2;
    public static final int FIRST_GIFT_LEVEL = 3;
    public static final int COVE_LEVEL = 4;
    public static final int EXTRA_TREAT_LEVEL = 5;
    public static final int SECOND_GIFT_LEVEL = 7;
    private static final int TREATS_PER_DAY = 3;
    private static final int GIFT_STEP_COINS = 5;
    private static final int XP_STEP = 100;

    private static final Perk[] PERKS = {
            new Perk(CHATTER_LEVEL, "Your cat has more to say"),
            new Perk(FIRST_GIFT_LEVEL, "Bigger coin gifts"),
            new Perk(COVE_LEVEL, "Trips to Treasure Cove"),
            new Perk(EXTRA_TREAT_LEVEL, "One more treat a day"),
            new Perk(SECOND_GIFT_LEVEL, "Even bigger coin gifts"),
            new Perk(MAX_LEVEL, "The Heart Collar"),
    };

    public static Perk[] perks() {
        return PERKS.clone();
    }

    /** Total bond needed to reach a level: 100, 300, 600, 1000 and so on. */
    public static int xpFor(int level) {
        return XP_STEP * (level - 1) * level / 2;
    }

    public static int level(Progress p) {
        int level = 1;
        while (level < MAX_LEVEL && p.bondXp >= xpFor(level + 1)) level++;
        return level;
    }

    /** How far through this level the bond is, from 0 to 1. Full at the top level. */
    public static float progress(Progress p) {
        int level = level(p);
        if (level == MAX_LEVEL) return 1f;
        return (p.bondXp - xpFor(level)) / (float) (xpFor(level + 1) - xpFor(level));
    }

    /** Bond still needed for the next level. Zero at the top level. */
    public static int xpToNext(Progress p) {
        int level = level(p);
        return level == MAX_LEVEL ? 0 : xpFor(level + 1) - p.bondXp;
    }

    public static int treatsPerDay(Progress p) {
        return level(p) >= EXTRA_TREAT_LEVEL ? TREATS_PER_DAY + 1 : TREATS_PER_DAY;
    }

    public static int treatsLeft(Progress p, long today) {
        return Math.max(0, treatsPerDay(p) - (p.treatDay == today ? p.treatsToday : 0));
    }

    public static boolean canFeed(Progress p, Treat treat, long today) {
        return treatsLeft(p, today) > 0 && p.coins >= treat.price;
    }

    /** True if the cat hasn't had a treat today, once the player has met treats at all. */
    public static boolean isHungry(Progress p, long today) {
        return p.bondXp > 0 && p.treatDay != today;
    }

    /**
     * Feeds the cat, if {@link #canFeed} allows it. Returns the prize this treat won, or null. The Heart
     * Collar is the only one, and it's marked as paid so a cloud merge can't hand it out again.
     */
    public static Reward feed(Progress p, Treat treat, long today) {
        if (!canFeed(p, treat, today)) return null;
        p.treatsToday = (p.treatDay == today ? p.treatsToday : 0) + 1;
        p.treatDay = today;
        p.coins -= treat.price;
        p.bondXp += treat.xp;
        if (level(p) < MAX_LEVEL || p.bondCollarPaid) return null;
        p.bondCollarPaid = true;
        return Reward.item(Shop.Item.HEART_COLLAR).grant(p, today);
    }

    /** Extra coins on each of the cat's gifts. */
    public static int giftBonus(Progress p) {
        int level = level(p);
        return (level >= FIRST_GIFT_LEVEL ? GIFT_STEP_COINS : 0) + (level >= SECOND_GIFT_LEVEL ? GIFT_STEP_COINS : 0);
    }
}
