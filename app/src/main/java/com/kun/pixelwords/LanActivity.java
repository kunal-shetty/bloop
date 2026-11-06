package com.kun.pixelwords;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.kun.pixelwords.audio.Sfx;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.lan.LanClient;
import com.kun.pixelwords.lan.LanHost;
import com.kun.pixelwords.lan.LanMatch;
import com.kun.pixelwords.lan.LanPlayer;
import com.kun.pixelwords.services.LanDiscovery;
import com.kun.pixelwords.services.LanSession;
import com.kun.pixelwords.ui.AmbientView;
import com.kun.pixelwords.ui.PanelDrawable;
import com.kun.pixelwords.ui.PixelButton;
import com.kun.pixelwords.ui.PixelModal;
import com.kun.pixelwords.ui.PixelTheme;

import java.io.IOException;
import java.util.List;

/** The Wi-Fi lobby: host a room or join one on the same network, then wait for the race to start. */
public class LanActivity extends BaseActivity {

    private LanDiscovery discovery;
    private LanMatch match;
    private boolean racing; // the match now belongs to the game screen
    private LinearLayout list;
    private TextView status, youText;
    private PixelButton hostBtn, startBtn;

    @Override
    protected View createBackground() {
        AmbientView ambient = new AmbientView(this);
        ambient.setTheme(AmbientView.Theme.LEAVES);
        return ambient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        discovery = new LanDiscovery(this);
        buildUi();
        if (!LanDiscovery.onWifi(this)) {
            status.setText("Connect to Wi-Fi to race. Everyone needs to be on the same network.");
            hostBtn.setEnabled(false);
            return;
        }
        status.setText("Looking for rooms on your Wi-Fi...");
        discovery.search(this::showRooms);
        if (Settings.playerName().isEmpty()) askName();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        discovery.stop();
        if (!racing) LanSession.close();
    }

    private int dp(float v) {
        return (int) PixelTheme.dp(this, v);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(12), dp(14), dp(14));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        PixelButton back = new PixelButton(this, "", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        back.setIcon("back");
        back.setIconSize(24);
        back.setClickSound(Sfx.Id.UI_BACK);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = PixelTheme.display(this, "WI-FI RACE", 14, PixelTheme.GOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(new View(this), new LinearLayout.LayoutParams(dp(46), dp(46))); // keeps the title centred
        page.addView(bar, matchWrap(0));

        LinearLayout you = new LinearLayout(this);
        you.setGravity(Gravity.CENTER_VERTICAL);
        you.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
        you.setPadding(dp(12), dp(6), dp(6), dp(8));
        youText = PixelTheme.body(this, "", 18, PixelTheme.CREAM);
        you.addView(youText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        PixelButton change = new PixelButton(this, "CHANGE", PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
        change.setLabelSize(8);
        change.setOnClickListener(v -> askName());
        you.addView(change, new LinearLayout.LayoutParams(dp(92), dp(40)));
        page.addView(you, matchWrap(10));
        showYou();

        hostBtn = new PixelButton(this, "HOST A ROOM", PixelTheme.GOLD, PixelTheme.INK);
        hostBtn.setLabelSize(12);
        hostBtn.setIcon("home");
        hostBtn.setIconSize(22);
        hostBtn.setOnClickListener(v -> host());
        page.addView(hostBtn, matchWrap(10));

        startBtn = new PixelButton(this, "START RACE", PixelTheme.MINT, PixelTheme.INK);
        startBtn.setLabelSize(12);
        startBtn.setIcon("play");
        startBtn.setIconSize(22);
        startBtn.setVisibility(View.GONE);
        startBtn.setOnClickListener(v -> startRace());
        page.addView(startBtn, matchWrap(10));

        status = PixelTheme.body(this, "", 17, PixelTheme.TEXT);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, dp(4));
        page.addView(status, matchWrap(0));

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        contentLayer.addView(page, fill());
    }

    private LinearLayout.LayoutParams matchWrap(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    private LanPlayer self() {
        return new LanPlayer(Settings.playerId(), Settings.playerName(), Settings.playerTag());
    }

    private void showYou() {
        youText.setText("You: " + self().label());
    }

    private void askName() {
        EditText input = new EditText(this);
        input.setText(Settings.playerName());
        input.setSingleLine(true);
        input.setHint("Your name");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(LanPlayer.MAX_NAME_LENGTH)});
        input.setTypeface(PixelTheme.BODY);
        input.setTextColor(PixelTheme.TEXT);
        input.setHintTextColor(PixelTheme.TEXT_DIM);
        new PixelModal(this).title("YOUR NAME").icon("heart").accent(PixelTheme.GOLD)
                .body("This is what other players see. Your number stays the same, so friends can tell it's you.")
                .custom(input)
                .primary("SAVE", "check", () -> {
                    Settings.setPlayerName(LanPlayer.cleanName(input.getText().toString()));
                    showYou();
                })
                .show();
    }

    private void showRooms(List<LanDiscovery.Room> rooms) {
        if (match != null) return;
        list.removeAllViews();
        status.setText(rooms.isEmpty() ? "No rooms yet. Ask a friend to host, or host your own." : "Tap a room to join.");
        for (LanDiscovery.Room room : rooms) {
            PixelButton b = new PixelButton(this, room.name, PixelTheme.PANEL_LIGHT, PixelTheme.TEXT);
            b.setLabelSize(10);
            b.setIcon("swords");
            b.setIconSize(20);
            b.setOnClickListener(v -> join(room));
            list.addView(b, matchWrap(8));
        }
    }

    private void host() {
        hostBtn.setEnabled(false);
        new Thread(() -> {
            try {
                LanHost room = new LanHost(self());
                runOnUiThread(() -> {
                    enterRoom(room);
                    discovery.announce(self().label(), room.port());
                });
            } catch (IOException e) {
                runOnUiThread(() -> leaveRoom("Couldn't open a room on this network."));
            }
        }, "lan-host").start();
    }

    private void join(LanDiscovery.Room room) {
        status.setText("Joining " + room.name + "...");
        hostBtn.setEnabled(false);
        LanClient client = new LanClient(self());
        enterRoom(client);
        new Thread(() -> {
            try {
                client.connect(room.host, room.port);
            } catch (IOException e) {
                runOnUiThread(() -> leaveRoom("Couldn't reach that room. It may have closed."));
            }
        }, "lan-join").start();
    }

    private void enterRoom(LanMatch m) {
        match = m;
        LanSession.set(m);
        m.setListener(new LanMatch.Listener() {
            @Override public void onPlayersChanged() { runOnUiThread(LanActivity.this::showPlayers); }
            @Override public void onRaceStarted() { runOnUiThread(LanActivity.this::openRace); }
            @Override public void onScoresChanged() {}
            @Override public void onRaceEnded() {}
            @Override public void onDisconnected(String reason) { runOnUiThread(() -> leaveRoom(reason)); }
        });
        hostBtn.setVisibility(View.GONE);
        showPlayers();
    }

    private void showPlayers() {
        if (match == null) return;
        List<LanPlayer> players = match.players();
        list.removeAllViews();
        for (LanPlayer p : players) {
            TextView row = PixelTheme.body(this, p.label(), 18, PixelTheme.TEXT);
            row.setBackground(new PanelDrawable(0xE03A2E6E, Math.max(2f, dp(1))));
            row.setPadding(dp(12), dp(10), dp(12), dp(12));
            list.addView(row, matchWrap(8));
        }
        if (match.isHost()) {
            boolean ready = players.size() >= 2;
            startBtn.setVisibility(View.VISIBLE);
            startBtn.setEnabled(ready);
            status.setText(ready ? players.size() + " of " + LanMatch.MAX_PLAYERS + " players. Ready when you are!"
                    : "Waiting for friends to join...");
        } else if (!players.isEmpty()) {
            status.setText("Waiting for the host to start...");
        }
    }

    private void startRace() {
        startBtn.setEnabled(false);
        LanHost host = (LanHost) match;
        new Thread(() -> host.startRace(LanMatch.RACE_SECONDS), "lan-start").start();
    }

    private void openRace() {
        if (racing) return;
        racing = true;
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE, GameActivity.Mode.LAN.name());
        go(i);
        finish();
    }

    /** Back to the room list after a failed join or a closed room, with the reason on show. */
    private void leaveRoom(String reason) {
        LanSession.close();
        match = null;
        discovery.stopAnnouncing();
        startBtn.setVisibility(View.GONE);
        hostBtn.setVisibility(View.VISIBLE);
        hostBtn.setEnabled(true);
        list.removeAllViews();
        status.setText(reason);
    }
}
