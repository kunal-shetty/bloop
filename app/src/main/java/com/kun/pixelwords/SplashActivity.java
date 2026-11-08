package com.kun.pixelwords;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.kun.pixelwords.data.GameEngine;
import com.kun.pixelwords.services.PlayGamesService;
import com.kun.pixelwords.ui.SplashView;

/**
 * Plays the animated boot sequence, and holds on it until the dictionary has loaded and Google Play Games
 * has finished its sign-in check. That check can show Google's account prompt; opening the menu before it
 * ends would bury the prompt under the game, and the player would never get signed in.
 */
public class SplashActivity extends BaseActivity {

    /** Gives up waiting on Google after this long, so a stuck check can't keep the game from starting. */
    private static final long SIGN_IN_WAIT_MS = 30_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SplashView splash;
    private boolean dictionaryReady, signInChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splash = new SplashView(this);
        splash.setListener(() -> {
            go(new Intent(this, MainActivity.class));
            finish();
        });
        contentLayer.addView(splash, fill());
        GameEngine.get().whenReady(() -> {
            dictionaryReady = true;
            readyIfDone();
        });
        if (PlayGamesService.isConfigured(this)) {
            PlayGamesService.isSignedIn(this, signedIn -> signInDone());
            handler.postDelayed(this::signInDone, SIGN_IN_WAIT_MS);
        } else {
            signInDone();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void signInDone() {
        if (signInChecked) return;
        signInChecked = true;
        readyIfDone();
    }

    private void readyIfDone() {
        if (dictionaryReady && signInChecked) splash.setReady();
    }
}
