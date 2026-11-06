package com.kun.pixelwords.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.Purchases;

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

    /** A screen's copy may be older than a purchase that cleared while it was open, so that is kept too. */
    public void save(Progress p) {
        Purchases.absorb(p, load());
        p.updatedAt = System.currentTimeMillis();
        prefs.edit().putString(KEY, p.serialize()).apply();
    }
}
