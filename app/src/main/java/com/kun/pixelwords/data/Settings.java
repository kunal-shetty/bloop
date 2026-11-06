package com.kun.pixelwords.data;

import android.content.Context;
import android.content.SharedPreferences;

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
    public static boolean reminders() { return prefs.getBoolean("reminders", false); }
    public static int reminderHour() { return prefs.getInt("reminderHour", DEFAULT_REMINDER_HOUR); }
    /** True once the player has been offered reminders, so they are only asked one time. */
    public static boolean reminderOffered() { return prefs.getBoolean("reminderOffered", false); }

    public static void setSound(boolean on) { prefs.edit().putBoolean("sound", on).apply(); }
    public static void setHaptics(boolean on) { prefs.edit().putBoolean("haptics", on).apply(); }
    public static void setMusic(boolean on) { prefs.edit().putBoolean("music", on).apply(); }
    public static void setReminders(boolean on) { prefs.edit().putBoolean("reminders", on).apply(); }
    public static void setReminderHour(int hour) { prefs.edit().putInt("reminderHour", hour).apply(); }
    public static void setReminderOffered() { prefs.edit().putBoolean("reminderOffered", true).apply(); }
}
