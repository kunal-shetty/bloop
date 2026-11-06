package com.kun.pixelwords.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Themed pages of words to collect. Finding a listed word in any puzzle earns its sticker. A sticker is
 * a bit in {@link Progress#album}, one number per page, so never reorder a page's words: add new pages
 * at the end.
 */
public final class Album {

    private Album() {}

    public static final int WORDS_PER_PAGE = 9;

    private static final int PAGE_COINS = 150;

    public enum Page {
        ANIMALS("Animals", "heart", "cat", "dog", "fox", "ant", "bee", "rat", "pig", "cow", "bat"),
        FOOD("Food", "flame", "egg", "tea", "pie", "jam", "ham", "soup", "rice", "milk", "cake"),
        NATURE("Nature", "sparkle", "sun", "sea", "sky", "tree", "rain", "wind", "leaf", "rock", "moon"),
        HOME("Home", "home", "bed", "door", "lamp", "rug", "cup", "sofa", "desk", "wall", "roof"),
        BODY("Body", "heart", "arm", "leg", "eye", "ear", "toe", "hand", "foot", "nose", "hair"),
        COLOURS("Colours", "star", "red", "blue", "pink", "gold", "grey", "green", "black", "white", "brown"),
        TIME("Time", "clock", "day", "week", "year", "hour", "noon", "night", "today", "month", "spring"),
        TRAVEL("Travel", "cloud", "car", "bus", "map", "road", "ship", "boat", "train", "hotel", "beach");

        public final String title;
        public final String icon;
        public final String[] words;

        Page(String title, String icon, String... words) {
            this.title = title;
            this.icon = icon;
            this.words = words;
        }
    }

    public static boolean has(Progress p, Page page, int index) {
        return (p.album[page.ordinal()] & 1L << index) != 0;
    }

    public static int collected(Progress p, Page page) {
        return Long.bitCount(p.album[page.ordinal()]);
    }

    public static boolean isComplete(Progress p, Page page) {
        return collected(p, page) >= WORDS_PER_PAGE;
    }

    public static boolean isClaimed(Progress p, Page page) {
        return (p.albumClaimed & 1L << page.ordinal()) != 0;
    }

    public static boolean isClaimable(Progress p, Page page) {
        return isComplete(p, page) && !isClaimed(p, page);
    }

    public static int claimableCount(Progress p) {
        int ready = 0;
        for (Page page : Page.values()) if (isClaimable(p, page)) ready++;
        return ready;
    }

    /** Sticks a found word into every page it belongs to. Returns true if that was a new sticker. */
    public static boolean collect(Progress p, String word) {
        String w = word.toLowerCase(Locale.ROOT);
        boolean fresh = false;
        for (Page page : Page.values()) {
            for (int i = 0; i < page.words.length; i++) {
                if (page.words[i].equals(w) && !has(p, page, i)) {
                    p.album[page.ordinal()] |= 1L << i;
                    fresh = true;
                }
            }
        }
        return fresh;
    }

    /** Gives a sticker the player doesn't have, and returns its word, or null if the album is full. */
    static String awardRandom(Progress p, long seed) {
        List<String> missing = new ArrayList<>();
        for (Page page : Page.values()) {
            for (int i = 0; i < page.words.length; i++) {
                if (!has(p, page, i)) missing.add(page.words[i]);
            }
        }
        if (missing.isEmpty()) return null;
        String word = missing.get(new Random(seed).nextInt(missing.size()));
        collect(p, word);
        return word;
    }

    /** What finishing a page pays on top of its coins. */
    public static Reward pagePrize(Page page) {
        switch (page) {
            case NATURE: return Reward.item(Shop.Item.EXPLORER_HAT);
            case TIME: return Reward.item(Shop.Item.SCHOLAR_GLASSES);
            default: return Reward.hints(3);
        }
    }

    /** Pays a finished page once. Returns the prize, or null if it isn't ready. */
    public static Reward claim(Progress p, Page page) {
        if (!isClaimable(p, page)) return null;
        p.albumClaimed |= 1L << page.ordinal();
        p.coins += PAGE_COINS;
        return pagePrize(page).grant(p, page.ordinal());
    }
}
