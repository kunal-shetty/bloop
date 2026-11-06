package com.kun.pixelwords.lan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Someone in a Wi-Fi race. The id never changes; the name and four-digit tag are what people see. */
public final class LanPlayer {

    public static final int MAX_NAME_LENGTH = 12;
    private static final String DEFAULT_NAME = "Player";

    public final String id;
    public final String name;
    public final String tag;
    public volatile int score;

    public LanPlayer(String id, String name, String tag) {
        this.id = id;
        this.name = cleanName(name);
        this.tag = tag;
    }

    /** What other players see, such as Kunal#4821, so two people called Kunal can tell who's who. */
    public String label() {
        return name + "#" + tag;
    }

    /** Keeps names short and printable, and free of anything that could break the message format. */
    public static String cleanName(String raw) {
        String name = raw == null ? "" : raw.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        if (name.isEmpty()) return DEFAULT_NAME;
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH).trim() : name;
    }

    /** Highest score first. Players on the same score keep their order. */
    public static List<LanPlayer> ranked(List<LanPlayer> players) {
        List<LanPlayer> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparingInt((LanPlayer p) -> p.score).reversed());
        return sorted;
    }
}
