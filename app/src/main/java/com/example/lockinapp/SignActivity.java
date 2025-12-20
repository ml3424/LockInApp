package com.example.lockinapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignActivity extends AppCompatActivity {

    public EditText eTEmail, eTPassword;
    public TextView tVSign, tVSignClick;

    public FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    public int sign = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_activity);

        eTEmail = findViewById(R.id.eTEmail);
        eTPassword = findViewById(R.id.eTPassword);
        tVSign = findViewById(R.id.tVSign);
        tVSignClick = findViewById(R.id.tVSignClick);
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
                        if (!task.isSuccessful())
                        {

                        }
                        else // user creation failed - log and inform user
                        {
                            String errorMessage;
                            if (task.getException() != null) errorMessage = task.getException().getMessage();
                            else errorMessage = "Unknown Error";

                            Log.e("Registration", "Registration Failed: " + errorMessage);
                            Toast.makeText(SignActivity.this, "Error occurred", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        // create user to save in firebase
    }

    private void signinUser() {

    }

    public void onEnter(View view) {
        if(sign == 0) // sign in = 0
        {
            signinUser();
        }
        else
        {
            registerUser();
        }
    }

    public void on_change_sign(View view) {
        if(sign == 0) // sign in = 0
        {
            sign = 1;
            tVSignClick.setText("To sign up click");
            tVSign.setText("Sign in:");
        }
        else // signup = 1
        {
            sign = 0;
            tVSignClick.setText("To sign in click");
            tVSign.setText("Sign up:");
        }
    }
}