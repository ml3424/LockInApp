package com.example.lockinapp.Activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.lockinapp.R;
import com.example.lockinapp.Fragments.StatsFragment;
import com.example.lockinapp.Fragments.StoreFragment;
import com.example.lockinapp.Fragments.StudyFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * The type Main study activity.
 */
public class MainStudyActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    /**
     * Sets up the main study screen, loads the default {@code StudyFragment} on startup.
     */
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

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_stats) {
                    selectedFragment = new StatsFragment();
                }
                else if (id == R.id.nav_study) {
                    selectedFragment = new StudyFragment();
                }
                else if (id == R.id.nav_store) {
                    selectedFragment = new StoreFragment();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                }

                // return true to display the item as the selected item
                return true;
            }
        });
    }

    /**
     * Replaces the current fragment in the container with a new one.
     * @param fragment The new Fragment instance to display.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}