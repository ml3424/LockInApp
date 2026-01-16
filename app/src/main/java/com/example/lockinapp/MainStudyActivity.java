package com.example.lockinapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainStudyActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private SeekBar seekBarTime;
    private TextView tvSelectedTime;
    private Button btnToggleTimer, btnLogOut;
    private Spinner spinnerSubjects;

    private String currentUserId;
    private boolean isTimerRunning = false;
    private int selectedMinutes = 0;
    private String selectedSubject = "";

    // receiver to catch updates from the service and show countdown on screen
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("TIMER_UPDATE")) {
                String timeLeft = intent.getStringExtra("TIME_LEFT");
                tvSelectedTime.setText(timeLeft); // update the minutes on screen
            }
            else if (intent.getAction().equals("TIMER_FINISHED")) {
                handleStudySessionEnd(); // custom end logic
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_study);

        seekBarTime = findViewById(R.id.seekBarTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnToggleTimer = findViewById(R.id.btnToggleTimer);
        btnLogOut = findViewById(R.id.btnLogOut);
        spinnerSubjects = findViewById(R.id.spinnerSubjects);

        currentUserId = getIntent().getStringExtra("USER_ID");

        String[] subjects = {"Math", "English", "History", "Computer Science", "Physics"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerSubjects.setAdapter(adapter);
        spinnerSubjects.setOnItemSelectedListener(this);


        // setup seekbar listener
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // updates the text as the user slides the bar
                selectedMinutes = progress;
                tvSelectedTime.setText("" + selectedMinutes + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedSubject = parent.getItemAtPosition(position).toString();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void startTimer() {
        if (selectedMinutes > 0)
        {
            btnLogOut.setEnabled(false);

            // start timer service
            Intent serviceIntent = new Intent(this, TimerService.class);
            serviceIntent.putExtra("DURATION_MIN", selectedMinutes);
            startService(serviceIntent);

            // able stoping the timer
            isTimerRunning = true;
            btnToggleTimer.setText("Stop Timer");
            seekBarTime.setEnabled(false); // lock seekbar
        }
    }

    private void stopTimer() {
        // stop timer service
        Intent serviceIntent = new Intent(this, TimerService.class);
        stopService(serviceIntent);

        resetUITimer();
    }

    private void handleStudySessionEnd()
    {
        isTimerRunning = false;
        btnToggleTimer.setVisibility(View.GONE); // remove STOP TIMER button as requested
        tvSelectedTime.setText("Done!");
        resetUITimer();

        // save to Firebase
        saveSessionToFirebase();
    }

    private void saveSessionToFirebase()
    {
        DatabaseReference sessionsRef = FirebaseDatabase.getInstance().getReference("StudySessions");

        // generate a unique ID for this session
        String sessionId = sessionsRef.push().getKey();

        // calculate data
        long durationSeconds = selectedMinutes * 60;
        int points = (int) (selectedMinutes * 10); // 10 points per minute
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());

        // create the session object
        StudySession session = new StudySession(
                sessionId,
                currentUserId,
                selectedSubject,
                currentTime,
                durationSeconds,
                85, // aiConcentrationScore - for now
                points
        );

        if (sessionId != null) {
            sessionsRef.child(sessionId).setValue(session)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Session saved! You earned " + points + " points", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("firebase", "failed to save session", e);
                    });
        }
    }

    private void resetUITimer() {
        isTimerRunning = false;
        btnToggleTimer.setText("START TIMER");
        btnToggleTimer.setVisibility(View.VISIBLE);
        seekBarTime.setEnabled(true); // unlock seekbar
        tvSelectedTime.setText("0");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMER_UPDATE");
        filter.addAction("TIMER_FINISHED");
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, filter);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerReceiver);
        super.onPause();
    }

    public void on_click_log_out(View view) {
        // update shared preferences to stop auto-login
        SharedPreferences sP = getSharedPreferences("stay_logged_in", MODE_PRIVATE);
        SharedPreferences.Editor editor = sP.edit();
        editor.putBoolean("stayConnected", false);
        editor.apply();

        // sign out from firebase auth
        FirebaseAuth.getInstance().signOut();
        
        // back to sign activity
        Intent intent = new Intent(MainStudyActivity.this, SignActivity.class);
        startActivity(intent);
        finish();
    }

    public void on_click_toggle_timer(View view) {
        if (!isTimerRunning)
        {
            startTimer();
        }
        else
        {
            stopTimer();
        }
    }
}