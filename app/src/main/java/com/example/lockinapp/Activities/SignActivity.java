package com.example.lockinapp.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lockinapp.R;
import com.example.lockinapp.Objects.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * The type Sign activity.
 */
public class SignActivity extends AppCompatActivity {

    private final String logoUrl = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Rocket/3D/rocket_3d.png";

    private EditText eTEmail, eTPassword;
    private TextView tVSign, tVSignClick;
    private CheckBox cBRemember;
    private ImageView iVAppLogo;

    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private SharedPreferences sP;

    private int sign = 0;

    /**
     * Initializes the activity and implements a "Lazy Sign-out" strategy.
     * <p>
     * Checks if the user enabled "stayConnected". If not, signs them out only on
     * a fresh app launch (onCreate) to prevent logout during multitasking,
     * while ensuring re-authentication is required after the app is closed.
     *
     * @param savedInstanceState Bundle containing the activity's saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_activity);

        eTEmail = findViewById(R.id.eTEmail);
        eTPassword = findViewById(R.id.eTPassword);
        tVSign = findViewById(R.id.tVSign);
        tVSignClick = findViewById(R.id.tVSignClick);
        cBRemember = findViewById(R.id.cBRemember);
        iVAppLogo = findViewById(R.id.iVLogo);

        sP = getSharedPreferences("stay_logged_in", MODE_PRIVATE);
        Glide.with(this).load(logoUrl).into(iVAppLogo);

        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        boolean isRemembered = sP.getBoolean("stayConnected", false);

        if (currentUser != null) {
            if(isRemembered)
            {
                goToMainActivity(currentUser.getUid());
            }
            else {
                mFirebaseAuth.signOut();
            }
        }
    }

    /**
     * Registers a new user using Firebase Authentication and initializes their profile in the database.
     * <p>
     * This method validates the email and password input fields, creates a new account,
     * saves login preferences, and stores a new {@code User} object in the Realtime Database
     * before navigating to the main activity.
     */
    private void registerUser() {
        String email = eTEmail.getText().toString().trim();
        String password = eTPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_LONG).show();
            return;
        }

        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            saveLoginPreference();

                            String userId = mFirebaseAuth.getCurrentUser().getUid();
                            User newUser = new User(userId, email.split("@")[0]); // the name is the first part of the email

                            mDatabase.child("Users").child(userId).setValue(newUser)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> dbTask) {
                                            if (dbTask.isSuccessful()) {
                                                Toast.makeText(SignActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();

                                                goToMainActivity(userId);
                                            }
                                            else {
                                                Toast.makeText(SignActivity.this, "DB Error: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                        }
                        else // user creation failed - log and inform user
                        {
                            String errorMessage;
                            if (task.getException() != null) errorMessage = task.getException().getMessage();
                            else errorMessage = "Unknown Error";

                            Log.e("Registration", "Registration Failed: " + errorMessage);
                            Toast.makeText(SignActivity.this, "Error occurred - " + errorMessage , Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Authenticates an existing user with Firebase using an email and password.
     * <p>
     * This method retrieves user credentials from the input fields, performs a basic
     * validation check, and attempts to sign in. Upon success, it updates login
     * preferences and navigates the user to the main activity.D
     */
    private void signinUser() {
        String email = eTEmail.getText().toString().trim();
        String password = eTPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mFirebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            saveLoginPreference();

                            Toast.makeText(SignActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            String userId = mFirebaseAuth.getCurrentUser().getUid();

                            goToMainActivity(userId);
                        }
                        else {
                            Toast.makeText(SignActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Saves the user's login preference to SharedPreferences.
     * <p>
     * Specifically, it stores whether the "Remember Me" checkbox is checked
     * to determine if the user should remain connected in future sessions.
     */
    private void saveLoginPreference() {
        SharedPreferences.Editor editor = sP.edit();
        editor.putBoolean("stayConnected", cBRemember.isChecked());
        editor.apply();
    }

    /**
     * Navigates from the sign-in/up screen to the main study activity.
     * * @param userId The unique identifier of the authenticated user to be passed
     * to the next activity.
     */
    private void goToMainActivity(String userId) {
        Intent studyIntent = new Intent(SignActivity.this, MainStudyActivity.class);
        studyIntent.putExtra("USER_ID", userId);
        startActivity(studyIntent);
        finish(); // to get back to the screen only use logout
    }

    /**
     * Handles the main action button click (Sign In or Sign Up).
     * <p>
     * Depending on the current toggle state ({@code sign}), it triggers
     * either the login or the registration flow.
     * * @param view The view that was clicked.
     *
     * @param view the view
     */
    public void onEnter(View view) {
        if(sign == 0) // sign in = 0
        {
            signinUser();
        }
        else // sign up = 1
        {
            registerUser();
        }
    }

    /**
     * Toggles the UI state between "Sign In" and "Sign Up" modes.
     * <p>
     * Updates the {@code sign} flag and modifies the text of relevant
     * TextViews to reflect the current mode to the user.
     * * @param view The view that was clicked.
     *
     * @param view the view
     */
    public void on_change_sign(View view) {
        if(sign == 0) // sign in = 0
        {
            sign = 1;
            tVSignClick.setText("To sign in click");
            tVSign.setText("Sign up:");
        }
        else // signup = 1
        {
            sign = 0;
            tVSignClick.setText("To sign up click");
            tVSign.setText("Sign in:");
        }
    }
}