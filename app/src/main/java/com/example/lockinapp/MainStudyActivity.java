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
    private TextView tvSelectedTime, tvUserName;
    private Button btnToggleTimer;

    private String currentUserId;
    private boolean isTimerRunning = false;
    private int selectedMinutes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_study);

    }

    private void startTimer() {
    }
    private void stopTimer() {
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