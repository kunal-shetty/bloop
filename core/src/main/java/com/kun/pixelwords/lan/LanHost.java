package com.kun.pixelwords.lan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** The phone that opened the room. It runs the server, and its word is final on who's in and when it ends. */
public final class LanHost extends LanMatch {

    /** Covers the countdown and a little lag, on top of the race itself. */
    private static final long DEFAULT_GRACE_MS = 8_000;

    private final ServerSocket server;
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Set<String> finished = new HashSet<>();
    private final long graceMs;
    private volatile boolean started;

    public LanHost(LanPlayer self) throws IOException {
        this(self, DEFAULT_GRACE_MS);
    }

    /** @param graceMs how long after the race time the host stops waiting for stragglers */
    public LanHost(LanPlayer self, long graceMs) throws IOException {
        super(self.id);
        this.graceMs = graceMs;
        players.put(self.id, self);
        server = new ServerSocket(0); // any free port; other phones find it through discovery
        Thread accept = new Thread(this::acceptLoop, "lan-accept");
        accept.setDaemon(true);
        accept.start();
    }

    @Override
    public boolean isHost() {
        return true;
    }

    public int port() {
        return server.getLocalPort();
    }

    /** Sends everyone into the race on one shared board. Needs at least one other player. */
    public boolean startRace(int seconds) {
        if (started || players().size() < 2) return false;
        started = true;
        this.seconds = seconds;
        seed = new SecureRandom().nextLong();
        broadcast(line(START, seed, seconds));
        Thread deadline = new Thread(() -> enforceDeadline(seconds * 1000L + graceMs), "lan-deadline");
        deadline.setDaemon(true);
        deadline.start();
        listener.onRaceStarted();
        return true;
    }

    /** A phone that stalls or pauses can't hold up everyone else's results. */
    private void enforceDeadline(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            return;
        }
        announceEnd();
    }

    @Override
    public void sendScore(int points) {
        setScore(localId, points);
        broadcast(line(SCORE, localId, points));
        listener.onScoresChanged();
    }

    @Override
    public void finish(int points, boolean solved) {
        sendScore(points);
        playerDone(localId, solved);
    }

    private void playerDone(String id, boolean solved) {
        boolean everyone;
        synchronized (finished) {
            finished.add(id);
            everyone = allFinished();
        }
        if (solved || everyone) announceEnd();
    }

    private boolean allFinished() {
        for (LanPlayer p : players()) if (!finished.contains(p.id)) return false;
        return true;
    }

    private void announceEnd() {
        if (raceEnded) return;
        broadcast(line(END));
        endRace();
    }

    private void broadcast(String message) {
        for (Client c : clients.values()) c.send(message);
    }

    private String playersMessage() {
        StringBuilder sb = new StringBuilder(PLAYERS);
        for (LanPlayer p : players()) {
            sb.append(SEPARATOR).append(p.id).append(PLAYER_SEPARATOR).append(p.name)
                    .append(PLAYER_SEPARATOR).append(p.tag).append(PLAYER_SEPARATOR).append(p.score);
        }
        return sb.toString();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = server.accept();
                Thread reader = new Thread(() -> new Client(socket).run(), "lan-client");
                reader.setDaemon(true);
                reader.start();
            } catch (IOException e) {
                return; // close() shut the server socket
            }
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            server.close();
        } catch (IOException ignored) {
            // already closing
        }
        for (Client c : new ArrayList<>(clients.values())) c.drop();
        sender.shutdown();
    }

    /** One connected phone, read on its own thread. */
    private final class Client {
        private final Socket socket;
        private PrintWriter out;
        private String id;

        Client(Socket socket) {
            this.socket = socket;
        }

        void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String hello = in.readLine();
                if (hello == null || !admit(fields(hello))) return;
                for (String line = in.readLine(); line != null; line = in.readLine()) handle(fields(line));
            } catch (IOException e) {
                // the phone went away; handled below
            } finally {
                left();
            }
        }

        private boolean admit(String[] f) {
            if (f.length < 5 || !HELLO.equals(f[0])) return refuse("That isn't a Bloop game.");
            if (!String.valueOf(PROTOCOL_VERSION).equals(f[1])) return refuse("You and the host need the same version of Bloop.");
            if (started) return refuse("The race has already started.");
            synchronized (players) {
                if (players.containsKey(f[2])) return refuse("You're already in this room.");
                if (players.size() >= MAX_PLAYERS) return refuse("This room is full.");
                id = f[2];
                players.put(id, new LanPlayer(id, f[3], f[4]));
            }
            clients.put(id, this);
            broadcast(playersMessage());
            listener.onPlayersChanged();
            return true;
        }

        private boolean refuse(String reason) {
            out.println(line(REJECT, reason));
            return false;
        }

        private void handle(String[] f) {
            try {
                if (SCORE.equals(f[0]) && f.length >= 2) {
                    int points = Integer.parseInt(f[1]);
                    setScore(id, points);
                    broadcast(line(SCORE, id, points));
                    listener.onScoresChanged();
                } else if (DONE.equals(f[0]) && f.length >= 2) {
                    playerDone(id, Boolean.parseBoolean(f[1]));
                }
            } catch (NumberFormatException ignored) {
                // a garbled score changes nothing
            }
        }

        void send(String message) {
            if (closed || sender.isShutdown()) return;
            sender.execute(() -> {
                if (out != null) out.println(message);
            });
        }

        void drop() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // already gone
            }
        }

        private void left() {
            drop();
            if (id == null || closed) return;
            clients.remove(id);
            synchronized (players) {
                players.remove(id);
            }
            broadcast(playersMessage());
            listener.onPlayersChanged();
            if (started) {
                boolean everyone;
                synchronized (finished) {
                    everyone = allFinished();
                }
                if (everyone) announceEnd();
            }
        }
    }
}
