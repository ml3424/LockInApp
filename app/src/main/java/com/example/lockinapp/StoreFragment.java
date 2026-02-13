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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StoreFragment extends Fragment {
    private RecyclerView recyclerVStore;
    private TextView tVUserPoints;

    private StoreAdapter storeAdapter;
    private List<Reward> rewardList;

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

        // setting up recyclerview with grid layout (2 columns)
        recyclerVStore.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rewardList = new ArrayList<>();

        // initializing firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        rewardsRef = database.getReference("Rewards");
        usersRef = database.getReference("Users").child(currentUserId);
        userRewardsRef = database.getReference("UserRewards").child(currentUserId);

        storeAdapter = new StoreAdapter(getContext(), rewardList, new StoreAdapter.OnItemClickListener() {
            @Override
            public void onBuyClick(Reward reward) {
                // calling the purchase method when button is clicked
                purchase(reward);
            }
        });
        recyclerVStore.setAdapter(storeAdapter);

        // loading data
        loadUserPoints();
        loadRewardsFromFirebase();

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
                Toast.makeText(getContext(), "Failed to load points", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRewardsFromFirebase() {
        rewardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rewardList.clear(); // clearing list to avoid duplicates
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Reward reward = dataSnapshot.getValue(Reward.class);
                    rewardList.add(reward);
                }
                storeAdapter.notifyDataSetChanged(); // notifying adapter to refresh the grid
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load store", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void purchase(Reward reward) {
        if (currentUserPoints >= reward.getCost()) {
            // user has enough points
            int newPoints = currentUserPoints - reward.getCost();
            usersRef.child("currentPoints").setValue(newPoints);

            // add item to "UserRewards" node with timestamp
            String purchaseKey = userRewardsRef.push().getKey(); // create unique key
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // map for the new purchase entry
            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("rewardId", reward.getRewardId());
            purchaseData.put("purchaseDate", currentDate);

            if (purchaseKey != null) {
                userRewardsRef.child(purchaseKey).setValue(purchaseData);
            }

            Toast.makeText(getContext(), "Purchased: " + reward.getName() + "!", Toast.LENGTH_SHORT).show();

        }
        else {
            Toast.makeText(getContext(), "Not enough points!", Toast.LENGTH_SHORT).show();
        }
    }
}
