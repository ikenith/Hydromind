package com.example.hydromind;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        int interval = prefs.getInt(MainActivity.KEY_INTERVAL, 2);
        int wakeHour = prefs.getInt(MainActivity.KEY_WAKE, 8);
        int sleepHour = prefs.getInt(MainActivity.KEY_SLEEP, 22);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent ri = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, ri,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= wakeHour && currentHour < sleepHour) {
            cal.add(Calendar.HOUR_OF_DAY, interval);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, wakeHour);
            cal.set(Calendar.MINUTE, 0);
            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }
}
