package com.kun.pixelwords.data;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Local-calendar day number (days since 1970-01-01 in the device's current time zone). */
public final class Days {

    public static final long MILLIS_PER_HOUR = 60 * 60 * 1000L;

    private Days() {}

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

    /** A day number's date, read in UTC because the day number has already allowed for the time zone. */
    private static Calendar calendar(long day) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        c.setTimeInMillis(day * DAY_MS);
        return c;
    }

    /** The date as just its day of the month, like 12. */
    public static String dayOfMonth(long day) {
        return String.valueOf(calendar(day).get(Calendar.DAY_OF_MONTH));
    }

    /** The date in short, like NOV 12. */
    public static String monthAndDay(long day) {
        Calendar c = calendar(day);
        return MONTHS[c.get(Calendar.MONTH)] + " " + c.get(Calendar.DAY_OF_MONTH);
    }

    public static long today() {
        long now = System.currentTimeMillis();
        return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), DAY_MS);
    }
}
