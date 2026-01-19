package com.example.lockinapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SessionFeedbackFragment extends Fragment {

    private EditText eTFeedbackInput;
    private TextView tvFeedbackScore, tvAiFeedback;
    private Button btnSendFeedback, btnBack;

    private GeminiManager geminiManager;
    private String sessionId;
    private int sessionScore;
    private long sessionDurationMin;

    public SessionFeedbackFragment() {
        // required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inflate fragment layout
        View view = inflater.inflate(R.layout.session_feedback_fragment, container, false);

        // initialize views
        eTFeedbackInput = view.findViewById(R.id.eTFeedbackInput);
        tvFeedbackScore = view.findViewById(R.id.tvFeedbackScore);
        tvAiFeedback = view.findViewById(R.id.tvAiFeedback);
        btnSendFeedback = view.findViewById(R.id.btnSendFeedback);
        btnBack = view.findViewById(R.id.btnBack);

        geminiManager = GeminiManager.getInstance();

        // retrieve data passed from study fragment
        if (getArguments() != null) {
            sessionId = getArguments().getString("SESSION_ID");
            sessionScore = getArguments().getInt("SCORE", 0);
            sessionDurationMin = getArguments().getLong("DURATION_MIN", 0);

            tvFeedbackScore.setText(String.valueOf(sessionScore));
        }

        // setup send button logic
        btnSendFeedback.setOnClickListener(v -> generateAiFeedback());

        // back button logic
        btnBack.setOnClickListener(v -> {
            // go back to the previous fragment (StudyFragment)
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // if for some reason backstack is empty, replace manually
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new StudyFragment())
                        .commit();
            }
        });

        return view;
    }

    private void generateAiFeedback() {
        String userFeedback = eTFeedbackInput.getText().toString().trim();

        if (userFeedback.isEmpty()) {
            Toast.makeText(requireContext(), "Please tell us how it went first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // build a professional prompt for gemini
        String prompt = "The user finished a study session. " +
                "Duration: " + sessionDurationMin + " minutes. " +
                "AI Concentration Score: " + sessionScore + "/100. " +
                "User's feeling: '" + userFeedback + "'. " +
                "Provide a short, warm, motivating feedback and one tip for the next session in Hebrew.";

        tvAiFeedback.setText("Thinking...");
        btnSendFeedback.setEnabled(false);

        // call gemini manager for text prompt
        geminiManager.sendTextPrompt(prompt, new GeminiCallBack() {
            @Override
            public void onSuccess(String result) {
                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    tvAiFeedback.setText(result);
                    btnSendFeedback.setEnabled(true);
                });
            }

            @Override
            public void onFailure(Throwable error) {
                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {
                    tvAiFeedback.setText("Error getting feedback: " + error.getMessage());
                    btnSendFeedback.setEnabled(true);
                });
            }
        });
    }
}
