package com.example.lockinapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignActivity extends AppCompatActivity {

    private EditText eTEmail, eTPassword;
    private TextView tVSign, tVSignClick;
    private CheckBox cBRemember;

    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private SharedPreferences sP; // shared preferences reference

    private int sign = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_activity);

        eTEmail = findViewById(R.id.eTEmail);
        eTPassword = findViewById(R.id.eTPassword);
        tVSign = findViewById(R.id.tVSign);
        tVSignClick = findViewById(R.id.tVSignClick);
        cBRemember = findViewById(R.id.cBRemember);

        sP = getSharedPreferences("stay_logged_in", MODE_PRIVATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // check if user is already signed in and checkbox was checked
        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        boolean isRemembered = sP.getBoolean("stayConnected", false); // get key stayConnected from shared preferences

        if (currentUser != null && isRemembered) {
            // go directly to main study activity
            goToMainActivity(currentUser.getUid());
        }
    }

    private void registerUser() {
        // extract input text and trim space
        String email = eTEmail.getText().toString().trim();
        String password = eTPassword.getText().toString().trim();

        // check the fields are not empty
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_LONG).show();
            return;
        }
        // use firebase auth to create a new user
        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            // save preference based on checkbox
                            saveLoginPreference();

                            String userId = mFirebaseAuth.getCurrentUser().getUid(); // UID as userId
                            User newUser = new User(userId, email.split("@")[0]); // the name is the first part of the email

                            mDatabase.child("Users").child(userId).setValue(newUser)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> dbTask) {
                                            if (dbTask.isSuccessful()) {
                                                Toast.makeText(SignActivity.this, "Registered successfully!", Toast.LENGTH_SHORT).show();

                                                goToMainActivity(userId);
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
        // create user to save in firebase
    }

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
                            // save preference based on checkbox
                            saveLoginPreference();

                            Toast.makeText(SignActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();

                            // retrieve the unique identifier for the authenticated user
                            String userId = mFirebaseAuth.getCurrentUser().getUid();

                            goToMainActivity(userId);
                        } else {
                            // provide specific feedback from firebase if the authentication fails
                            Toast.makeText(SignActivity.this, "Login failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // helper method to save preference
    private void saveLoginPreference() {
        SharedPreferences.Editor editor = sP.edit();
        editor.putBoolean("stayConnected", cBRemember.isChecked());
        editor.apply();
    }

    // helper method to navigate to main screen
    private void goToMainActivity(String userId) {
        Intent studyIntent = new Intent(SignActivity.this, MainStudyActivity.class);
        studyIntent.putExtra("USER_ID", userId);
        startActivity(studyIntent);
        finish(); // to get back to the screen only use logout
    }

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