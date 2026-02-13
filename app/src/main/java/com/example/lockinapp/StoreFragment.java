package com.example.lockinapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StoreFragment extends Fragment {
    private RecyclerView recyclerVStore;
    private TextView tVUserPoints;

    // firebase references
    private DatabaseReference rewardsRef;
    private DatabaseReference usersRef;
    private DatabaseReference userRewardsRef;

    private String currentUserId;
    private int currentUserPoints = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.store_fragment, container, false);

        // initializing views
        recyclerVStore = view.findViewById(R.id.recyclerVStore);
        tVUserPoints = view.findViewById(R.id.tVUserPoints);

        // initializing firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        rewardsRef = database.getReference("Rewards");
        usersRef = database.getReference("Users").child(currentUserId);
        userRewardsRef = database.getReference("UserRewards").child(currentUserId);

        // loading data
        loadUserPoints();
        // todo: loadRewardsFromFirebase();

        return view;
    }

    private void loadUserPoints() {
        usersRef.child("currentPoints").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // getting the integer value safely
                    Integer points = snapshot.getValue(Integer.class);
                    if (points != null) {
                        currentUserPoints = points;
                        tVUserPoints.setText("My Points: " + currentUserPoints);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handling database errors
                Toast.makeText(getContext(), "Failed to load points", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
