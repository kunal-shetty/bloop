package com.kun.pixelwords.game;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Gift codes. There's no server, so the codes ship with the app, as hashes so they can't be read out of
 * it. Each one works once per save, and a bit in the save remembers it. To add a code, run
 * {@code ./gradlew :core:codeHash -Pcode=YOURCODE} and add a line with a new id. Never renumber.
 */
public final class RedeemCode {

    public enum Status { OK, UNKNOWN, ALREADY_USED, EXPIRED }

    /** What came of trying a code. {@code rewards} is only filled in when the status is OK. */
    public static final class Result {
        public final Status status;
        public final List<Reward> rewards;

        private Result(Status status, List<Reward> rewards) {
            this.status = status;
            this.rewards = rewards;
        }
    }

    private static final class Entry {
        final int id;
        final long hash;
        final long lastDay;
        final Reward[] rewards;

        Entry(int id, long hash, long lastDay, Reward... rewards) {
            this.id = id;
            this.hash = hash;
            this.lastDay = lastDay;
            this.rewards = rewards;
        }
    }

    /** Pass this as the last day for a code that never runs out. */
    private static final long FOREVER = Long.MAX_VALUE;
    private static final String SALT = "bloop-gift-code:";
    private static final int HASH_BYTES = 8;

    private static final Entry[] CODES = {
            new Entry(0, 0x7cf1b43209b6a8e3L, FOREVER, Reward.coins(200), Reward.hints(2)),
            new Entry(1, 0xb4008750837a73beL, FOREVER, Reward.coins(150), Reward.powerUp(PowerUps.Kind.REVEAL_WORD, 1)),
            new Entry(2, 0xc2f6ecb4a613a361L, FOREVER, Reward.coins(300), Reward.sticker()),
    };

    private RedeemCode() {}

    /** Case, spaces and dashes don't matter when typing a code. */
    private static String tidy(String typed) {
        return typed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    public static long hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest((SALT + tidy(code)).getBytes(StandardCharsets.UTF_8));
            long out = 0;
            for (byte b : Arrays.copyOf(digest, HASH_BYTES)) out = out << 8 | (b & 0xFF);
            return out;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every Java platform must provide SHA-256
        }
    }

    public static Result redeem(Progress p, String typed, long today) {
        long hash = hash(typed);
        for (Entry code : CODES) {
            if (code.hash != hash) continue;
            if ((p.redeemedCodes & 1L << code.id) != 0) return new Result(Status.ALREADY_USED, new ArrayList<>());
            if (today > code.lastDay) return new Result(Status.EXPIRED, new ArrayList<>());
            p.redeemedCodes |= 1L << code.id;
            List<Reward> paid = new ArrayList<>();
            for (Reward r : code.rewards) paid.add(r.grant(p, today + paid.size()));
            return new Result(Status.OK, paid);
        }
        return new Result(Status.UNKNOWN, new ArrayList<>());
    }

    /** Prints a code's hash, for adding it to the list. */
    public static void main(String[] args) {
        System.out.println("0x" + Long.toHexString(hash(args[0])) + "L");
    }
}
