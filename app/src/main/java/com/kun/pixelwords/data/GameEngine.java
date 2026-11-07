package com.kun.pixelwords.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.kun.pixelwords.game.Definitions;
import com.kun.pixelwords.generator.Dictionary;
import com.kun.pixelwords.generator.LevelGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the shipped dictionary once, off the main thread, and exposes the level generator.
 * Screens call {@link #whenReady(Runnable)} and get called back on the main thread.
 */
public final class GameEngine {

    private static GameEngine instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Runnable> waiting = new ArrayList<>();
    private volatile LevelGenerator generator;
    private volatile Exception failure;
    private volatile Definitions definitions;

    public static synchronized void init(final Context context) {
        if (instance != null) return;
        instance = new GameEngine();
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() { instance.load(app); }
        }, "dictionary-loader").start();
    }

    public static GameEngine get() {
        if (instance == null) throw new IllegalStateException("GameEngine.init() was not called");
        return instance;
    }

    private void load(Context app) {
        try (InputStream words = app.getAssets().open("words.txt");
             InputStream common = app.getAssets().open("common.txt")) {
            // The shipped files are already filtered by :core:prepareDictionary; pass no bad-word list.
            Dictionary dict = Dictionary.load(words, common, null);
            generator = new LevelGenerator(dict);
        } catch (Exception e) {
            failure = e;
        }
        try (InputStream meanings = app.getAssets().open("definitions.txt")) {
            definitions = Definitions.load(meanings);
        } catch (IOException e) {
            definitions = null; // the game plays fine without them: tapping a word just explains nothing
        }
        final List<Runnable> toRun;
        synchronized (this) {
            toRun = new ArrayList<>(waiting);
            waiting.clear();
        }
        for (final Runnable r : toRun) main.post(r);
    }

    /** What words mean, or null if they haven't loaded or couldn't be. */
    public Definitions definitions() {
        return definitions;
    }

    /** Runs {@code r} on the main thread as soon as the generator is available. */
    public void whenReady(Runnable r) {
        synchronized (this) {
            if (generator == null && failure == null) {
                waiting.add(r);
                return;
            }
        }
        main.post(r);
    }

    /** Non-null once ready. */
    public LevelGenerator generator() {
        if (failure != null) throw new IllegalStateException("Dictionary failed to load", failure);
        return generator;
    }
}
