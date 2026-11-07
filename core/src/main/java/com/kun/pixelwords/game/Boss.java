package com.kun.pixelwords.game;

/**
 * The four hand-made bosses of the first four regions, and the attacks every boss chooses from. After
 * those, each region builds a boss of its own. See {@link Region#bossName()} and {@link Region#bossAttacks()}.
 */
public enum Boss {

    FROSTWHISKER("Sir Frostwhisker", "boss_frost", Attack.FREEZE),
    MADAME_MOTH("Madame Moth", "boss_moth", Attack.SCRAMBLE),
    COUNT_GLOOM("Count Gloom", "boss_bat", Attack.DARKEN),
    EMBER_KING("The Ember King", "boss_king", Attack.STEAL);

    public enum Attack {
        FREEZE("Freezes a word. Find another one to thaw it.", "ICE! FIND ANOTHER WORD"),
        SCRAMBLE("Blows the wheel out of order.", "THE WHEEL BLEW AWAY!"),
        DARKEN("Plunges a word into darkness, hiding its letters until you find another.", "DARKNESS FALLS!"),
        STEAL("Steals a letter from the wheel. Find a word without it to get it back.", "A LETTER WAS STOLEN!");

        /** What it does, for showing before the fight. */
        public final String detail;
        /** What the banner says when it strikes. */
        public final String shout;

        Attack(String detail, String shout) {
            this.detail = detail;
            this.shout = shout;
        }
    }

    public final String title;
    public final String icon;
    public final Attack attack;

    Boss(String title, String icon, Attack attack) {
        this.title = title;
        this.icon = icon;
        this.attack = attack;
    }

    /** The hand-made boss of one of the first regions. */
    static Boss forChapter(int chapter) {
        return values()[chapter];
    }
}
