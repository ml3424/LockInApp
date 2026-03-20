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
    private String selectedSubject = "";
    private boolean isTimerRunning = false;
    private int selectedMinutes = 0;

    /**
     * Launcher to handle the runtime camera permission request.
     * If permission is granted, it starts the camera using cameraManager.
     * If denied, it displays a Toast explaining why the permission is necessary.
     */
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

    /**
     * Receiver that listens to broadcasts from TimerService.
     * Updates the UI with the remaining time or triggers the session end logic
     * when the timer finishes.
     */
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("TIMER_UPDATE")) {
                String timeLeft = intent.getStringExtra("TIME_LEFT");
                tvSelectedTime.setText(timeLeft); // update the minutes on screen
            }
            else if (intent.getAction().equals("TIMER_FINISHED")) {
                handleStudySessionEnd();
            }
        }
    };

    public StudyFragment() {}

    /**
     * Inflates the fragment's layout and initializes its UI components and managers.
     * <p>
     * During the inflation process, the XML layout is converted into a View object.
     * This method also sets up the camera manager, permissions, and UI listeners for the study session.
     *
     * @param inflater The LayoutInflater object to inflate views in the fragment.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous state.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.study_fragment, container, false);

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

        checkCameraPermissions();

        setupSpinner();
        setupSeekBar();
        setupButtons();
        applyUserPreferences();

        return view;
    }

    /**
     * Resumes fragment operations, synchronizes timer state, and registers receivers.
     * <p>
     * This method checks SharedPreferences to see if a background timer has expired
     * while the app was inactive. If active, it restores the broadcast receiver
     * for live updates and resumes camera monitoring.
     */
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean timerWasRunning = prefs.getBoolean("is_timer_running_bg", false);

        if (timerWasRunning) {
            long expectedEndTime = prefs.getLong("expected_end_time", 0);
            if (System.currentTimeMillis() >= expectedEndTime) {
                prefs.edit().putBoolean("is_timer_running_bg", false).apply();
                handleStudySessionEnd();
                return;
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMER_UPDATE");
        filter.addAction("TIMER_FINISHED");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(timerReceiver, filter);

        if (cameraManager != null && isTimerRunning) {
            cameraManager.resumeCaptures();
        }
    }

    /**
     * Suspends fragment operations and cleans up transient resources.
     * <p>
     * Unregisters the {@code timerReceiver} to prevent memory leaks and
     * pauses camera captures to conserve system resources while the fragment
     * is not in the foreground.
     */
    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timerReceiver);

        if (cameraManager != null) {
            cameraManager.pauseCaptures();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isTimerRunning) {
            stopTimer();
            saveSessionToFirebase();

            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("show_feedback_on_return", true).apply();
        }
    }

    /**
     * Configures the subjects Spinner with a predefined list of academic topics.
     * <p>
     * Sets up the {@code ArrayAdapter} and attaches an item selection listener
     * to handle subject changes during the study session.
     */
    private void setupSpinner() {
        String[] subjects = {"Math", "English", "History", "Computer Science", "Physics"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerSubjects.setAdapter(adapter);
        spinnerSubjects.setOnItemSelectedListener(this);
    }

    /**
     * Initializes the SeekBar to control study session duration.
     * <p>
     * Updates {@code tvSelectedTime} in real-time as the user slides the bar,
     * ensuring a minimum value of 1 minute.
     */
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

    /**
     * Assigns click listeners to the fragment's primary buttons.
     * <p>
     * Links the timer toggle and logout buttons to their respective logic handlers.
     */
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

    /**
     * Verifies and requests camera permissions if not already granted.
     * <p>
     * If access is permitted, it triggers the camera start {@code StudyCameraManager} startCamera logic;
     * otherwise, it launches the permission request.
     */
    private void checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
        else {
            cameraManager.startCamera();
        }
    }

    /**
     * Updates the {@code selectedSubject} based on the user's choice in the Spinner.
     * @param parent The AdapterView where the selection happened.
     * @param view The view within the AdapterView that was clicked.
     * @param position The position of the view in the adapter.
     * @param id The row id of the item that is selected.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedSubject = parent.getItemAtPosition(position).toString();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Saves study session end time, initiates the study session timer and background monitoring.
     * <p>
     * This method disables navigation controls, starts the {@code TimerService} with
     * the selected duration, and starts camera manager to begin focus tracking.
     */
    private void startTimer() {
        // debug:
        // navigateToFeedback("0", 0, 0);
        // TODO: remove that

        if (selectedMinutes > 0)
        {
            // save end time and that the timer is running
            // so if the user leave the app and the session finished, the app will show the end of the session
            long expectedEndTime = System.currentTimeMillis() + ((long) selectedMinutes * 60 * 1000);
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putLong("expected_end_time", expectedEndTime)
                    .putBoolean("is_timer_running_bg", true)
                    .apply();

            btnLogOut.setEnabled(false);

            Intent serviceIntent = new Intent(requireContext(), TimerService.class);
            serviceIntent.putExtra("DURATION_MIN", selectedMinutes);
            requireActivity().startService(serviceIntent);

            // able stoping the timer
            isTimerRunning = true;
            btnToggleTimer.setText("Stop Timer");
            seekBarTime.setEnabled(false);

            cameraManager.startRandomCaptures();
        }
    }

    /**
     * Manually ends the current study session.
     * <p>
     * Stops the {@code TimerService}, stops background camera captures,
     * and resets the UI.
     */
    private void stopTimer() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_timer_running_bg", false).apply();

        Intent serviceIntent = new Intent(requireContext(), TimerService.class);
        requireContext().stopService(serviceIntent);

        cameraManager.stopRandomCaptures();
        resetUITimer();
    }

    /**
     * Finalizes the study session upon completion.
     * <p>
     * Stops camera monitoring, resets the UI,and saves the data to Firebase.
     */
    private void handleStudySessionEnd()
    {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_timer_running_bg", false).apply();

        cameraManager.stopRandomCaptures();
        resetUITimer();
        saveSessionToFirebase();
    }

    /**
     * Calculates the total points earned during the study session.
     * <p>
     * The score is based on a base rate per minute, adjusted by AI focus levels,
     * active store boosters (1.35x), and long-session bonuses.
     * @return The final calculated points as an integer.
     */
    private int calcSessionScore() {
        int aiScore = cameraManager.getAverageScore();
        int pointsEarned = selectedMinutes * 10; // Base: 10 pts/min

        // Apply AI focus multiplier
        if (aiScore > 80) pointsEarned *= 1.2;
        else if (aiScore < 50 && aiScore > 0) pointsEarned *= 0.9;

        // Apply Shop Booster
        if (requireContext()
                .getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getBoolean("has_point_booster", false)) {
            pointsEarned *= 1.35;
        }

        // Long session bonus
        if (selectedMinutes >= 15) pointsEarned += 5;

        return pointsEarned;
    }

    /**
     * Saves the completed study session data to Firebase.
     * <p>
     * This method creates a {@code StudySession} object, generates a unique ID,
     * and updates both the sessions log and the user's global point total.
     * Upon success, it navigates to the feedback screen.
     */
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

    /**
     * Updates user statistics.
     * <p>
     * @param userRef Reference to the user's database node.
     * @param earned Points to add.
     * @param durationSeconds Seconds to add to total study time.
     */
    private void updateUserGlobalPoints(DatabaseReference userRef, int earned, long durationSeconds) {
        userRef.get().addOnSuccessListener(new OnSuccessListener<com.google.firebase.database.DataSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
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

    /**
     * Applies saved user preferences to the UI, including themes, fonts, and badges.
     * <p>
     * Fetches styles from {@code SharedPreferences} and updates the layout background,
     * text colors, typefaces, and the visibility of the "Golden Badge" accordingly.
     */
    private void applyUserPreferences() {
        SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
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

    /**
     * Navigates to the Feedback Fragment and passes session results using a Bundle.
     * <p>
     * Ensures the current fragment is attached before performing the transaction
     * and adds the transition to the back stack to allow user navigation.
     * @param sessionId Unique ID of the saved session.
     * @param score The points calculated for the session.
     * @param durationMin The total duration of the session in minutes.
     */
    private void navigateToFeedback(String sessionId, int score, int durationMin) {
        SessionFeedbackFragment feedbackFrag = new SessionFeedbackFragment();
        Bundle args = new Bundle();

        args.putString("SESSION_ID", sessionId);
        args.putInt("SCORE", score);
        args.putLong("DURATION_MIN", (long) durationMin);
        feedbackFrag.setArguments(args);

        if (isAdded()) { // ensure fragment is still attached to activity
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, feedbackFrag)
                    .addToBackStack(null) // lets user go back if they want
                    .commit();
        }
    }

    /**
     * Resets the UI components to their initial state after a session ends or stopped.
     * <p>
     * Re-enables the SeekBar and logout button, resets the timer toggle text,
     * and clears the displayed time.
     */
    private void resetUITimer() {
        isTimerRunning = false;
        btnToggleTimer.setText("Start!");
        btnToggleTimer.setVisibility(View.VISIBLE);
        btnLogOut.setEnabled(true);

        seekBarTime.setEnabled(true); // unlock seekbar
        tvSelectedTime.setText("1");
    }

    /**
     * Handles the logout process and cleans up user session data.
     * <p>
     * Updates {@code SharedPreferences} to disable the "stayConnected" flag,
     * signs out from Firebase, and redirects the user back to the {@code SignActivity}.
     */
    private void on_click_log_out() {
        // update shared preferences to stop auto-login
        SharedPreferences sP = requireActivity().getSharedPreferences("stay_logged_in", MODE_PRIVATE);
        SharedPreferences.Editor editor = sP.edit();
        editor.putBoolean("stayConnected", false);
        editor.apply();

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(requireContext(), SignActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    /**
     * Toggles the study session timer state based on its current status.
     * <p>
     * if the timer is already running, stops the active session
     * else, starts a new session.
     *
     * @param view The view that triggered the click event.
     */
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

