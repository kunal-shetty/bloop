package com.kun.pixelwords.game;

/** One-shot helpers. A saved token is used first, then coins. */
public final class PowerUps {

    private PowerUps() {}

    public enum Kind {
        REVEAL_WORD("Reveal Word", "book", 90, "Fills in a whole word.", 2),
        PICK_LETTER("Pick a Letter", "bulb", 40, "Tap any hidden letter to show it.", 1),
        EXTRA_TIME("+30 Seconds", "clock", 40, "More time, but the run can't set a record.", 0);

        public final String title;
        public final String icon;
        public final int price;
        public final String detail;
        /** How many hints this counts as, for stars and the score penalty. */
        public final int hintCost;

        Kind(String title, String icon, int price, String detail, int hintCost) {
            this.title = title;
            this.icon = icon;
            this.price = price;
            this.detail = detail;
            this.hintCost = hintCost;
        }
    }

    public static final int EXTRA_SECONDS = 30;

    public static int tokens(Progress p, Kind kind) {
        switch (kind) {
            case REVEAL_WORD: return p.revealTokens;
            case PICK_LETTER: return p.pickTokens;
            default: return p.timeTokens;
        }
    }

    public static void addTokens(Progress p, Kind kind, int count) {
        switch (kind) {
            case REVEAL_WORD: p.revealTokens += count; break;
            case PICK_LETTER: p.pickTokens += count; break;
            default: p.timeTokens += count; break;
        }
    }

    public static boolean canUse(Progress p, Kind kind) {
        return tokens(p, kind) > 0 || p.coins >= kind.price;
    }

    /** Pays for one use. False if the player has neither a token nor the coins. */
    public static boolean spend(Progress p, Kind kind) {
        if (tokens(p, kind) > 0) {
            addTokens(p, kind, -1);
            return true;
        }
        if (p.coins < kind.price) return false;
        p.coins -= kind.price;
        return true;
    }
}
