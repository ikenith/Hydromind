package com.example.hydromind;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView tvStreak, tvProgress, tvMotivation;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private Spinner spGoal, spSize, spInterval;
    private Button btnWakeTime, btnSleepTime;

    private int currentCount = 0;
    private int dailyGoal = 8;
    private int streak = 0;

    private final String PREFS_NAME = "HydroMindPrefs";
    private final String KEY_COUNT = "currentCount";
    private final String KEY_GOAL = "dailyGoal";
    private final String KEY_STREAK = "streak";
    private final String KEY_DATE = "lastDate";
    private final String KEY_INTERVAL = "intervalHours";
    private final String KEY_WAKE_HOUR = "wakeHour";
    private final String KEY_WAKE_MIN = "wakeMin";
    private final String KEY_SLEEP_HOUR = "sleepHour";
    private final String KEY_SLEEP_MIN = "sleepMin";
    private final String KEY_GLASS_SIZE = "glassSize";

    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) scheduleReminders();
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        createNotificationChannel();

        tvStreak = findViewById(R.id.tvStreak);
        tvProgress = findViewById(R.id.tvProgress);
        tvMotivation = findViewById(R.id.tvMotivation);
        progressBar = findViewById(R.id.progressBar);
        fabAdd = findViewById(R.id.fabAdd);
        spGoal = findViewById(R.id.spGoal);
        spSize = findViewById(R.id.spSize);
        spInterval = findViewById(R.id.spInterval);
        btnWakeTime = findViewById(R.id.btnWakeTime);
        btnSleepTime = findViewById(R.id.btnSleepTime);

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"6", "8", "10"});
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGoal.setAdapter(goalAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"250ml", "350ml", "500ml"});
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSize.setAdapter(sizeAdapter);

        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Every 1 hour", "Every 2 hours", "Every 3 hours"});
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInterval.setAdapter(intervalAdapter);

        loadData();
        checkNewDay();
        updateUI();

        fabAdd.setOnClickListener(v -> {
            currentCount++;
            saveData();
            updateUI();
        });

        setupListeners();
        requestNotifPermission();
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                scheduleReminders();
            }
        } else {
            scheduleReminders();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("water_reminders", "Water Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void setupListeners() {
        spGoal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (v instanceof TextView) ((TextView) v).setTextColor(getResources().getColor(R.color.text_primary));
                dailyGoal = Integer.parseInt(p.getItemAtPosition(pos).toString());
                prefs.edit().putInt(KEY_GOAL, dailyGoal).apply();
                updateUI();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (v instanceof TextView) ((TextView) v).setTextColor(getResources().getColor(R.color.text_primary));
                prefs.edit().putInt(KEY_GLASS_SIZE, pos).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (v instanceof TextView) ((TextView) v).setTextColor(getResources().getColor(R.color.text_primary));
                prefs.edit().putInt(KEY_INTERVAL, pos + 1).apply();
                scheduleReminders();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnWakeTime.setOnClickListener(v -> showTimePicker(true));
        btnSleepTime.setOnClickListener(v -> showTimePicker(false));
    }

    private void showTimePicker(boolean isWake) {
        int h = prefs.getInt(isWake ? KEY_WAKE_HOUR : KEY_SLEEP_HOUR, isWake ? 8 : 22);
        int m = prefs.getInt(isWake ? KEY_WAKE_MIN : KEY_SLEEP_MIN, 0);

        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            prefs.edit().putInt(isWake ? KEY_WAKE_HOUR : KEY_SLEEP_HOUR, hourOfDay)
                    .putInt(isWake ? KEY_WAKE_MIN : KEY_SLEEP_MIN, minute).apply();
            updateTimeButtons();
            scheduleReminders();
        }, h, m, true).show();
    }

    private void loadData() {
        currentCount = prefs.getInt(KEY_COUNT, 0);
        dailyGoal = prefs.getInt(KEY_GOAL, 8);
        streak = prefs.getInt(KEY_STREAK, 0);

        int goalIdx = (dailyGoal == 6) ? 0 : (dailyGoal == 10) ? 2 : 1;
        spGoal.setSelection(goalIdx, false);
        spSize.setSelection(prefs.getInt(KEY_GLASS_SIZE, 0), false);
        spInterval.setSelection(prefs.getInt(KEY_INTERVAL, 2) - 1, false);

        updateTimeButtons();
    }

    private void updateTimeButtons() {
        btnWakeTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                prefs.getInt(KEY_WAKE_HOUR, 8), prefs.getInt(KEY_WAKE_MIN, 0)));
        btnSleepTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                prefs.getInt(KEY_SLEEP_HOUR, 22), prefs.getInt(KEY_SLEEP_MIN, 0)));
    }

    private void saveData() {
        prefs.edit().putInt(KEY_COUNT, currentCount).apply();
    }

    private void checkNewDay() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString(KEY_DATE, "");

        if (!today.equals(lastDate)) {
            if (!lastDate.isEmpty()) {
                if (currentCount >= dailyGoal) streak++;
                else streak = 0;
            }
            currentCount = 0;
            saveData();
            prefs.edit().putInt(KEY_STREAK, streak).putString(KEY_DATE, today).apply();
        }
    }

    private void updateUI() {
        tvStreak.setText("\ud83d\udd25 Streak: " + streak + " days");
        tvProgress.setText(currentCount + " / " + dailyGoal);
        progressBar.setMax(dailyGoal * 10);
        progressBar.setProgress(currentCount * 10);

        double pct = (double) currentCount / dailyGoal;
        if (pct >= 1.0) tvMotivation.setText("Goal reached! Awesome! \ud83d\udca7");
        else if (pct >= 0.75) tvMotivation.setText("Almost there! Keep going \ud83d\udca7");
        else if (pct >= 0.5) tvMotivation.setText("Halfway! Drink up \ud83d\udca7");
        else if (pct >= 0.25) tvMotivation.setText("Good start! \ud83d\udca7");
        else tvMotivation.setText("Let's get hydrated! \ud83d\udca7");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNewDay();
        loadData();
        updateUI();
    }

    private void scheduleReminders() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.setAction("com.example.hydromind.REMINDER");
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            return;
        }

        int intervalH = prefs.getInt(KEY_INTERVAL, 2);
        long intervalMs = intervalH * 3600000L;
        
        Calendar now = Calendar.getInstance();
        if (am != null) {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, now.getTimeInMillis() + intervalMs, intervalMs, pi);
        }
    }
}
