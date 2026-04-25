package com.example.lockinapp.Fragments;

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

import com.example.lockinapp.R;
import com.example.lockinapp.Utils.GeminiCallBack;
import com.example.lockinapp.Utils.GeminiManager;

/**
 * The type Session feedback fragment.
 */
public class SessionFeedbackFragment extends Fragment {

    private EditText eTFeedbackInput;
    private TextView tvFeedbackScore, tvAiFeedback;
    private Button btnSendFeedback, btnBack;

    private GeminiManager geminiManager;
    private int sessionScore;
    private long sessionDurationMin;

    /**
     * Instantiates a new Session feedback fragment.
     */
    public SessionFeedbackFragment() {}

    /**
     * Initializes the feedback UI and retrieves session data from the last session.
     * <p>
     * Inflates the layout, binds UI components, and extracts the {@code sessionScore}
     * and {@code sessionId} passed from the study session.
     *
     * @return The root view for the Session Feedback screen.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.session_feedback_fragment, container, false);

        eTFeedbackInput = view.findViewById(R.id.eTFeedbackInput);
        tvFeedbackScore = view.findViewById(R.id.tvFeedbackScore);
        tvAiFeedback = view.findViewById(R.id.tvAiFeedback);
        btnSendFeedback = view.findViewById(R.id.btnSendFeedback);
        btnBack = view.findViewById(R.id.btnBack);

        geminiManager = GeminiManager.getInstance();

        // retrieve data passed from study fragment
        if (getArguments() != null) {
            sessionScore = getArguments().getInt("SCORE", 0);
            sessionDurationMin = getArguments().getInt("DURATION_MIN", 0);

            if (sessionScore == -1) {
                tvFeedbackScore.setText("--");
            }
            else {
                tvFeedbackScore.setText(String.valueOf(sessionScore));
            }
        }
        setupBtns();
        return view;
    }

    /**
     * Configures click listeners for the feedback and navigation buttons.
     * <p>
     * The "Send" button triggers AI-generated insights based on the user's input,
     * while the "Back" button safely manages the fragment backstack or
     * redirects to the {@code StudyFragment} as a fallback.
     */
    private void setupBtns()
    {
        btnSendFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateAiFeedback();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    // remove the current fragment from the stack to return to the previous state
                    getParentFragmentManager().popBackStack();
                }
                else {
                    // prevent a stuck ui if the backstack is unexpectedly empty
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new StudyFragment())
                            .commit();
                }
            }
        });
    }

    /**
     * Generates personalized, AI-driven study insights based on session performance and user input.
     * <p>
     * This method puts the duration, AI-calculated concentration score, and the user's
     * self-reported feelings into a structured prompt for the Gemini API.
     * <p>
     * It handles the response by updating the UI on the Main Thread and
     * provides a motivating,summary with tips for improvement.
     */
    private void generateAiFeedback() {
        String userFeedback = eTFeedbackInput.getText().toString().trim();

        if (userFeedback.isEmpty()) {
            Toast.makeText(requireContext(), "Please tell us how it went first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String aiScoreText;
        if (sessionScore == -1) {
            aiScoreText = "Note: No AI concentration data is available for this session (error). Focus only on duration and user feelings. ";
        } else {
            aiScoreText = "AI Concentration Score: " + sessionScore + "/100. ";
        }

        String prompt = "The user finished a study session, be sure to acknowledge the user's feeling. " +
                "Duration: " + sessionDurationMin + " minutes. " +
                aiScoreText +
                "User's feeling: '" + userFeedback + "'. " +
                "Provide a short, warm, motivating feedback and one tip for the next session in Hebrew. no more than 4 sentences.";

        tvAiFeedback.setText("Thinking...");
        btnSendFeedback.setEnabled(false);

        geminiManager.sendTextPrompt(prompt, new GeminiCallBack() {
            @Override
            public void onSuccess(final String result) {
                if (!isAdded() || getActivity() == null) return;

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvAiFeedback.setText(result);
                        btnSendFeedback.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable error) {
                if (getActivity() == null) return;

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvAiFeedback.setText("Error getting feedback: " + error.getMessage());
                        btnSendFeedback.setEnabled(true);
                    }
                });
            }
        });
    }
}
