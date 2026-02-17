package com.example.lockinapp;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StudyFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    private SeekBar seekBarTime;
    private TextView tvSelectedTime;
    private ImageView ivGoldenBadge;
    private Button btnToggleTimer, btnLogOut;
    private Spinner spinnerSubjects;
    private PreviewView cameraPreview; // XML view needed for camera
    private View mainLayout;

    private StudyCameraManager cameraManager;

    private String currentUserId;
    private boolean isTimerRunning = false;
    private int selectedMinutes = 0;
    private String selectedSubject = "";


    public StudyFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // first inflate the layout
        View view = inflater.inflate(R.layout.study_fragment, container, false);

        // find views within the inflated view object
        seekBarTime = view.findViewById(R.id.seekBarTime);
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        ivGoldenBadge = view.findViewById(R.id.ivGoldenBadge);
        btnToggleTimer = view.findViewById(R.id.btnToggleTimer);
        btnLogOut = view.findViewById(R.id.btnLogOut);
        spinnerSubjects = view.findViewById(R.id.spinnerSubjects);
        cameraPreview = view.findViewById(R.id.cameraPreview);
        mainLayout = view.findViewById(R.id.main);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        cameraManager = new StudyCameraManager(requireContext(), getViewLifecycleOwner(), cameraPreview);

        // Check permissions immediately
        checkCameraPermissions();

        setupSpinner();
        setupSeekBar();
        setupButtons();
        applyUserPreferences();

        // inflate the layout for this fragment
        return view;
    }

    private void setupSpinner() {
        String[] subjects = {"Math", "English", "History", "Computer Science", "Physics"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerSubjects.setAdapter(adapter);
        spinnerSubjects.setOnItemSelectedListener(this);
    }

    private void setupSeekBar() {
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedMinutes = progress+1;
                tvSelectedTime.setText("" + selectedMinutes + " min");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupButtons() {
        btnToggleTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on_click_toggle_timer(v);
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on_click_log_out();
            }
        });
    }

    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
        else {
            cameraManager.startCamera(); // Start camera only if allowed
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                cameraManager.startCamera();
                            } else {
                                Toast.makeText(requireContext(), "Camera needed for AI analysis", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

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
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedSubject = parent.getItemAtPosition(position).toString();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void startTimer() {
        // debug check
        // navigateToFeedback("0", 0, 0);

        if (selectedMinutes > 0)
        {
            btnLogOut.setEnabled(false);

            // start timer service
            Intent serviceIntent = new Intent(requireContext(), TimerService.class);
            serviceIntent.putExtra("DURATION_MIN", selectedMinutes);
            requireActivity().startService(serviceIntent);

            // able stoping the timer
            isTimerRunning = true;
            btnToggleTimer.setText("Stop Timer");
            seekBarTime.setEnabled(false); // lock seekbar

            cameraManager.startRandomCaptures();
        }
    }

    private void stopTimer() {
        // stop timer service
        Intent serviceIntent = new Intent(requireContext(), TimerService.class);
        requireContext().stopService(serviceIntent);

        cameraManager.stopRandomCaptures();
        resetUITimer();
    }

    private void handleStudySessionEnd()
    {
        isTimerRunning = false;
        btnToggleTimer.setVisibility(View.GONE); // remove STOP TIMER button as requested
        
        cameraManager.stopRandomCaptures();
        resetUITimer();

        // save to Firebase
        saveSessionToFirebase();
    }

    private int calcSessionScore()
    {
        int aiScore = cameraManager.getAverageScore();
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());

        // points algorithm
        // start with 10 points per minute
        int pointsEarned = selectedMinutes * 10;

        // ai concentration multiplier: 20% bonus for high focus (>80)
        if (aiScore > 80) {
            pointsEarned = (int) (pointsEarned * 1.2);
        }
        // penalty for very low focus (<50)
        else if (aiScore < 50 && aiScore > 0) {
            pointsEarned = (int) (pointsEarned * 0.9);
        }

        SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean hasBooster = sharedPref.getBoolean("has_point_booster", false);
        if(hasBooster)
        {
            pointsEarned *= 1.35;
        }
        if (selectedMinutes >= 15) {
            pointsEarned += 5;
        }

        return pointsEarned;
    }

    private void saveSessionToFirebase()
    {
        DatabaseReference sessionsRef = FirebaseDatabase.getInstance().getReference("StudySessions");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

        // generate a unique ID for this session
        String sessionId = sessionsRef.push().getKey();

        long durationSeconds = selectedMinutes * 60;
        int points = calcSessionScore();
        int aiScore = cameraManager.getAverageScore();
        String currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new java.util.Date());

        // create the session object
        StudySession session = new StudySession(
                sessionId,
                currentUserId,
                selectedSubject,
                currentTime,
                durationSeconds,
                aiScore,
                points
        );

        if (sessionId != null) {
            sessionsRef.child(sessionId).setValue(session)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            updateUserGlobalPoints(userRef, points, durationSeconds);

                            Toast.makeText(requireContext(), "Session saved! You earned " + points + " points", Toast.LENGTH_LONG).show();
                            navigateToFeedback(sessionId, aiScore, selectedMinutes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("firebase", "failed to save session", e);
                        }
                    });
        }
    }

    // helper method to update the user's total and current points
    private void updateUserGlobalPoints(DatabaseReference userRef, int earned, long durationSeconds) {
        userRef.get().addOnSuccessListener(new OnSuccessListener<com.google.firebase.database.DataSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // fetching current values from firebase
                    int currentPoints = 0;
                    int totalPoints = 0;
                    long totalTime = 0;

                    if (snapshot.hasChild("currentPoints"))
                        currentPoints = snapshot.child("currentPoints").getValue(Integer.class);
                    if (snapshot.hasChild("totalPoints"))
                        totalPoints = snapshot.child("totalPoints").getValue(Integer.class);
                    if (snapshot.hasChild("totalStudyTime"))
                        totalTime = snapshot.child("totalStudyTime").getValue(Long.class);

                    userRef.child("currentPoints").setValue(currentPoints + earned);
                    userRef.child("totalPoints").setValue(totalPoints + earned);
                    userRef.child("totalStudyTime").setValue(totalTime + durationSeconds);
                }
            }
        });
    }

    private void applyUserPreferences() {
        SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        // check for theme setting
        String activeTheme = sharedPref.getString("active_theme", "default");

        if (mainLayout != null) {
            // reset to default
            mainLayout.setBackgroundColor(Color.WHITE);
            tvSelectedTime.setTextColor(Color.BLACK);

            if (activeTheme.equals("dark")) {
                mainLayout.setBackgroundColor(Color.parseColor("#212121")); // dark gray
                tvSelectedTime.setTextColor(Color.WHITE);
            }
            else if (activeTheme.equals("pink")) {
                mainLayout.setBackgroundColor(Color.parseColor("#F8BBD0")); // light pink
                tvSelectedTime.setTextColor(Color.parseColor("#880E4F")); // dark pink text
            }
            else if (activeTheme.equals("nature")) {
                mainLayout.setBackgroundColor(Color.parseColor("#C8E6C9")); // light green
                tvSelectedTime.setTextColor(Color.parseColor("#1B5E20")); // dark green text
            }
        }

        String activeFont = sharedPref.getString("active_font", "default");

        if (activeFont.equals("retro")) {
            tvSelectedTime.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        }
        else if (activeFont.equals("classic")) {
            tvSelectedTime.setTypeface(Typeface.SERIF, Typeface.BOLD);
        }
        else {
            // default font
            tvSelectedTime.setTypeface(Typeface.DEFAULT_BOLD);
        }

        boolean hasBadge = sharedPref.getBoolean("has_golden_badge", false);
        if (hasBadge && ivGoldenBadge != null) {
            // if purchased, show the image on screen
            ivGoldenBadge.setVisibility(View.VISIBLE);
        }
        else if (ivGoldenBadge != null) {
            ivGoldenBadge.setVisibility(View.GONE);
        }
    }

    private void navigateToFeedback(String sessionId, int score, int durationMin) {
        // create the feedback fragment and bundle
        SessionFeedbackFragment feedbackFrag = new SessionFeedbackFragment();
        Bundle args = new Bundle();

        // put session data to pass it over
        args.putString("SESSION_ID", sessionId);
        args.putInt("SCORE", score);
        args.putLong("DURATION_MIN", (long) durationMin);
        feedbackFrag.setArguments(args);

        // switch fragments using the fragment manager
        if (isAdded()) { // ensure fragment is still attached to activity
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, feedbackFrag)
                    .addToBackStack(null) // lets user go back if they want
                    .commit();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMER_UPDATE");
        filter.addAction("TIMER_FINISHED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(timerReceiver, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timerReceiver);
        super.onPause();
    }

    private void resetUITimer() {
        isTimerRunning = false;
        btnToggleTimer.setText("Start!");
        btnToggleTimer.setVisibility(View.VISIBLE);
        btnLogOut.setEnabled(true);

        seekBarTime.setEnabled(true); // unlock seekbar
        tvSelectedTime.setText("0");
    }

    private void on_click_log_out() {
        // update shared preferences to stop auto-login
        SharedPreferences sP = requireActivity().getSharedPreferences("stay_logged_in", MODE_PRIVATE);
        SharedPreferences.Editor editor = sP.edit();
        editor.putBoolean("stayConnected", false);
        editor.apply();

        // sign out from firebase auth
        FirebaseAuth.getInstance().signOut();

        // back to sign activity
        Intent intent = new Intent(requireContext(), SignActivity.class);
        startActivity(intent);
        requireActivity().finish();
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

