package com.example.lockinapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainStudyActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_study);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        // set default fragment on first load
        if (savedInstanceState == null) {
            loadFragment(new StudyFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_study);
        }

        // setup navigation listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_stats) {
                selectedFragment = new StatsFragment();
            } else if (id == R.id.nav_study) {
                selectedFragment = new StudyFragment();
            } else if (id == R.id.nav_store) {
                selectedFragment = new StoreFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    // method to switch fragments
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}