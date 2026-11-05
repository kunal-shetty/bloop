package com.kun.pixelwords.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Player preferences. Read on every sound or buzz, so toggles apply instantly. */
public final class Settings {

    private static SharedPreferences prefs;

    private Settings() {}

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("pixelwords_settings", Context.MODE_PRIVATE);
    }

    public static boolean sound() { return prefs.getBoolean("sound", true); }
    public static boolean haptics() { return prefs.getBoolean("haptics", true); }

    public static void setSound(boolean on) { prefs.edit().putBoolean("sound", on).apply(); }
    public static void setHaptics(boolean on) { prefs.edit().putBoolean("haptics", on).apply(); }
}
