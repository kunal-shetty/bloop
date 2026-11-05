package com.kun.pixelwords.services;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.kun.pixelwords.R;

/**
 * AdMob rewarded video (watch an ad for a hint). Uses Google's public TEST ad unit until you put your
 * own in strings.xml.
 *
 * Before release you also need: a consent flow (Google's UMP SDK) where required, a privacy policy,
 * and the Play Console Data safety form.
 *
 * NOTE: written from memory of the play-services-ads API and not compiled in the authoring environment.
 */
public final class AdsService {

    private static AdsService instance;

    private final Context app;
    private final String unitId;
    private RewardedAd rewarded;
    private boolean loading;

    public static synchronized AdsService get(Context context) {
        if (instance == null) instance = new AdsService(context.getApplicationContext());
        return instance;
    }

    private AdsService(Context app) {
        this.app = app;
        this.unitId = app.getString(R.string.admob_rewarded_unit);
        MobileAds.initialize(app, new OnInitializationCompleteListener() {
            @Override public void onInitializationComplete(InitializationStatus status) {
                load();
            }
        });
    }

    public boolean isReady() {
        return rewarded != null;
    }

    public void load() {
        if (loading || rewarded != null) return;
        loading = true;
        RewardedAd.load(app, unitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewarded = ad;
                loading = false;
            }

            @Override public void onAdFailedToLoad(LoadAdError error) {
                rewarded = null;
                loading = false;
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
        ad.show(activity, new OnUserEarnedRewardListener() {
            @Override public void onUserEarnedReward(RewardItem item) {
                onReward.run();
            }
        });
    }
}
