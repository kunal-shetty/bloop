package com.kun.pixelwords.services;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.kun.pixelwords.R;
import com.kun.pixelwords.game.AdPolicy;
import com.kun.pixelwords.game.Progress;

/**
 * AdMob: rewarded videos the player chooses to watch, and ad breaks between levels (see AdPolicy).
 * Nothing loads until Google's consent check allows it.
 *
 * Debug builds take Google's test IDs from src/debug/res/values/admob.xml. Never tap real ads on your
 * own phone: AdMob can close the account for it.
 */
public final class AdsService {

    private static AdsService instance;

    private final Context app;
    private final ConsentInformation consent;
    private final String rewardedUnit;
    private final String interstitialUnit;
    private boolean started;
    private RewardedAd rewarded;
    private boolean loadingRewarded;
    private InterstitialAd interstitial;
    private boolean loadingInterstitial;
    private int levelsSinceBreak;
    private long lastBreakAt = -AdPolicy.MIN_MILLIS_BETWEEN_AD_BREAKS;

    public static synchronized AdsService get(Context context) {
        if (instance == null) instance = new AdsService(context.getApplicationContext());
        return instance;
    }

    private AdsService(Context app) {
        this.app = app;
        this.consent = UserMessagingPlatform.getConsentInformation(app);
        this.rewardedUnit = app.getString(R.string.admob_rewarded_unit);
        this.interstitialUnit = app.getString(R.string.admob_interstitial_unit);
    }

    /** Asks for consent if the player's region requires it, then starts ads if they're allowed. */
    public void gatherConsent(Activity activity) {
        consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> startIfAllowed()),
                error -> startIfAllowed());
        startIfAllowed(); // an answer from an earlier session still counts while this check runs
    }

    private void startIfAllowed() {
        if (started || !consent.canRequestAds()) return;
        started = true;
        MobileAds.initialize(app, status -> {
            load();
            loadInterstitial();
        });
    }

    /** True if the player's region lets them change their answer, so Settings must offer it. */
    public boolean hasPrivacyOptions() {
        return consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showPrivacyOptions(Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> startIfAllowed());
    }

    public void load() {
        if (!started || loadingRewarded || rewarded != null) return;
        loadingRewarded = true;
        RewardedAd.load(app, rewardedUnit, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewarded = ad;
                loadingRewarded = false;
            }

            @Override public void onAdFailedToLoad(LoadAdError error) {
                rewarded = null;
                loadingRewarded = false;
            }
        });
    }

    private void loadInterstitial() {
        if (!started || loadingInterstitial || interstitial != null) return;
        loadingInterstitial = true;
        InterstitialAd.load(app, interstitialUnit, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) {
                interstitial = ad;
                loadingInterstitial = false;
            }

            @Override public void onAdFailedToLoad(LoadAdError error) {
                interstitial = null;
                loadingInterstitial = false;
            }
        });
    }

    /**
     * Shows the ad if one is loaded. {@code onReward} runs only if the player watched enough to earn it;
     * {@code onUnavailable} runs if there was no ad to show.
     */
    public void showRewarded(Activity activity, final Runnable onReward, Runnable onUnavailable) {
        final RewardedAd ad = rewarded;
        if (ad == null) {
            load();
            if (onUnavailable != null) onUnavailable.run();
            return;
        }
        rewarded = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                load(); // preload the next one
            }

            @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                load();
            }
        });
        ad.show(activity, item -> onReward.run());
    }

    /**
     * Call as the player moves on to the next level. Runs {@code next} straight away, or after an ad
     * break if one is due. A break that isn't ready is skipped, so the player never waits for an ad.
     */
    public void betweenLevels(Activity activity, Progress progress, final Runnable next) {
        levelsSinceBreak++;
        long sinceBreak = SystemClock.elapsedRealtime() - lastBreakAt;
        final InterstitialAd ad = interstitial;
        if (ad == null || !AdPolicy.adBreakDue(progress, levelsSinceBreak, sinceBreak)) {
            loadInterstitial();
            next.run();
            return;
        }
        interstitial = null;
        levelsSinceBreak = 0;
        lastBreakAt = SystemClock.elapsedRealtime();
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                loadInterstitial();
                next.run();
            }

            @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                loadInterstitial();
                next.run();
            }
        });
        ad.show(activity);
    }
}
