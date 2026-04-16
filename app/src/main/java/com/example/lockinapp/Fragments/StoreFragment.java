package com.example.lockinapp.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import com.example.lockinapp.R;
import com.example.lockinapp.Objects.Reward;
import com.example.lockinapp.Adapters.StoreAdapter;
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
    private List<Reward> rewardList  = new ArrayList<>();

    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private int currentUserPoints = 0;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference rewardsRef = database.getReference("Rewards");
    private DatabaseReference usersRef = database.getReference("Users").child(currentUserId);
    private DatabaseReference userRewardsRef = database.getReference("UserRewards").child(currentUserId);

    /**
     * Initializes the store UI and handles item interaction logic.
     * <p>
     * Sets up a 2-column grid for rewards and defines a smart click behavior:
     * <ul>
     * <li>If the item is already owned (verified with {@code SharedPreferences}),
     * it equips the item immediately.</li>
     * <li>If the item is new, it triggers the {@link #purchase(Reward)} flow.</li>
     * </ul>
     *
     * @return The root view for the Store/Rewards screen.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.store_fragment, container, false);

        recyclerVStore = view.findViewById(R.id.recyclerVStore);
        tVUserPoints = view.findViewById(R.id.tVUserPoints);

        // setting up recyclerview with grid layout (2 columns)
        recyclerVStore.setLayoutManager(new GridLayoutManager(getContext(), 2));

        storeAdapter = new StoreAdapter(getContext(), rewardList, new StoreAdapter.OnItemClickListener() {
            @Override
            public void onBuyClick(Reward reward) {
                SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                boolean isOwned = sharedPref.getBoolean("owned_" + reward.getRewardId(), false);

                if (isOwned) {
                    equipReward(reward);
                    Toast.makeText(getContext(), "Equipped: " + reward.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    purchase(reward);
                }
            }
        });
        recyclerVStore.setAdapter(storeAdapter);

        loadUserPoints();
        fetchStoreCatalog();
        loadPurchases();

        return view;
    }

    /**
     * Synchronizes the user's total points with the Firebase.
     * <p>
     * This method attaches a live listener to the {@code currentPoints} node.
     * Any changes in the database (e.g., earning points from a session or
     * spending them in the store) will automatically trigger a UI update
     * in the {@code tVUserPoints} display.
     */
    private void loadUserPoints() {
        usersRef.child("currentPoints").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                if (snapshot.exists()) {
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


    /**
     * Synchronizes purchased rewards from Firebase to local storage.
     * <p>
     * This method listens to the user's purchase history in the cloud and
     * updates the local {@code SharedPreferences} with "owned" flags for each
     * reward ID found.
     * <p>
     * If new purchases are detected, it applies the changes locally and
     * refreshes the {@code StoreAdapter} to update the button states from
     * "Buy" to "Equip" or "Equipped".
     */
    private void loadPurchases() {
        userRewardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                boolean hasChanges = false;

                for (DataSnapshot purchaseSnapshot : snapshot.getChildren()) {
                    String rewardId = purchaseSnapshot.child("rewardId").getValue(String.class);
                    if (rewardId != null) {
                        editor.putBoolean("owned_" + rewardId, true);
                        hasChanges = true;
                    }
                }

                if (hasChanges) {
                    editor.apply();
                    if (storeAdapter != null) {
                        storeAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load store", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetches the global list of available rewards from the database.
     * <p>
     * This method synchronizes the UI with the master catalog in Firebase.
     * It clears the local list to ensure fresh data and triggers a
     * {@code notifyDataSetChanged()} to refresh the grid layout once the
     * data is received.
     * <p>
     * Note: While this method loads metadata (names, prices), the actual
     * images are managed and cached by the Glide library in the adapter.
     */
    private void fetchStoreCatalog() {
        rewardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                rewardList.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Reward reward = childSnapshot.getValue(Reward.class);
                    if (reward != null) {
                        rewardList.add(reward);
                    }
                }

                if (storeAdapter != null) {
                    storeAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StoreFragment", "Error fetching catalog", error.toException());
            }
        });
    }

    /**
     * Processes a reward purchase by validating points and updating the database.
     * <p>
     * This method performs a multi-step transaction:
     * <ul>
     * <li>Checks if the user has sufficient points.</li>
     * <li>Deducts the cost and updates the {@code currentPoints} in Firebase.</li>
     * <li>Records the purchase with a timestamp in the {@code UserRewards} node.</li>
     * <li>Updates {@code SharedPreferences} to reflect ownership locally.</li>
     * <li>Automatically triggers {@link #equipReward(Reward)} for immediate use.</li>
     * </ul>
     *
     * @param reward The item the user is attempting to buy.
     */
    private void purchase(Reward reward) {
        if (currentUserPoints >= reward.getCost()) {
            // user has enough points
            int newPoints = currentUserPoints - reward.getCost();
            usersRef.child("currentPoints").setValue(newPoints);

            // add item to "UserRewards" node with timestamp
            String purchaseKey = userRewardsRef.push().getKey();
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("rewardId", reward.getRewardId());
            purchaseData.put("purchaseDate", currentDate);

            if (purchaseKey != null) {
                userRewardsRef.child(purchaseKey).setValue(purchaseData);
            }

            SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            sharedPref.edit().putBoolean("owned_" + reward.getRewardId(), true).apply();

            equipReward(reward);
            Toast.makeText(getContext(), "Purchased: " + reward.getName() + "!", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getContext(), "Not enough points!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Activates a purchased reward and applies its effects to the user's profile.
     * <p>
     * Depending on the reward type, this method updates {@code SharedPreferences} to:
     * <ul>
     * <li><b>Themes:</b> Switch the app's visual skin (e.g., Pink, Dark, Nature).</li>
     * <li><b>Fonts:</b> Update the typography style (e.g., Retro, Classic).</li>
     * <li><b>Boosters:</b> Enable gameplay modifiers like point multipliers or badges.</li>
     * </ul>
     * It also refreshes the {@code RecyclerView} to reflect the "Equipped" state visually.
     *
     * @param reward The selected reward to be applied.
     */
    private void equipReward(Reward reward)
    {
        String rewardId = reward.getRewardId();
        SharedPreferences sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // themes
        if (rewardId.equals("r1")) editor.putString("active_theme", "pink");
        else if (rewardId.equals("r2")) editor.putString("active_theme", "dark");
        else if (rewardId.equals("r4")) editor.putString("active_theme", "nature");

        // fonts
        else if (rewardId.equals("r5")) editor.putString("active_font", "retro");
        else if (rewardId.equals("r7")) editor.putString("active_font", "classic");

        // boosters
        else if (rewardId.equals("r3")) editor.putBoolean("has_point_booster", true);
        //else if (rewardId.equals("r6")) editor.putBoolean("has_golden_badge", true);

        editor.apply();

        if (storeAdapter != null) {
            storeAdapter.notifyDataSetChanged();
        }
    }
}
