package com.example.hydromind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || 
            "android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {

            SharedPreferences prefs = context.getSharedPreferences("HydroMindPrefs", Context.MODE_PRIVATE);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent reminderIntent = new Intent(context, ReminderReceiver.class);
            reminderIntent.setAction("com.example.hydromind.REMINDER");
            
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            int intervalH = prefs.getInt("intervalHours", 2);
            long intervalMs = intervalH * 3600000L;

            Calendar now = Calendar.getInstance();
            if (am != null) {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, now.getTimeInMillis() + intervalMs, intervalMs, pi);
            }
        }
    }
}
