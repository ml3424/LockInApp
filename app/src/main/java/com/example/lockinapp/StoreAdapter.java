package com.example.lockinapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // library for loading images from url
import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.RewardViewHolder> {

    private Context context;
    private List<Reward> rewardList;
    private OnItemClickListener listener;

    /**
     * Communication bridge between the Adapter and the Fragment.
     * <p>
     * This interface allows the Fragment to handle purchase logic,
     * keeping the Adapter focused purely on data display.
     */
    public interface OnItemClickListener {
        /**
         * Triggered when the user clicks the action button on a reward item.
         * @param reward The specific reward associated with the clicked item.
         */
        void onBuyClick(Reward reward);
    }

    /**
     * Initializes the adapter with reward data and a click listener.
     * @param context  The fragment context for layout inflation and SharedPreferences.
     * @param rewardList The list of {@link Reward} objects to display in the store.
     * @param listener Implementation of the click handler for purchase/equip actions.
     */
    public StoreAdapter(Context context, List<Reward> rewardList, OnItemClickListener listener) {
        this.context = context;
        this.rewardList = rewardList;
        this.listener = listener;
    }

    /**
     * Inflates the XML layout for an individual reward item and wraps it in a ViewHolder.
     * <p>
     * This is called by the RecyclerView when it needs a new item view to display.
     */
    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    /**
     * Binds reward data to the UI components of a specific item in the grid.
     * <p>
     * This method performs several key tasks:
     * <ul>
     * <li>Sets the name and point cost text.</li>
     * <li>Updates the button's appearance (Buy/Equip/Equipped).</li>
     * <li>Asynchronously loads the reward image from a URL using the Glide library.</li>
     * <li>Attaches the click listener to the action button.</li>
     * </ul>
     */
    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        Reward currentReward = rewardList.get(position);

        holder.textViewName.setText(currentReward.getName());
        holder.textViewCost.setText(currentReward.getCost() + " Points");

        updateButtonState(holder, currentReward.getRewardId());

        // using glide library to load image from internet url into imageview
        Glide.with(context)
                .load(currentReward.getImageUrl())
                .placeholder(R.drawable.ic_launcher_foreground) // default image while loading
                .into(holder.imageViewReward);

        holder.buttonBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onBuyClick(currentReward);
                }
            }
        });
    }

    /**
     * Dynamically updates the purchase/equip button based on item ownership and activity.
     * <p>
     * This method checks {@code SharedPreferences} to determine the current state
     * of a reward and applies the following visual logic:
     * <ul>
     * <li><b>Equipped:</b> The item is currently active (Disabled, Gray).</li>
     * <li><b>Equip:</b> The item is owned but not active (Enabled, Orange).</li>
     * <li><b>Buy:</b> The item is not yet owned (Enabled, Blue).</li>
     * </ul>
     *
     * @param holder   The ViewHolder containing the UI components for the reward item.
     * @param rewardId The unique identifier of the reward being processed.
     */
    private void updateButtonState(RewardViewHolder holder, String rewardId) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean isOwned = sharedPref.getBoolean("owned_" + rewardId, false);
        boolean isActive = false;

        String activeTheme = sharedPref.getString("active_theme", "");
        String activeFont = sharedPref.getString("active_font", "");

        if (rewardId.equals("r1") && activeTheme.equals("pink")) isActive = true;
        else if (rewardId.equals("r2") && activeTheme.equals("dark")) isActive = true;
        else if (rewardId.equals("r4") && activeTheme.equals("nature")) isActive = true;
        else if (rewardId.equals("r5") && activeFont.equals("retro")) isActive = true;
        else if (rewardId.equals("r7") && activeFont.equals("classic")) isActive = true;
        else if ((rewardId.equals("r3") || rewardId.equals("r6")) && isOwned) isActive = true;

        if (isActive) {
            holder.buttonBuy.setEnabled(false);
            holder.buttonBuy.setText("Equipped");
            holder.buttonBuy.setBackgroundColor(android.graphics.Color.GRAY);
        } else if (isOwned) {
            holder.buttonBuy.setEnabled(true);
            holder.buttonBuy.setText("Equip");
            holder.buttonBuy.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));
        } else {
            holder.buttonBuy.setEnabled(true);
            holder.buttonBuy.setText("Buy");
            holder.buttonBuy.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
        }
    }

    /**
     * Returns the total number of items in the reward list.
     * <p>
     * This tells the {@code RecyclerView} how many rows or grid cells
     * it needs to prepare for display.
     *
     * @return The size of the {@code rewardList}.
     */
    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    /**
     * A container for the UI components of a single reward item.
     * <p>
     * The {@code ViewHolder} pattern is used to "cache" references to the
     * views (TextViews, ImageView, Button). This avoids repeated calls to
     * {@code findViewById}, which significantly improves scrolling performance
     * and reduces battery consumption.
     */
    public static class RewardViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewCost;
        ImageView imageViewReward;
        Button buttonBuy;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewName = itemView.findViewById(R.id.tVRewardName);
            textViewCost = itemView.findViewById(R.id.tVRewardCost);
            imageViewReward = itemView.findViewById(R.id.iVReward);
            buttonBuy = itemView.findViewById(R.id.btnBuy);
        }
    }
}