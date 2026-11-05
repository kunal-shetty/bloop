package com.kun.pixelwords.services;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.games.AuthenticationResult;
import com.kun.pixelwords.R;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Google Play Games Services (v2): sign-in, leaderboards and cloud save.
 *
 * NOTE: written against the play-services-games-v2 API from memory and not compiled in the authoring
 * environment. If a signature differs in the version you pin, the compiler will point at the exact
 * line; the structure should not need to change.
 */
public final class PlayGamesService {

    public interface Callback<T> {
        void onResult(T value);
    }

    private static final String SNAPSHOT_NAME = "progress";
    private static final int REQUEST_LEADERBOARD = 9004;
    private static final String PLACEHOLDER = "REPLACE_ME";

    private PlayGamesService() {}

    /** False until a real Play Console game ID is in strings.xml; sign-in cannot work without one. */
    public static boolean isConfigured(Context c) {
        return !c.getString(R.string.play_games_app_id).matches("0+");
    }

    public static boolean hasLeaderboards(Context c) {
        return !PLACEHOLDER.equals(c.getString(R.string.leaderboard_daily));
    }

    public static void initialize(Application app) {
        PlayGamesSdk.initialize(app);
    }

    public static void isSignedIn(Activity a, final Callback<Boolean> cb) {
        PlayGames.getGamesSignInClient(a).isAuthenticated()
                .addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                    @Override public void onComplete(Task<AuthenticationResult> t) {
                        cb.onResult(t.isSuccessful() && t.getResult().isAuthenticated());
                    }
                });
    }

    public static void signIn(Activity a, final Callback<Boolean> cb) {
        PlayGames.getGamesSignInClient(a).signIn()
                .addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                    @Override public void onComplete(Task<AuthenticationResult> t) {
                        cb.onResult(t.isSuccessful() && t.getResult().isAuthenticated());
                    }
                });
    }

    public static void submitScore(Activity a, String leaderboardId, long score) {
        if (PLACEHOLDER.equals(leaderboardId)) return; // not configured yet
        PlayGames.getLeaderboardsClient(a).submitScore(leaderboardId, score);
    }

    public static void showLeaderboard(final Activity a, String leaderboardId) {
        if (PLACEHOLDER.equals(leaderboardId)) return;
        PlayGames.getLeaderboardsClient(a).getLeaderboardIntent(leaderboardId)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override public void onSuccess(Intent intent) {
                        a.startActivityForResult(intent, REQUEST_LEADERBOARD);
                    }
                });
    }

    /** Writes the serialized Progress to the player's Play Games saved game. Silent no-op if not signed in. */
    public static void saveProgress(Activity a, final String data) {
        final SnapshotsClient client = PlayGames.getSnapshotsClient(a);
        client.open(SNAPSHOT_NAME, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                    @Override public void onComplete(Task<SnapshotsClient.DataOrConflict<Snapshot>> t) {
                        if (!t.isSuccessful() || t.getResult().isConflict()) return;
                        Snapshot snapshot = t.getResult().getData();
                        snapshot.getSnapshotContents().writeBytes(data.getBytes(StandardCharsets.UTF_8));
                        client.commitAndClose(snapshot, SnapshotMetadataChange.EMPTY_CHANGE);
                    }
                });
    }

    /** Reads the saved game; calls back with the serialized Progress, or null if none / not signed in. */
    public static void loadProgress(Activity a, final Callback<String> cb) {
        final SnapshotsClient client = PlayGames.getSnapshotsClient(a);
        client.open(SNAPSHOT_NAME, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
                .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                    @Override public void onComplete(Task<SnapshotsClient.DataOrConflict<Snapshot>> t) {
                        if (!t.isSuccessful() || t.getResult().isConflict()) {
                            cb.onResult(null);
                            return;
                        }
                        Snapshot snapshot = t.getResult().getData();
                        String text = null;
                        try {
                            byte[] bytes = snapshot.getSnapshotContents().readFully();
                            if (bytes != null && bytes.length > 0) text = new String(bytes, StandardCharsets.UTF_8);
                        } catch (IOException ignored) {
                            // treat as no cloud data
                        }
                        client.discardAndClose(snapshot);
                        cb.onResult(text);
                    }
                });
    }
}
