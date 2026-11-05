package com.kun.pixelwords.services;

/**
 * In-app purchases: NOT IMPLEMENTED YET. This file fixes the product list and the seam the game
 * uses, so the rest of the app does not change when Google Play Billing is wired in.
 *
 * Planned products (create them in Play Console > Monetize > Products):
 *   coins_small, coins_medium, coins_large  - consumable coin packs
 *   hints_pack                              - consumable hint tokens
 *   streak_freeze                           - consumable, protects a streak for one missed day
 *   remove_ads                              - one-time; removes optional ad prompts
 *
 * Implementation notes for later: add com.android.billingclient:billing, connect a BillingClient,
 * query product details, launch the purchase flow, then call acknowledgePurchase /
 * consumeAsync and grant the reward in Progress. Verify purchases server-side before trusting them
 * for anything you cannot afford to have spoofed.
 */
public final class BillingService {

    public static final String COINS_SMALL = "coins_small";
    public static final String COINS_MEDIUM = "coins_medium";
    public static final String COINS_LARGE = "coins_large";
    public static final String HINTS_PACK = "hints_pack";
    public static final String STREAK_FREEZE = "streak_freeze";
    public static final String REMOVE_ADS = "remove_ads";

    private BillingService() {}

    public static boolean isAvailable() {
        return false;
    }
}
