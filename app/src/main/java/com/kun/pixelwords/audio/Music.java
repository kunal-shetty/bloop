package com.kun.pixelwords.audio;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import com.kun.pixelwords.BaseActivity;
import com.kun.pixelwords.data.Settings;

import java.io.IOException;
import java.util.Random;

/**
 * Endless background music: the tracks play in turn, forever, while one of the game's screens is
 * visible. It stops for phone calls and ads, and stays silent if the player already has their own
 * music going. Only BaseActivity screens count, so an ad's full-screen activity pauses it.
 */
public final class Music implements Application.ActivityLifecycleCallbacks, AudioManager.OnAudioFocusChangeListener {

    private static final String[] TRACKS = {"dawn", "drift", "lantern"};
    private static final float VOLUME = 0.32f;
    private static final float DUCKED_VOLUME = 0.1f;

    private static Music instance;

    private final Context context;
    private final AudioManager audio;
    private MediaPlayer player;
    private int track;
    private int screensVisible;
    private boolean ready;    // the player has loaded its track
    private boolean playing;  // music should be sounding right now
    private boolean focused;  // Android has given us the audio

    private Music(Context context) {
        this.context = context;
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        track = new Random().nextInt(TRACKS.length);
    }

    public static void init(Application app) {
        if (instance != null) return;
        instance = new Music(app);
        app.registerActivityLifecycleCallbacks(instance);
    }

    /** Call after the player changes the music setting. */
    public static void refresh() {
        if (instance != null) instance.update();
    }

    private void update() {
        if (screensVisible > 0 && Settings.music()) start();
        else stop();
    }

    @SuppressWarnings("deprecation")
    private void start() {
        if (playing) return;
        if (!focused) {
            if (audio.isMusicActive()) return; // their own music wins
            int result = audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;
            focused = true;
        }
        playing = true;
        if (player == null) load();
        else if (ready) player.start();
    }

    @SuppressWarnings("deprecation")
    private void stop() {
        pause();
        if (focused) audio.abandonAudioFocus(this);
        focused = false;
    }

    private void pause() {
        if (playing && ready) player.pause();
        playing = false;
    }

    private void load() {
        ready = false;
        MediaPlayer next = new MediaPlayer();
        next.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        next.setVolume(VOLUME, VOLUME);
        next.setOnPreparedListener(mp -> {
            ready = true;
            if (playing) mp.start();
        });
        next.setOnCompletionListener(mp -> advance());
        next.setOnErrorListener((mp, what, extra) -> {
            Log.w("Music", "playback error " + what + "/" + extra);
            release();
            playing = false;
            return true;
        });
        try (AssetFileDescriptor fd = context.getAssets().openFd("music/" + TRACKS[track] + ".ogg")) {
            next.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            player = next;
            next.prepareAsync();
        } catch (IOException e) {
            Log.w("Music", "missing track " + TRACKS[track], e);
            next.release();
            playing = false;
        }
    }

    private void advance() {
        release();
        track = (track + 1) % TRACKS.length;
        if (playing) load();
    }

    private void release() {
        if (player != null) player.release();
        player = null;
        ready = false;
    }

    @Override
    public void onAudioFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (player != null) player.setVolume(VOLUME, VOLUME);
                update();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (player != null) player.setVolume(DUCKED_VOLUME, DUCKED_VOLUME);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause(); // a call: focus comes back to us afterwards
                break;
            default:
                stop();
                break;
        }
    }

    // A new screen starts before the old one stops, so moving between screens never interrupts the music.
    @Override
    public void onActivityStarted(Activity activity) {
        if (!(activity instanceof BaseActivity)) return;
        screensVisible++;
        update();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!(activity instanceof BaseActivity)) return;
        screensVisible--;
        update();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
