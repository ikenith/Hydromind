package com.example.hydromind;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String ACTION_LOG = "com.example.hydromind.ACTION_LOG";
    private static final String ACTION_SNOOZE = "com.example.hydromind.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_LOG.equals(action)) {
            // Log a glass directly from notification
            SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
            int count = prefs.getInt(MainActivity.KEY_COUNT, 0) + 1;
            prefs.edit().putInt(MainActivity.KEY_COUNT, count).apply();

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(42);

            // Reschedule next reminder
            scheduleNext(context);
            return;
        }

        if (ACTION_SNOOZE.equals(action)) {
            // Snooze 30 minutes
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent ri = new Intent(context, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, ri,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 30 * 60 * 1000, pi);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(42);
            return;
        }

        // Default: show the reminder notification
        showNotification(context);
        scheduleNext(context);
    }

    private void showNotification(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        int count = prefs.getInt(MainActivity.KEY_COUNT, 0);
        int goal = prefs.getInt(MainActivity.KEY_GOAL, 8);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent logIntent = new Intent(context, ReminderReceiver.class);
        logIntent.setAction(ACTION_LOG);
        PendingIntent logPi = PendingIntent.getBroadcast(context, 1, logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String body = "You've had " + count + " of " + goal + " glasses today 💧";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to drink water! 💧")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .addAction(0, "Logged a glass 💧", logPi)
            .addAction(0, "Snooze 30 min", snoozePi);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(42, builder.build());
    }

    private void scheduleNext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        int interval = prefs.getInt(MainActivity.KEY_INTERVAL, 2);
        int wakeHour = prefs.getInt(MainActivity.KEY_WAKE, 8);
        int sleepHour = prefs.getInt(MainActivity.KEY_SLEEP, 22);

        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.HOUR_OF_DAY, interval);

        int nextHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (nextHour >= wakeHour && nextHour < sleepHour) {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
}
