package com.kun.pixelwords.generator;

/** Small deterministic hash helpers so every device derives identical levels from the same inputs. */
public final class SeedUtil {

    private SeedUtil() {}

    /** SplitMix64-style mixer combining two longs into one well-distributed seed. */
    public static long mix(long a, long b) {
        long z = a ^ (b + 0x9E3779B97F4A7C15L + (a << 6) + (a >>> 2));
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
