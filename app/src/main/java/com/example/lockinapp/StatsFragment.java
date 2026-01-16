package com.example.lockinapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StatsFragment extends Fragment {

    private BarChart barChart;
    private Spinner spinnerStatsSubject;
    private TextView tvDetailedStats;

    private String currentUserId;
    private List<StudySession> allSessions;
    private List<String> dateLabels = new ArrayList<>();

    public StatsFragment() {
        // required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_fragment, container, false);

        // link views
        barChart = view.findViewById(R.id.barChart);
        spinnerStatsSubject = view.findViewById(R.id.spinnerStatsSubject);
        tvDetailedStats = view.findViewById(R.id.tvDetailedStats);

        allSessions = new ArrayList<>();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupSpinner();
        loadDataFromFirebase();

        return view;
    }

    private void setupSpinner() {
        // added "General" as the first option
        String[] subjects = {"General", "Math", "English", "History", "Computer Science", "Physics"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerStatsSubject.setAdapter(adapter);

        spinnerStatsSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // recalculate stats when subject changes
                updateStats(parent.getItemAtPosition(position).toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDataFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("StudySessions");

        // fetch only sessions belonging to this user
        ref.orderByChild("userId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSessions.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    StudySession session = data.getValue(StudySession.class);
                    if (session != null) {
                        allSessions.add(session);
                    }
                }
                // initial update with General
                updateStats(spinnerStatsSubject.getSelectedItem().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // failed to read from firebase
            }
        });
    }

    private void updateStats(String selectedSubject) {
        long totalSeconds = 0;
        int totalConcentration = 0;
        int count = 0;

        // map to keeps dates sorted automatically
        java.util.Map<String, Float> dateMap = new java.util.TreeMap<>();

        for (StudySession s : allSessions) {
            if (selectedSubject.equals("General") || s.subject.equals(selectedSubject)) {
                totalSeconds += s.durationSeconds;
                totalConcentration += s.aiConcentrationScore;
                count++;

                // extract date only (yyyy-MM-dd)
                String date = s.startTime.split(" ")[0];
                float minutes = s.durationSeconds / 60f;

                if (dateMap.containsKey(date)) {
                    dateMap.put(date, dateMap.get(date) + minutes);
                } else {
                    dateMap.put(date, minutes);
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        dateLabels.clear(); // clear old labels
        int index = 0;

        for (java.util.Map.Entry<String, Float> entry : dateMap.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue()));
            dateLabels.add(entry.getKey()); // save the date string as a label
        }

        // calculate averages
        double avgConcentration = (count > 0) ? (double) totalConcentration / count : 0;
        long totalMinutes = totalSeconds / 60;

        String info = "Selected: " + selectedSubject +
                "\nTotal Study Time: " + totalMinutes + " min" +
                "\nAvg Concentration Score: " + String.format("%.1f", avgConcentration);
        tvDetailedStats.setText(info);

        showChart(entries);
    }

    private void showChart(List<BarEntry> entries) {
        BarDataSet dataSet = new BarDataSet(entries, "Minutes per Day");
        dataSet.setColor(android.graphics.Color.parseColor("#FF69B4"));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // y config
        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // adds the actual date strings to the x
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.size()) {
                    return dateLabels.get(index);
                }
                return "";
            }
        });

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getDescription().setEnabled(false);

        //  tilts wont overlap
        xAxis.setLabelRotationAngle(-45);

        barChart.animateY(1000);
        barChart.invalidate();
    }
}
