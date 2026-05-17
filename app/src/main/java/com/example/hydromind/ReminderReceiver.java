package com.example.hydromind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private final String PREFS_NAME = "HydroMindPrefs";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if ("com.example.hydromind.LOG_WATER".equals(action)) {
            int currentCount = prefs.getInt("currentCount", 0);
            prefs.edit().putInt("currentCount", currentCount + 1).apply();
            NotificationManagerCompat.from(context).cancel(1001);
            return;
        }

        if ("com.example.hydromind.SNOOZE".equals(action)) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
            snoozeIntent.setAction("com.example.hydromind.REMINDER");
            PendingIntent pi = PendingIntent.getBroadcast(context, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
            long snoozeTime = System.currentTimeMillis() + (30 * 60 * 1000);
            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
            }
            NotificationManagerCompat.from(context).cancel(1001);
            return;
        }

        if (!isWithinTime(prefs)) return;

        Intent logIntent = new Intent(context, ReminderReceiver.class);
        logIntent.setAction("com.example.hydromind.LOG_WATER");
        PendingIntent piLog = PendingIntent.getBroadcast(context, 2, logIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction("com.example.hydromind.SNOOZE");
        PendingIntent piSnooze = PendingIntent.getBroadcast(context, 3, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "water_reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Time to drink water! \ud83d\udca7")
                .setContentText("Keep up with your daily goal.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_input_add, "Logged a glass \ud83d\udca7", piLog)
                .addAction(android.R.drawable.ic_popup_sync, "Snooze 30 min", piSnooze);

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(1001, builder.build());
        }
    }

    private boolean isWithinTime(SharedPreferences prefs) {
        Calendar cal = Calendar.getInstance();
        int curH = cal.get(Calendar.HOUR_OF_DAY);
        int curM = cal.get(Calendar.MINUTE);
        int curTotal = curH * 60 + curM;

        int wakeTotal = prefs.getInt("wakeHour", 8) * 60 + prefs.getInt("wakeMin", 0);
        int sleepTotal = prefs.getInt("sleepHour", 22) * 60 + prefs.getInt("sleepMin", 0);

        if (sleepTotal < wakeTotal) {
            return curTotal >= wakeTotal || curTotal <= sleepTotal;
        } else {
            return curTotal >= wakeTotal && curTotal <= sleepTotal;
        }
    }
}
