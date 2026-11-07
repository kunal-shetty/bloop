package com.kun.pixelwords.game;

/** The rival at the end of each chapter. There's one for each chapter scenery, and each fights differently. */
public enum Boss {

    FROSTWHISKER("Sir Frostwhisker", "boss_frost", Attack.FREEZE,
            "Sir Frostwhisker freezes a word. Find another one to thaw it.", "ICE! FIND ANOTHER WORD"),
    MADAME_MOTH("Madame Moth", "boss_moth", Attack.SCRAMBLE,
            "Madame Moth beats her wings and blows the wheel out of order.", "THE WHEEL BLEW AWAY!"),
    COUNT_GLOOM("Count Gloom", "boss_bat", Attack.DARKEN,
            "Count Gloom plunges a word into darkness, hiding its letters until you find another.", "DARKNESS FALLS!"),
    EMBER_KING("The Ember King", "boss_king", Attack.STEAL,
            "The Ember King steals a letter from the wheel. Find a word without it to get it back.", "A LETTER WAS STOLEN!");

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

    /** The boss guarding a chapter, in step with the chapter's scenery. */
    public static Boss forChapter(int chapter) {
        return values()[Journey.themeIndex(chapter)];
    }
}
