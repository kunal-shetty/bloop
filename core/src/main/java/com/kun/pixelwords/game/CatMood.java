package com.kun.pixelwords.game;

import java.util.Random;

/** What the cat says and when it brings coins. Chance decides, but the daily limit lives in the save. */
public final class CatMood {

    public static final int MAX_GIFTS_PER_DAY = 3;
    public static final int MIN_GIFT_COINS = 5;
    public static final int MAX_GIFT_COINS = 15;

    private static final float GIFT_CHANCE = 0.35f;
    /** How often the cat mentions what's going on, instead of just chatting. */
    private static final float TOPICAL_CHANCE = 0.6f;

    private static final String[] CHATTER = {
            "Meow!",
            "You're doing great!",
            "Pet me?",
            "I only know one word. Meow.",
            "Found any bonus words?",
            "Is it fish time?",
            "I like your style.",
    };

    private CatMood() {}

    /** Coins the cat is offering today, or 0 if it has nothing, or has given all it can. */
    public static int rollGift(Progress p, long today, Random rnd) {
        if (giftsToday(p, today) >= MAX_GIFTS_PER_DAY || rnd.nextFloat() >= GIFT_CHANCE) return 0;
        return MIN_GIFT_COINS + rnd.nextInt(MAX_GIFT_COINS - MIN_GIFT_COINS + 1);
    }

    public static void collectGift(Progress p, long today, int coins) {
        p.catGiftsToday = giftsToday(p, today) + 1;
        p.catGiftDay = today;
        p.coins += coins;
    }

    private static int giftsToday(Progress p, long today) {
        return p.catGiftDay == today ? p.catGiftsToday : 0;
    }

    public static String line(Progress p, long today, Random rnd) {
        String topical = topical(p, today);
        if (topical != null && rnd.nextFloat() < TOPICAL_CHANCE) return topical;
        return CHATTER[rnd.nextInt(CHATTER.length)];
    }

    private static String topical(Progress p, long today) {
        int streak = StreakLogic.displayStreak(p.streak, today);
        if (streak == 0 && p.streak.best > 0) return "I missed you! Let's start a new streak.";
        if (streak > 0 && p.streak.lastDay != today) return "Keep our " + streak + "-day streak going!";
        if (Journey.isHard(p.level)) return "The next level is hard. I believe in you!";
        if (Journey.slotOf(p.level) == Journey.LEVELS_PER_CHAPTER) return "One more level to the trophy!";
        return null;
    }
}
