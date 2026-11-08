package com.kun.pixelwords;

/** Switches for parts of the game that are built but not ready to ship. */
public final class Features {

    private Features() {}

    /**
     * Ads and real-money purchases. Off until the game is verified on Play. Set this to true to bring
     * back ad breaks, videos, consent and the coin store: nothing else needs to change.
     */
    public static final boolean MONETIZATION = true;
}
