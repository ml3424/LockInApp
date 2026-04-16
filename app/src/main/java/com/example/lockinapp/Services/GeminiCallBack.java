package com.example.lockinapp.Services;

public interface GeminiCallBack {
    /**
     * Called when the Gemini AI model successfully returns a text response.
     *
     * @param result The text response received from the Gemini AI model.
     */
    public void onSuccess(String result);

    /**
     * Called when an error occurs during the interaction with the Gemini AI model.
     *
     * @param error The {@link Throwable} object representing the error that occurred.
     */
    public void onFailure(Throwable error);
}
