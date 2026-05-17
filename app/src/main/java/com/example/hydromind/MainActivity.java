package com.example.hydromind;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "hydromind_prefs";
    public static final String KEY_COUNT = "glass_count";
    public static final String KEY_GOAL = "daily_goal";
    public static final String KEY_ML = "glass_ml";
    public static final String KEY_INTERVAL = "reminder_interval";
    public static final String KEY_WAKE = "wake_hour";
    public static final String KEY_SLEEP = "sleep_hour";
    public static final String KEY_STREAK = "streak";
    public static final String KEY_LAST_DATE = "last_date";
    public static final String CHANNEL_ID = "hydromind_channel";

    private SharedPreferences prefs;
    private int glassCount, dailyGoal, glassMl, reminderInterval, wakeHour, sleepHour, streak;

    private TextView tvCount, tvGoal, tvMotivation, tvStreak, tvMl, tvTotalMl;
    private ProgressBar progressBar;
    private Button btnAdd, btnGoal6, btnGoal8, btnGoal10;
    private Button btnMl250, btnMl350, btnMl500;
    private Button btnInterval1, btnInterval2, btnInterval3;
    private Button btnWakeEarly, btnWakeNormal, btnWakeLate;
    private Button btnSleepEarly, btnSleepNormal, btnSleepLate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        createNotificationChannel();
        checkDayReset();
        loadPrefs();
        initViews();
        updateUI();
        requestNotificationPermission();
    }

    private void loadPrefs() {
        glassCount = prefs.getInt(KEY_COUNT, 0);
        dailyGoal = prefs.getInt(KEY_GOAL, 8);
        glassMl = prefs.getInt(KEY_ML, 250);
        reminderInterval = prefs.getInt(KEY_INTERVAL, 2);
        wakeHour = prefs.getInt(KEY_WAKE, 8);
        sleepHour = prefs.getInt(KEY_SLEEP, 22);
        streak = prefs.getInt(KEY_STREAK, 0);
    }

    private void initViews() {
        tvCount = findViewById(R.id.tv_count);
        tvGoal = findViewById(R.id.tv_goal);
        tvMotivation = findViewById(R.id.tv_motivation);
        tvStreak = findViewById(R.id.tv_streak);
        tvMl = findViewById(R.id.tv_ml);
        tvTotalMl = findViewById(R.id.tv_total_ml);
        progressBar = findViewById(R.id.progress_bar);

        btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> addGlass());

        // Goal buttons
        btnGoal6 = findViewById(R.id.btn_goal_6);
        btnGoal8 = findViewById(R.id.btn_goal_8);
        btnGoal10 = findViewById(R.id.btn_goal_10);
        btnGoal6.setOnClickListener(v -> setGoal(6));
        btnGoal8.setOnClickListener(v -> setGoal(8));
        btnGoal10.setOnClickListener(v -> setGoal(10));

        // ML buttons
        btnMl250 = findViewById(R.id.btn_ml_250);
        btnMl350 = findViewById(R.id.btn_ml_350);
        btnMl500 = findViewById(R.id.btn_ml_500);
        btnMl250.setOnClickListener(v -> setMl(250));
        btnMl350.setOnClickListener(v -> setMl(350));
        btnMl500.setOnClickListener(v -> setMl(500));

        // Interval buttons
        btnInterval1 = findViewById(R.id.btn_interval_1);
        btnInterval2 = findViewById(R.id.btn_interval_2);
        btnInterval3 = findViewById(R.id.btn_interval_3);
        btnInterval1.setOnClickListener(v -> setInterval(1));
        btnInterval2.setOnClickListener(v -> setInterval(2));
        btnInterval3.setOnClickListener(v -> setInterval(3));

        // Wake time buttons
        btnWakeEarly = findViewById(R.id.btn_wake_early);
        btnWakeNormal = findViewById(R.id.btn_wake_normal);
        btnWakeLate = findViewById(R.id.btn_wake_late);
        btnWakeEarly.setOnClickListener(v -> setWake(6));
        btnWakeNormal.setOnClickListener(v -> setWake(8));
        btnWakeLate.setOnClickListener(v -> setWake(10));

        // Sleep time buttons
        btnSleepEarly = findViewById(R.id.btn_sleep_early);
        btnSleepNormal = findViewById(R.id.btn_sleep_normal);
        btnSleepLate = findViewById(R.id.btn_sleep_late);
        btnSleepEarly.setOnClickListener(v -> setSleep(20));
        btnSleepNormal.setOnClickListener(v -> setSleep(22));
        btnSleepLate.setOnClickListener(v -> setSleep(23));
    }

    public void addGlass() {
        glassCount++;
        prefs.edit().putInt(KEY_COUNT, glassCount).apply();
        updateUI();
        if (glassCount == dailyGoal) {
            Toast.makeText(this, "🎉 Goal reached! Amazing!", Toast.LENGTH_LONG).show();
        }
    }

    private void setGoal(int goal) {
        dailyGoal = goal;
        prefs.edit().putInt(KEY_GOAL, goal).apply();
        updateUI();
    }

    private void setMl(int ml) {
        glassMl = ml;
        prefs.edit().putInt(KEY_ML, ml).apply();
        updateUI();
    }

    private void setInterval(int hours) {
        reminderInterval = hours;
        prefs.edit().putInt(KEY_INTERVAL, hours).apply();
        scheduleReminder();
        Toast.makeText(this, "Reminder set every " + hours + "h 💧", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void setWake(int hour) {
        wakeHour = hour;
        prefs.edit().putInt(KEY_WAKE, hour).apply();
        scheduleReminder();
        updateUI();
    }

    private void setSleep(int hour) {
        sleepHour = hour;
        prefs.edit().putInt(KEY_SLEEP, hour).apply();
        scheduleReminder();
        updateUI();
    }

    private void updateUI() {
        tvCount.setText(String.valueOf(glassCount));
        tvGoal.setText("/ " + dailyGoal + " glasses");
        tvMl.setText(glassMl + " ml per glass");
        tvTotalMl.setText((glassCount * glassMl) + " ml today");
        tvStreak.setText("🔥 " + streak + " day streak");

        int progress = (int) ((glassCount / (float) dailyGoal) * 100);
        progressBar.setProgress(Math.min(progress, 100));

        // Motivational messages
        if (glassCount == 0) {
            tvMotivation.setText("Start your day right! 💧");
        } else if (progress < 25) {
            tvMotivation.setText("Good start, keep going!");
        } else if (progress < 50) {
            tvMotivation.setText("You're on a roll 💪");
        } else if (progress < 75) {
            tvMotivation.setText("Halfway there, amazing!");
        } else if (progress < 100) {
            tvMotivation.setText("Almost done, push through! 🚀");
        } else {
            tvMotivation.setText("Goal crushed! You're a legend 🏆");
        }

        // Highlight active goal button
        btnGoal6.setAlpha(dailyGoal == 6 ? 1f : 0.4f);
        btnGoal8.setAlpha(dailyGoal == 8 ? 1f : 0.4f);
        btnGoal10.setAlpha(dailyGoal == 10 ? 1f : 0.4f);

        // Highlight active ml button
        btnMl250.setAlpha(glassMl == 250 ? 1f : 0.4f);
        btnMl350.setAlpha(glassMl == 350 ? 1f : 0.4f);
        btnMl500.setAlpha(glassMl == 500 ? 1f : 0.4f);

        // Highlight active interval button
        btnInterval1.setAlpha(reminderInterval == 1 ? 1f : 0.4f);
        btnInterval2.setAlpha(reminderInterval == 2 ? 1f : 0.4f);
        btnInterval3.setAlpha(reminderInterval == 3 ? 1f : 0.4f);

        // Highlight wake buttons
        btnWakeEarly.setAlpha(wakeHour == 6 ? 1f : 0.4f);
        btnWakeNormal.setAlpha(wakeHour == 8 ? 1f : 0.4f);
        btnWakeLate.setAlpha(wakeHour == 10 ? 1f : 0.4f);

        // Highlight sleep buttons
        btnSleepEarly.setAlpha(sleepHour == 20 ? 1f : 0.4f);
        btnSleepNormal.setAlpha(sleepHour == 22 ? 1f : 0.4f);
        btnSleepLate.setAlpha(sleepHour == 23 ? 1f : 0.4f);
    }

    private void checkDayReset() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_DATE, "");

        if (!today.equals(lastDate)) {
            int prevCount = prefs.getInt(KEY_COUNT, 0);
            int prevGoal = prefs.getInt(KEY_GOAL, 8);
            int currentStreak = prefs.getInt(KEY_STREAK, 0);

            if (!lastDate.isEmpty()) {
                if (prevCount >= prevGoal) {
                    currentStreak++;
                } else {
                    currentStreak = 0;
                }
            }

            prefs.edit()
                .putInt(KEY_COUNT, 0)
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_STREAK, currentStreak)
                .apply();
        }
    }

    public void scheduleReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);

        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= wakeHour && currentHour < sleepHour) {
            cal.add(Calendar.HOUR_OF_DAY, reminderInterval);
            if (cal.get(Calendar.HOUR_OF_DAY) < sleepHour) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, wakeHour);
            cal.set(Calendar.MINUTE, 0);
            if (cal.getTimeInMillis() < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Water Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reminders to drink water");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        scheduleReminder();
    }
}
