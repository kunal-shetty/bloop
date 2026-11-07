package com.kun.pixelwords.game;

/** The ways a chapter's rival can fight. Every region's boss uses one, under its own name. See {@link Region#bossName()}. */
public enum Boss {

    FROSTWHISKER("Sir Frostwhisker", "boss_frost", Attack.FREEZE,
            "Freezes a word. Find another one to thaw it.", "ICE! FIND ANOTHER WORD"),
    MADAME_MOTH("Madame Moth", "boss_moth", Attack.SCRAMBLE,
            "Blows the wheel out of order.", "THE WHEEL BLEW AWAY!"),
    COUNT_GLOOM("Count Gloom", "boss_bat", Attack.DARKEN,
            "Plunges a word into darkness, hiding its letters until you find another.", "DARKNESS FALLS!"),
    EMBER_KING("The Ember King", "boss_king", Attack.STEAL,
            "Steals a letter from the wheel. Find a word without it to get it back.", "A LETTER WAS STOLEN!");

    public enum Attack { FREEZE, SCRAMBLE, DARKEN, STEAL }

    public final String title;
    public final String icon;
    public final Attack attack;
    /** What it does, for showing before the fight. */
    public final String detail;
    /** What the banner says when it strikes. */
    public final String shout;

    Boss(String title, String icon, Attack attack, String detail, String shout) {
        this.title = title;
        this.icon = icon;
        this.attack = attack;
        this.detail = detail;
        this.shout = shout;
    }

    /** The fighting style of a chapter's boss. */
    public static Boss forChapter(int chapter) {
        return values()[chapter % values().length];
    }
}
