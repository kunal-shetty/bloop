package com.kun.pixelwords.audio;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.kun.pixelwords.data.Settings;

/** One vocabulary of buzzes for the whole game, so every action has a consistent feel. */
public final class Haptics {

    private static Vibrator vibrator;
    private static boolean amplitude;

    private Haptics() {}

    public static void init(Context context) {
        vibrator = (Vibrator) context.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        amplitude = vibrator != null && Build.VERSION.SDK_INT >= 26 && vibrator.hasAmplitudeControl();
    }

    /** Lightest: a letter under the finger, a button going down. */
    public static void tick() {
        if (Build.VERSION.SDK_INT >= 29) predefined(VibrationEffect.EFFECT_TICK, 8);
        else oneShot(8, 70);
    }

    /** A button released, a tile landing. */
    public static void click() {
        if (Build.VERSION.SDK_INT >= 29) predefined(VibrationEffect.EFFECT_CLICK, 14);
        else oneShot(14, 120);
    }

    /** Something heavier: a modal opening, the logo slam. */
    public static void thud() {
        if (Build.VERSION.SDK_INT >= 29) predefined(VibrationEffect.EFFECT_HEAVY_CLICK, 26);
        else oneShot(26, 200);
    }

    /** A correct word: two rising taps. */
    public static void success() {
        wave(new long[]{0, 16, 50, 26}, new int[]{0, 110, 0, 220});
    }

    /** A wrong word: a rough double buzz. */
    public static void error() {
        wave(new long[]{0, 40, 40, 60}, new int[]{0, 200, 0, 140});
    }

    /** Level complete: a little drum roll that ends on a big hit. */
    public static void celebrate() {
        wave(new long[]{0, 18, 60, 18, 60, 18, 60, 90}, new int[]{0, 90, 0, 130, 0, 170, 0, 255});
    }

    /** Out of time, or a lost duel: one long fading rumble. */
    public static void fail() {
        wave(new long[]{0, 90, 30, 60, 30, 40}, new int[]{0, 220, 0, 150, 0, 90});
    }

    private static boolean off() {
        return vibrator == null || !Settings.haptics() || !vibrator.hasVibrator();
    }

    private static void predefined(int effect, long fallbackMs) {
        if (off()) return;
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(effect));
        else oneShot(fallbackMs, 150);
    }

    private static void oneShot(long ms, int strength) {
        if (off()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude ? strength : VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            legacy(ms);
        }
    }

    private static void wave(long[] timings, int[] strengths) {
        if (off()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(amplitude
                    ? VibrationEffect.createWaveform(timings, strengths, -1)
                    : VibrationEffect.createWaveform(timings, -1));
        } else {
            legacyWave(timings);
        }
    }

    @SuppressWarnings("deprecation")
    private static void legacy(long ms) {
        vibrator.vibrate(ms);
    }

    @SuppressWarnings("deprecation")
    private static void legacyWave(long[] timings) {
        vibrator.vibrate(timings, -1);
    }
}
