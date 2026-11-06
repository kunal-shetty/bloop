package com.kun.pixelwords.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/** Player preferences. Read on every sound or buzz, so toggles apply instantly. */
public final class Settings {

    private static final int DEFAULT_REMINDER_HOUR = 19;

    private static SharedPreferences prefs;

    private Settings() {}

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("pixelwords_settings", Context.MODE_PRIVATE);
    }

    public static boolean sound() { return prefs.getBoolean("sound", true); }
    public static boolean haptics() { return prefs.getBoolean("haptics", true); }
    public static boolean music() { return prefs.getBoolean("music", true); }
    /** Empty until the player chooses one. */
    public static String playerName() { return prefs.getString("playerName", ""); }

    /** A permanent id for this player, made the first time it's needed. */
    public static String playerId() {
        String id = prefs.getString("playerId", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("playerId", id).apply();
        }
        return id;
    }

    /** The four digits after a name, such as 4821, which tell two players with one name apart. */
    public static String playerTag() {
        String tag = prefs.getString("playerTag", null);
        if (tag == null) {
            tag = String.format(Locale.ROOT, "%04d", new Random().nextInt(10_000));
            prefs.edit().putString("playerTag", tag).apply();
        }
        return tag;
    }

    public static boolean colourblind() { return prefs.getBoolean("colourblind", false); }
    public static boolean largeText() { return prefs.getBoolean("largeText", false); }
    public static boolean leftHanded() { return prefs.getBoolean("leftHanded", false); }
    public static boolean reminders() { return prefs.getBoolean("reminders", false); }
    public static int reminderHour() { return prefs.getInt("reminderHour", DEFAULT_REMINDER_HOUR); }
    /** True once the player has been offered reminders, so they are only asked one time. */
    public static boolean reminderOffered() { return prefs.getBoolean("reminderOffered", false); }

    public static void setSound(boolean on) { prefs.edit().putBoolean("sound", on).apply(); }
    public static void setHaptics(boolean on) { prefs.edit().putBoolean("haptics", on).apply(); }
    public static void setMusic(boolean on) { prefs.edit().putBoolean("music", on).apply(); }
    public static void setPlayerName(String name) { prefs.edit().putString("playerName", name).apply(); }
    public static void setColourblind(boolean on) { prefs.edit().putBoolean("colourblind", on).apply(); }
    public static void setLargeText(boolean on) { prefs.edit().putBoolean("largeText", on).apply(); }
    public static void setLeftHanded(boolean on) { prefs.edit().putBoolean("leftHanded", on).apply(); }
    public static void setReminders(boolean on) { prefs.edit().putBoolean("reminders", on).apply(); }
    public static void setReminderHour(int hour) { prefs.edit().putInt("reminderHour", hour).apply(); }
    public static void setReminderOffered() { prefs.edit().putBoolean("reminderOffered", true).apply(); }
}
