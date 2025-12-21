package com.example.lockinapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainStudyActivity extends AppCompatActivity {

    private SeekBar seekBarTime;
    private TextView tvSelectedTime;
    private Button btnToggleTimer;

    private String currentUserId;
    private boolean isTimerRunning = false;
    private int selectedMinutes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_study);

        seekBarTime = findViewById(R.id.seekBarTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        btnToggleTimer = findViewById(R.id.btnToggleTimer);

        currentUserId = getIntent().getStringExtra("USER_ID");

        // setup seekbar listener
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // updates the text as the user slides the bar
                selectedMinutes = progress;
                tvSelectedTime.setText("Time: " + selectedMinutes + " minutes");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }


    private void startTimer() {
        if (selectedMinutes > 0)
        {
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

        // restart timer
        isTimerRunning = false;
        btnToggleTimer.setText("Start Timer");
        seekBarTime.setEnabled(true); // unlock seekbar
    }

    public void on_click_log_out(View view) {
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