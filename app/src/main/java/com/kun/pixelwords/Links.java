package com.kun.pixelwords;

/** Outside links and contact details shown in the app. Fill these in when they exist; empty ones are hidden. */
public final class Links {

    private Links() {}

    /** The community Discord invite, for example "https://discord.gg/yourcode". Empty until the server exists. */
    public static final String DISCORD_URL = "";

    /** Where players write for help or a refund question. Empty until a support address exists. */
    public static final String SUPPORT_EMAIL = "";

    public static boolean hasDiscord() {
        return !DISCORD_URL.isEmpty();
    }

    public static boolean hasEmail() {
        return !SUPPORT_EMAIL.isEmpty();
    }

    /** How a player reaches us, worded for whichever contact details exist. */
    public static String contact() {
        if (hasEmail() && hasDiscord()) return "email " + SUPPORT_EMAIL + " or message us on our Discord server (" + DISCORD_URL + ")";
        if (hasEmail()) return "email " + SUPPORT_EMAIL;
        if (hasDiscord()) return "message us on our Discord server (" + DISCORD_URL + ")";
        return "write to us through the game's page on Google Play";
    }
}
