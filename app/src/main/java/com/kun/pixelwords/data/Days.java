package com.kun.pixelwords.data;

import java.util.TimeZone;

/** Local-calendar day number (days since 1970-01-01 in the device's current time zone). */
public final class Days {

    private Days() {}

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    public static long today() {
        long now = System.currentTimeMillis();
        return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), DAY_MS);
    }
}
