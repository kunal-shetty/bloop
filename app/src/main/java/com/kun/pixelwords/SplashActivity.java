package com.kun.pixelwords;

import android.content.Intent;
import android.os.Bundle;

import com.kun.pixelwords.data.GameEngine;
import com.kun.pixelwords.ui.SplashView;

/** Plays the animated boot sequence, and holds on it until the dictionary has finished loading. */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashView splash = new SplashView(this);
        splash.setListener(() -> {
            go(new Intent(this, MainActivity.class));
            finish();
        });
        contentLayer.addView(splash, fill());
        GameEngine.get().whenReady(splash::setReady);
    }
}
