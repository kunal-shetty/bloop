package com.kun.pixelwords.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives the daily alarm and the reboot broadcast. PixelWordsApp has already set up Settings by now. */
public class ReminderReceiver extends BroadcastReceiver {

    static final String ACTION_REMIND = "com.kun.pixelwords.action.REMIND";
    static final String ACTION_TRIP = "com.kun.pixelwords.action.TRIP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TRIP.equals(intent.getAction())) {
            Reminders.postTripHome(context);
            return;
        }
        if (ACTION_REMIND.equals(intent.getAction())) Reminders.post(context);
        Reminders.schedule(context);
    }
}
