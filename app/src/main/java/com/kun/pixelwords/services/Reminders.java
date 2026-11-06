package com.kun.pixelwords.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.kun.pixelwords.R;
import com.kun.pixelwords.SplashActivity;
import com.kun.pixelwords.data.Days;
import com.kun.pixelwords.data.ProgressStore;
import com.kun.pixelwords.data.Settings;
import com.kun.pixelwords.game.Progress;
import com.kun.pixelwords.game.StreakLogic;

import java.util.Calendar;

/**
 * The once-a-day nudge to play. Each alarm books the next one, and app start and reboot book it again
 * because the system forgets alarms on reboot. Inexact on purpose: Android may deliver it up to an
 * hour late, and exact alarms need a permission Play restricts.
 */
public final class Reminders {

    private static final String CHANNEL = "daily_reminder";
    private static final String TRIP_CHANNEL = "cat_trips";
    private static final int NOTIFICATION_ID = 1;
    private static final int TRIP_NOTIFICATION_ID = 2;
    private static final int TRIP_REQUEST = 1;

    private Reminders() {}

    /** Books the next reminder, or cancels it if the player has them switched off. */
    public static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarm = alarmIntent(context);
        if (!Settings.reminders()) {
            alarms.cancel(alarm);
            return;
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime(Settings.reminderHour()), alarm);
    }

    /** Posts the reminder, unless today's daily is already solved. */
    public static void post(Context context) {
        if (!Settings.reminders() || !mayNotify(context)) return;
        Progress progress = new ProgressStore(context).load();
        long today = Days.today();
        if (progress.streak.lastDay == today) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT));
            builder = new Notification.Builder(context, CHANNEL);
        } else {
            builder = legacyBuilder(context);
        }
        Intent open = new Intent(context, SplashActivity.class);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(message(context, StreakLogic.displayStreak(progress.streak, today)))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, 0, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    /** Books a nudge for when the cat gets home from a trip. */
    public static void scheduleTripHome(Context context, long atMillis) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, tripIntent(context));
    }

    public static void cancelTripHome(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(tripIntent(context));
    }

    private static PendingIntent tripIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ReminderReceiver.ACTION_TRIP);
        return PendingIntent.getBroadcast(context, TRIP_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Says the cat is home, unless the trip was already collected. */
    static void postTripHome(Context context) {
        if (!mayNotify(context) || new ProgressStore(context).load().expPlace == 0) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(TRIP_CHANNEL,
                    context.getString(R.string.trip_channel), NotificationManager.IMPORTANCE_DEFAULT));
            builder = new Notification.Builder(context, TRIP_CHANNEL);
        } else {
            builder = legacyBuilder(context);
        }
        Intent open = new Intent(context, SplashActivity.class);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.trip_title))
                .setContentText(context.getString(R.string.trip_text))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context, TRIP_REQUEST, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.notify(TRIP_NOTIFICATION_ID, builder.build());
    }

    /** False only on Android 13+ when the player has not allowed notifications. */
    public static boolean mayNotify(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Clock label for a 24-hour value: 19 becomes "7 PM". */
    public static String hourLabel(int hour) {
        int twelve = hour % 12 == 0 ? 12 : hour % 12;
        return twelve + (hour < 12 ? " AM" : " PM");
    }

    private static String message(Context context, int streak) {
        return streak > 0
                ? context.getString(R.string.reminder_streak, streak)
                : context.getString(R.string.reminder_fresh);
    }

    private static long nextTime(int hour) {
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ReminderReceiver.ACTION_REMIND);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder legacyBuilder(Context context) {
        return new Notification.Builder(context);
    }
}
