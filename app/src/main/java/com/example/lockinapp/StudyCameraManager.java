package com.example.lockinapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class StudyCameraManager {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;

    private ImageCapture imageCapture;
    private final List<Integer> concentrationScores;

    private final Handler randomCaptureHandler;
    private Runnable randomCaptureRunnable;
    private final Random random;
    private boolean isRunning = false;

    public StudyCameraManager(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;

        this.concentrationScores = new ArrayList<>();
        this.randomCaptureHandler = new Handler(Looper.getMainLooper());
        this.random = new Random();
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                // Select Front Camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
                } catch (Exception exc) {
                    Log.e("CameraManager", "Use case binding failed", exc);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraManager", "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void startRandomCaptures() {
        if (isRunning) return;
        isRunning = true;
        concentrationScores.clear();

        randomCaptureRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    takePictureAndAnalyze();
                    // Schedule next capture (random between 30 sec to 3 min)
                    // For testing, you can reduce this (e.g., 10000 + random(20000))
                    long nextDelay = 30000 + random.nextInt(150000);
                    randomCaptureHandler.postDelayed(this, nextDelay);
                }
            }
        };
        // First capture after 10 seconds
        randomCaptureHandler.postDelayed(randomCaptureRunnable, 10000);
    }

    public void stopRandomCaptures() {
        isRunning = false;
        if (randomCaptureRunnable != null) {
            randomCaptureHandler.removeCallbacks(randomCaptureRunnable);
        }
    }

    public int getAverageScore() {
        if (concentrationScores.isEmpty()) return 0;
        int sum = 0;
        for (int score : concentrationScores) {
            sum += score;
        }
        return sum / concentrationScores.size();
    }

    private void takePictureAndAnalyze() {
        if (imageCapture == null) return;

        imageCapture.takePicture(ContextCompat.getMainExecutor(context),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraManager", "Photo capture failed: " + exception.getMessage());
                    }

                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        if (bitmap != null) {
                            sendToGemini(bitmap);
                        }
                    }
                });
    }

    private void sendToGemini(Bitmap bitmap) {
        String prompt = "Analyze the person's face. Estimate concentration level (0-100). Return ONLY the number.";

        GeminiManager.getInstance().sendTextWithPhotoPrompt(prompt, bitmap, new GeminiCallBack() {
            @Override
            public void onSuccess(String result) {
                try {
                    String cleanResult = result.replaceAll("[^0-9]", "");
                    if (!cleanResult.isEmpty()) {
                        int score = Integer.parseInt(cleanResult);
                        concentrationScores.add(score);
                        Log.d("CameraManager", "Added AI Score: " + score);
                    }
                } catch (Exception e) {
                    Log.e("CameraManager", "Failed to parse score", e);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                Log.e("CameraManager", "Gemini failed", error);
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // Fix rotation for front camera (mirror + rotate)
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        matrix.postScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}