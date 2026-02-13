package com.example.lockinapp;

import android.content.Context;
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

    // interface to handle clicks in the fragment instead of the adapter
    public interface OnItemClickListener {
        void onBuyClick(Reward reward);
    }

    public StoreAdapter(Context context, List<Reward> rewardList, OnItemClickListener listener) {
        this.context = context;
        this.rewardList = rewardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflating the custom layout for a reward item
        View view = LayoutInflater.from(context).inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        Reward currentReward = rewardList.get(position);

        // setting text values
        holder.textViewName.setText(currentReward.getName());
        holder.textViewCost.setText(currentReward.getCost() + " Points");

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

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    // inner class to hold the views for efficient scrolling
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