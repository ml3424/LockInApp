package com.example.lockinapp.Services;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.lockinapp.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.ImagePart;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import java.util.ArrayList;
import java.util.List;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class GeminiManager {
    private static GeminiManager instance;
    private GenerativeModel gemini;

    /**
     * Singleton manager for handling communication with the Gemini AI model.
     * <p>
     * This class encapsulates the {@code GenerativeModel} and ensures a single
     * entry point for AI requests across the application.
     */
    private GeminiManager() {
        gemini = new GenerativeModel(
                "gemini-2.5-flash-lite",
                BuildConfig.GEMINI_API
        );
    }

    /**
     * Provides access to the single instance of the GeminiManager.
     * @return The active {@code GeminiManager} instance.
     */
    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }


    /**
     * Sends a study prompt to the Gemini model.
     * <p>
     * This method handles the context required by the
     * Google AI SDK and translates the result into a simplified {@link GeminiCallBack}.
     *
     * @param prompt The instructions or data to be processed by the AI.
     * @param callback Interface to handle the success or failure of the generation.
     */
    public void sendTextPrompt(String prompt, GeminiCallBack callback) {
        gemini.generateContent(prompt,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i("GeminiManager", "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        }
                        else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }


    /**
     * Sends both a message and a photo to Gemini in the background.
     * <p>
     * This method combines text and image data into a single request.
     * It allows the AI to "see" the user's study environment and provide
     * feedback based on visual cues without freezing the app's UI.
     *
     * @param prompt   The text instructions for the AI.
     * @param photo    The image captured from the camera.
     * @param callback Interface to handle the AI's response or any errors.
     */
    public void sendTextWithPhotoPrompt(String prompt, Bitmap photo, GeminiCallBack callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(prompt));
        parts.add(new ImagePart(photo));

        Content[] content = new Content[1];
        content[0] = new Content(parts);

        gemini.generateContent(content,
                new Continuation<GenerateContentResponse>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof Result.Failure) {
                            Log.i("GeminiManager", "Error: " + ((Result.Failure) result).exception.getMessage());
                            callback.onFailure(((Result.Failure) result).exception);
                        }
                        else {
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }
}