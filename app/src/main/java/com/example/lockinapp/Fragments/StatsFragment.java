package com.example.lockinapp.Fragments;

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

import com.example.lockinapp.R;
import com.example.lockinapp.Objects.StudySession;
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
    private List<StudySession> allSessions = new ArrayList<>();
    private List<String> dateLabels = new ArrayList<>();
    String[] subjects = {"General", "Math", "English", "History", "Computer Science", "Physics"};

    public StatsFragment() {}

    /**
     * Initializes the statistics dashboard and triggers data retrieval.
     * <p>
     * This method sets up the visual components, including the {@code BarChart}
     * for performance visualization and a {@code Spinner} for filtering by subject.
     * It identifies the current user via Firebase Auth to ensure personalized
     * data loading.
     *
     * @return The root view for the Statistics screen.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_fragment, container, false);

        barChart = view.findViewById(R.id.barChart);
        spinnerStatsSubject = view.findViewById(R.id.spinnerStatsSubject);
        tvDetailedStats = view.findViewById(R.id.tvDetailedStats);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupSpinner();
        loadDataFromFirebase();

        return view;
    }

    /**
     * Configures the subject filter dropdown and its selection logic.
     * <p>
     * Populates the {@code Spinner} with the available study subjects and
     * attaches a listener to trigger {@link #updateStats(String)} whenever
     * a new category is selected, enabling data filtering.
     */
    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, subjects);
        spinnerStatsSubject.setAdapter(adapter);

        spinnerStatsSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateStats(parent.getItemAtPosition(position).toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Synchronizes study session history from Firebase.
     * <p>
     * This method attaches a live {@code ValueEventListener} to the "StudySessions"
     * node, filtered by the current user's ID. Upon any data change, it refreshes
     * the local {@code allSessions} list and triggers a UI update via
     * {@link #updateStats(String)} to reflect the latest progress.
     */
    private void loadDataFromFirebase() {
        DatabaseReference sessionsRef = FirebaseDatabase.getInstance().getReference("StudySessions");

        // fetch sessions belonging to this user
        sessionsRef.orderByChild("userId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSessions.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    StudySession session = data.getValue(StudySession.class);
                    if (session != null) {
                        allSessions.add(session);
                    }
                }
                updateStats(spinnerStatsSubject.getSelectedItem().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Processes sessions data to generate statistics for a specific subject.
     * <p>
     * This method filters the global session list, aggregates study duration by date,
     * and calculates overall performance - total time and average concentration.
     * <p>
     * It uses a {@code TreeMap} to ensure dates are sorted chronologically before
     * transforming the results into {@code BarEntry} objects for the visual chart.
     *
     * @param selectedSubject The subject to filter by, or "General" for an overview.
     */
    private void updateStats(String selectedSubject) {
        long totalSeconds = 0;
        int totalConcentration = 0;
        int count = 0;

        // map to keeps dates sorted automatically
        java.util.Map<String, Integer> dateMap = new java.util.TreeMap<>();

        for (StudySession s : allSessions) {
            if (selectedSubject.equals("General") || s.getSubject().equals(selectedSubject)) {
                totalSeconds += s.getDurationSeconds();
                totalConcentration += s.getAiConcentrationScore();
                count++;

                // extract date only (yyyy-MM-dd)
                String date = s.getStartTime().split(" ")[0];
                float minutes = s.getDurationSeconds() / 60f;

                if (dateMap.containsKey(date)) {
                    dateMap.put(date, dateMap.get(date) + (int)minutes);
                }
                else {
                    dateMap.put(date, (int)minutes);
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        dateLabels.clear(); // clear old labels
        int index = 0;

        for (java.util.Map.Entry<String, Integer> entry : dateMap.entrySet()) {
            entries.add(new BarEntry(index++, entry.getValue()));
            dateLabels.add(entry.getKey());
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

    /**
     * Turns the processed session data into a visual BarChart.
     * <p>
     * This method configures the {@code BarChart} aesthetics and axis behavior,
     * including:
     * <ul>
     * <li><b>X-Axis:</b> Maps numerical indices back to date strings with a 45-degree
     * tilt to prevent label overlapping.</li>
     * <li><b>Styling:</b> Applies a custom green theme and disables secondary axes
     * for a cleaner look.</li>
     * <li><b>Animation:</b> Executes a 1-second vertical grow effect when the
     * data is refreshed.</li>
     * </ul>
     *
     * @param entries A list of {@code BarEntry} objects containing daily study minutes.
     */
    private void showChart(List<BarEntry> entries) {
        BarDataSet dataSet = new BarDataSet(entries, "Minutes per Day");
        dataSet.setColor(android.graphics.Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // y config
        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45); // so tilts wont overlap

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
        barChart.animateY(1000);
        barChart.invalidate();
    }
}
