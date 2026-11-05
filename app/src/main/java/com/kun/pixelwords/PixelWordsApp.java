package com.kun.pixelwords;

import android.app.Application;

import com.kun.pixelwords.audio.Haptics;
import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.GameEngine;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.services.Reminders;
import com.kun.pixelwords.ui.PixelTheme;

public class PixelWordsApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Settings.init(this);
        PixelTheme.init(this);
        Haptics.init(this);
        Sfx.init(this);
        PlayGamesService.initialize(this);
        GameEngine.init(this); // starts loading the dictionary on a background thread
        Reminders.schedule(this);
    }
}
