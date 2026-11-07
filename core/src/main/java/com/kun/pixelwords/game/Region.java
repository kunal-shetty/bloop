package com.kun.pixelwords.game;

import com.kun.pixelwords.generator.SeedUtil;

import java.util.Random;

/**
 * Where a Journey chapter takes place: its name, colours, land and weather. The first four are
 * hand-made, and every chapter after that is built from its number, so no region ever comes round
 * again however far a player gets, and nothing about them needs saving.
 */
public final class Region {

    /** What falls, rises or drifts through a region. Speeds are per dp, so they look the same on any screen. */
    public enum Weather {
        SNOW(0f, 40f, 0f, false, 1f),
        PETALS(-25f, 30f, 18f, false, 1f),
        FIREFLIES(0f, 0f, 14f, true, 1f),
        LEAVES(-35f, 45f, 22f, false, 1f),
        RAIN(-14f, 240f, 0f, false, 4f),
        EMBERS(8f, -38f, 14f, false, 1f),
        BUBBLES(0f, -26f, 10f, false, 1f),
        STARDUST(10f, 7f, 8f, false, 1f),
        SAND(-80f, 9f, 10f, false, 0.6f),
        ASH(-8f, 16f, 10f, false, 1f);

        public final float vx, vy, sway;
        /** Wanders on the spot instead of crossing the screen. */
        public final boolean wanders;
        /** How tall a particle is, for its width. */
        public final float stretch;

        Weather(float vx, float vy, float sway, boolean wanders, float stretch) {
            this.vx = vx;
            this.vy = vy;
            this.sway = sway;
            this.wanders = wanders;
            this.stretch = stretch;
        }
    }

    private static final Region[] HAND_MADE = {
            new Region(0, "FROSTFALL", 0xFF0F1A3D, 0xFF2C4A7E, 0xFF22375F, 0xFF16223F, 0xFFE8F4FF, 0xFFFFFFFF, Weather.SNOW),
            new Region(1, "PETAL DRIFT", 0xFF2A1240, 0xFF8A3B6E, 0xFF4F2358, 0xFF2E1538, 0xFFFFD9E6, 0xFFFFB3C7, Weather.PETALS),
            new Region(2, "FIREFLY WOODS", 0xFF0C1030, 0xFF1E5A66, 0xFF143E46, 0xFF0B2228, 0xFFF4F7C4, 0xFFE9FF7A, Weather.FIREFLIES),
            new Region(3, "EMBER LEAVES", 0xFF2B1433, 0xFFB5552E, 0xFF5C2A24, 0xFF321618, 0xFFFFC56B, 0xFFFF9A3C, Weather.LEAVES),
    };

    private static final String[] FIRST_WORDS = {
            "AMBER", "ASHEN", "AZURE", "BRAMBLE", "BRASS", "BRIGHT", "CINDER", "CLOVER", "COBALT", "COPPER", "CORAL",
            "CRIMSON", "CRYSTAL", "DAWN", "DEEP", "DUSK", "ECHO", "EMERALD", "FERN", "FOG", "GILDED", "GLASS", "GOLDEN",
            "HUSHED", "HONEY", "INDIGO", "IRON", "IVORY", "JADE", "LANTERN", "LILAC", "LOST", "LUNAR", "MAPLE", "MARBLE",
            "MIDNIGHT", "MIST", "MOSS", "OAK", "OPAL", "PEARL", "PINE", "QUIET", "RAVEN", "ROSE", "RUBY", "RUSTY", "SAFFRON",
            "SALT", "SAPPHIRE", "SILVER", "SLEEPY", "SOLAR", "STARLIT", "STORM", "SUNKEN", "THISTLE", "THUNDER", "TIDAL",
            "VELVET", "VIOLET", "WANDERING", "WILLOW", "WINTER",
    };

    private static final String[] LAST_WORDS = {
            "ABBEY", "ARCHES", "BASIN", "BAY", "BEACH", "BLUFFS", "BRIDGE", "CANYON", "CAVERNS", "CLIFFS", "COAST", "COVE",
            "CRATER", "CREEK", "CROSSING", "DALE", "DELTA", "DUNES", "FALLS", "FEN", "FIELDS", "FJORD", "FOREST", "GARDEN",
            "GATE", "GLADE", "GLEN", "GROVE", "HARBOR", "HEATH", "HEIGHTS", "HILLS", "HOLLOW", "ISLES", "LAGOON", "LAKE",
            "MARSH", "MEADOW", "MESA", "MOOR", "PASS", "PEAKS", "PLAINS", "POINT", "REEF", "RIDGE", "RIVER", "SHORE",
            "SPRINGS", "STEPPE", "SUMMIT", "SWAMP", "TERRACE", "THICKET", "TIDES", "TUNDRA", "VALE", "VALLEY", "WATERS",
            "WETLANDS", "WILDS", "WOODS",
    };

    private static final String[] BOSS_RANKS = {
            "Baron", "Duchess", "Warden", "Lady", "Lord", "Queen", "Keeper", "Marquis", "Sage", "Knight", "Countess", "Prince",
    };

    /** Every single attack, then every pair, so plenty of different bosses can be built from them. */
    private static final Boss.Attack[] ATTACKS = Boss.Attack.values();
    private static final int FASTEST_PACE = 2, SLOWEST_PACE = 4;
    private static final long SALT = 0x5245_4749_4F4E_5331L;
    private static final long BOSS_SALT = 0x5245_4749_4F4E_4253L;
    private static final double GOLDEN_ANGLE = 137.50776405;
    /** Shares no factor with the number of word pairs, so stepping by it visits every pair before any comes back. */
    private static final int NAME_STRIDE = 1_303;
    private static final String[] ROMAN_TENS = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
    private static final String[] ROMAN_ONES = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

    public final int chapter;
    public final String name;
    public final int skyTop, skyBottom, hillFar, hillNear, moon, particle;
    public final Weather weather;
    /** Where the moon sits as a share of the screen, and its radius as a share of the width. */
    public final float moonX, moonY, moonSize;
    /** How tight the hills are, and how far they sit up or down the screen. */
    public final float hillWave, hillShift;
    private final double hue;

    private Region(int chapter, String name, int skyTop, int skyBottom, int hillFar, int hillNear, int moon, int particle, Weather weather) {
        this(chapter, name, skyTop, skyBottom, hillFar, hillNear, moon, particle, weather, 0.76f, 0.13f, 1f / 11f, 1f, 0f, 0);
    }

    private Region(int chapter, String name, int skyTop, int skyBottom, int hillFar, int hillNear, int moon, int particle,
                   Weather weather, float moonX, float moonY, float moonSize, float hillWave, float hillShift, double hue) {
        this.chapter = chapter;
        this.name = name;
        this.skyTop = skyTop;
        this.skyBottom = skyBottom;
        this.hillFar = hillFar;
        this.hillNear = hillNear;
        this.moon = moon;
        this.particle = particle;
        this.weather = weather;
        this.moonX = moonX;
        this.moonY = moonY;
        this.moonSize = moonSize;
        this.hillWave = hillWave;
        this.hillShift = hillShift;
        this.hue = hue;
    }

    /** The region of a zero-based chapter. The same on every device. */
    public static Region forChapter(int chapter) {
        if (chapter < HAND_MADE.length) return HAND_MADE[chapter];
        Random r = new Random(SeedUtil.mix(SALT, chapter));
        double hue = chapter * GOLDEN_ANGLE % 360.0; // always far from its neighbours, and never lands in the same place twice
        float saturation = 0.4f + r.nextFloat() * 0.4f;
        double land = hue + 120 + r.nextFloat() * 120;
        int skyTop = hsv(hue, saturation, 0.14f + r.nextFloat() * 0.1f);
        int skyBottom = hsv(hue + 15 + r.nextFloat() * 45, saturation, 0.4f + r.nextFloat() * 0.3f);
        int hillFar = hsv(land, 0.35f + r.nextFloat() * 0.3f, 0.28f + r.nextFloat() * 0.1f);
        int hillNear = hsv(land, 0.45f + r.nextFloat() * 0.3f, 0.12f + r.nextFloat() * 0.08f);
        int moon = hsv(hue + 180, 0.1f + r.nextFloat() * 0.25f, 0.95f);
        int particle = hsv(hue + 90 + r.nextFloat() * 180, 0.2f + r.nextFloat() * 0.4f, 1f);
        Weather[] kinds = Weather.values();
        return new Region(chapter, name(chapter), skyTop, skyBottom, hillFar, hillNear, moon, particle, kinds[r.nextInt(kinds.length)],
                0.18f + r.nextFloat() * 0.64f, 0.08f + r.nextFloat() * 0.12f, 0.06f + r.nextFloat() * 0.06f,
                0.65f + r.nextFloat() * 0.8f, (r.nextFloat() - 0.5f) * 0.08f, hue);
    }

    /** Works through every pair of words in a scattered order. If they ever all get used, the next round is numbered. */
    private static String name(int chapter) {
        int pairs = FIRST_WORDS.length * LAST_WORDS.length;
        int place = chapter - HAND_MADE.length;
        int round = place / pairs + 1;
        int pair = (int) ((long) (place % pairs) * NAME_STRIDE % pairs);
        String name = FIRST_WORDS[pair % FIRST_WORDS.length] + " " + LAST_WORDS[pair / FIRST_WORDS.length];
        return round == 1 ? name : name + " " + roman(round);
    }

    /** Numbers past 99 stay as digits. */
    private static String roman(int n) {
        return n < 100 ? ROMAN_TENS[n / 10] + ROMAN_ONES[n % 10] : String.valueOf(n);
    }

    public boolean hasHandMadeBoss() {
        return chapter < HAND_MADE.length;
    }

    /** The hand-made boss's icon. Only for regions where {@link #hasHandMadeBoss()}. */
    public String handMadeBossIcon() {
        return Boss.forChapter(chapter).icon;
    }

    /** A generated boss's face, as {@link #BOSS_ART_SIZE} rows of pixels. */
    public int[] bossArt() {
        return BossArt.paint(chapter, hue);
    }

    public static final int BOSS_ART_SIZE = BossArt.SIZE;

    /** What this region's boss does, in the order it does it: one attack, or two taken in turn. */
    public Boss.Attack[] bossAttacks() {
        if (hasHandMadeBoss()) return new Boss.Attack[]{Boss.forChapter(chapter).attack};
        Random r = new Random(SeedUtil.mix(BOSS_SALT, chapter));
        Boss.Attack first = ATTACKS[r.nextInt(ATTACKS.length)];
        if (r.nextBoolean()) return new Boss.Attack[]{first};
        Boss.Attack second = ATTACKS[(first.ordinal() + 1 + r.nextInt(ATTACKS.length - 1)) % ATTACKS.length];
        return new Boss.Attack[]{first, second};
    }

    /** How many words this region's boss lets pass between attacks. */
    public int bossPace() {
        if (hasHandMadeBoss()) return BossFight.USUAL_PACE;
        Random r = new Random(SeedUtil.mix(BOSS_SALT + 1, chapter));
        return FASTEST_PACE + r.nextInt(SLOWEST_PACE - FASTEST_PACE + 1);
    }

    /** What this region's boss does, for showing before the fight. */
    public String bossDetail() {
        StringBuilder out = new StringBuilder();
        for (Boss.Attack a : bossAttacks()) out.append(out.length() == 0 ? "" : " ").append(a.detail);
        return out.toString();
    }

    /** The name of the boss guarding this region. */
    public String bossName() {
        if (hasHandMadeBoss()) return Boss.forChapter(chapter).title;
        return BOSS_RANKS[chapter % BOSS_RANKS.length] + " of " + titleCase(name);
    }

    private static String titleCase(String words) {
        StringBuilder out = new StringBuilder();
        for (String word : words.split(" ")) {
            if (out.length() > 0) out.append(' ');
            boolean numeral = word.matches("[IVXLC]+|\\d+");
            out.append(numeral ? word : word.charAt(0) + word.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return out.toString();
    }

    static int hsv(double hue, float saturation, float value) {
        double h = ((hue % 360) + 360) % 360 / 60.0;
        double chroma = value * saturation;
        double x = chroma * (1 - Math.abs(h % 2 - 1));
        double[][] sides = {{chroma, x, 0}, {x, chroma, 0}, {0, chroma, x}, {0, x, chroma}, {x, 0, chroma}, {chroma, 0, x}};
        double[] rgb = sides[(int) h % 6];
        double m = value - chroma;
        return 0xFF000000 | channel(rgb[0] + m) << 16 | channel(rgb[1] + m) << 8 | channel(rgb[2] + m);
    }

    private static int channel(double v) {
        return (int) Math.round(Math.min(1.0, v) * 255);
    }
}
