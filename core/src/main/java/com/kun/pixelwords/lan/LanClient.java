package com.kun.pixelwords.lan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** A phone that joined someone else's room. Call connect() away from the UI thread. */
public final class LanClient extends LanMatch {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final LanPlayer self;
    private final Socket socket = new Socket();
    private PrintWriter out;

    public LanClient(LanPlayer self) {
        super(self.id);
        this.self = self;
    }

    /** Joins the room, and reads from it on a background thread until the connection ends. */
    public void connect(InetAddress host, int port) throws IOException {
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        out.println(line(HELLO, PROTOCOL_VERSION, self.id, self.name, self.tag));
        Thread reader = new Thread(this::readLoop, "lan-read");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        String reason = "The host left the room.";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                String[] f = fields(line);
                if (REJECT.equals(f[0]) && f.length >= 2) {
                    reason = f[1];
                    break;
                }
                handle(f);
            }
        } catch (IOException e) {
            reason = "Lost the connection to the host.";
        }
        if (!closed && !raceEnded) listener.onDisconnected(reason);
        close();
    }

    private void handle(String[] f) {
        try {
            if (PLAYERS.equals(f[0])) {
                readPlayers(f);
                listener.onPlayersChanged();
            } else if (START.equals(f[0]) && f.length >= 3) {
                seed = Long.parseLong(f[1]);
                seconds = Integer.parseInt(f[2]);
                listener.onRaceStarted();
            } else if (SCORE.equals(f[0]) && f.length >= 3) {
                if (!localId.equals(f[1])) setScore(f[1], Integer.parseInt(f[2]));
                listener.onScoresChanged();
            } else if (END.equals(f[0])) {
                endRace();
            }
        } catch (NumberFormatException ignored) {
            // a garbled message changes nothing
        }
    }

    private void readPlayers(String[] f) {
        synchronized (players) {
            players.clear();
            for (int i = 1; i < f.length; i++) {
                String[] p = f[i].split(Pattern.quote(PLAYER_SEPARATOR), -1);
                if (p.length < 4) continue;
                LanPlayer player = new LanPlayer(p[0], p[1], p[2]);
                player.score = Integer.parseInt(p[3]);
                players.put(player.id, player);
            }
        }
    }

    @Override
    public void sendScore(int points) {
        setScore(localId, points);
        send(line(SCORE, points));
    }

    @Override
    public void finish(int points, boolean solved) {
        sendScore(points);
        send(line(DONE, solved));
    }

    private void send(String message) {
        if (closed || sender.isShutdown()) return;
        sender.execute(() -> {
            if (out != null) out.println(message);
        });
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        sender.shutdown();
        try {
            socket.close();
        } catch (IOException ignored) {
            // already gone
        }
    }
}
