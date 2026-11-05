package com.kun.pixelwords.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.kun.pixelwords.game.Progress;

/** Local save: one SharedPreferences string holding Progress.serialize(). Cloud save syncs the same string. */
public final class ProgressStore {

    private static final String PREFS = "pixelwords";
    private static final String KEY = "progress";

    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Progress load() {
        return Progress.parse(prefs.getString(KEY, ""));
    }

    public void save(Progress p) {
        p.updatedAt = System.currentTimeMillis();
        prefs.edit().putString(KEY, p.serialize()).apply();
    }
}
