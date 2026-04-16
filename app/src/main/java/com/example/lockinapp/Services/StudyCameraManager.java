package com.example.lockinapp.Services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudyCameraManager {

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView previewView;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private final List<Integer> concentrationScores;

    private final Handler randomCaptureHandler;
    private Runnable randomCaptureRunnable;
    private final Random random;
    private boolean isRunning = false;

    /**
     * Initializes the Camera Manager with required Android and Lifecycle components.
     * <p>
     * Sets up the context, lifecycle binding, and preview surface, while
     * initializing data structures for focus scoring and random capture scheduling.
     *
     * @param context The activity context.
     * @param lifecycleOwner The lifecycle owner (Fragment) to bind camera sessions.
     * @param previewView The UI component used to host the camera's surface provider.
     */
    public StudyCameraManager(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;

        this.concentrationScores = new ArrayList<>();
        this.randomCaptureHandler = new Handler(Looper.getMainLooper());
        this.random = new Random();
    }

    /**
     * Configures and binds the CameraX lifecycle to the application.
     * <p>
     * This method utilizes a {@code Preview}.
     * Maintaining an active Preview stream provides:
     * <p>
     * Low Latency: Keeps the hardware(camera) "warm" to eliminate opening it to every pic.
     * Image Quality: Enables continuous Auto-Focus and Exposure for clear Gemini analysis.
     * Stability: Provides the required Surface(view that works directly with the hardware) for driver stability using {@code PreviewView}.
     */
    public void startCamera() {
        // get an instance of the camera provider to bind the camera lifecycle
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        // add a listener to wait for the camera provider to be ready
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();

                    // preview use case to display the camera feed
                    Preview preview = new Preview.Builder().build();
                    // link the preview to the previewView in the layout
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());

                    imageCapture = new ImageCapture.Builder().build();
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                    try {
                        cameraProvider.unbindAll();
                        // bind the camera to the lifecycle of the fragment
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);
                    }
                    catch (Exception exc) {
                        Log.e("CameraManager", "Use case binding failed", exc);
                    }

                }
                catch (ExecutionException | InterruptedException e) {
                    Log.e("CameraManager", "Camera provider failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(context)); // ensure this runs on the main thread for UI safety
    }

    /**
     * Starts the automated focus monitoring cycle.
     * <p>
     * This method initiates a recursive {@code Handler} loop that triggers image
     * capture and analysis at randomized intervals (between 30 seconds and 3 minutes).
     * <p>
     * The first capture is delayed by 10 seconds to allow the camera hardware and
     * the user's focus to stabilize.
     */
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
                    long nextDelay = 30000 + random.nextInt(150000);
                    randomCaptureHandler.postDelayed(this, nextDelay);
                }
            }
        };
        // First capture after 15 seconds
        randomCaptureHandler.postDelayed(randomCaptureRunnable, 15000);
    }

    /**
     * Permanently halts the focus monitoring cycle and cleans up resources.
     * <p>
     * This method sets the {@code isRunning} flag to false to prevent further cycles
     * and calls {@link #pauseCaptures()} to remove any pending tasks from the queue.
     */
    public void stopRandomCaptures() {
        isRunning = false;
        pauseCaptures();
    }

    /**
     * Temporarily suspends automated captures by clearing the execution queue.
     * <p>
     * Removes the {@code randomCaptureRunnable} from the {@code Handler}.
     * This is used both for temporary backgrounding (Pause) and as part of a
     * complete shutdown (Stop).
     */
    public void pauseCaptures() {
        if (randomCaptureRunnable != null) {
            randomCaptureHandler.removeCallbacks(randomCaptureRunnable);
        }
    }

    /**
     * Restarts the monitoring cycle after a pause, if the session is still active.
     * <p>
     * Re-schedules the capture loop with a brief 5-second buffer to allow
     * the camera hardware to stabilize after returning to the foreground.
     */
    public void resumeCaptures() {
        if (isRunning && randomCaptureRunnable != null) {
            randomCaptureHandler.postDelayed(randomCaptureRunnable, 5000);
        }
    }

    /**
     * Calculates the aiScore using concentration scores collected during the session.
     * <p>
     * This value represents the user's overall focus level based on Gemini's AI analysis
     * of random captures. If no scores were recorded, it returns a default of 0.
     *
     * @return The average concentration score as an integer (0-100).
     */
    public int getAverageScore() {
        if (concentrationScores.isEmpty()) return 0;
        int sum = 0;
        for (int score : concentrationScores) {
            sum += score;
        }
        return sum / concentrationScores.size();
    }

    /**
     * Captures a single frame from the camera and initiates the AI analysis pipeline.
     * <p>
     * This method triggers the {@code ImageCapture} use case. Upon a successful
     * capture, the resulting {@code ImageProxy} is converted into a {@code Bitmap},
     * the memory is immediately released, and the image is sent to Gemini.
     */
    private void takePictureAndAnalyze() {
        if (imageCapture == null) return;

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
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

    /**
     * Sends a captured frame to the Gemini for focus analysis.
     * <p>
     * This method converts the visual data into a concentration score (0-100).
     * If the AI returns 101, it indicates that no face was detected, triggering
     * a user notification. Valid scores are stored in {@code concentrationScores}
     * for final session averaging.
     *
     * @param bitmap The processed image frame to be analyzed.
     */
    private void sendToGemini(Bitmap bitmap) {
        String prompt = "Analyze the person's face. Estimate concentration level (0-100). Return ONLY the number. important: if the face is not shown return 101";

        GeminiManager.getInstance().sendTextWithPhotoPrompt(prompt, bitmap, new GeminiCallBack() {
            @Override
            public void onSuccess(String result) {
                try {
                    String cleanResult = result.replaceAll("[^0-9]", "");
                    if (!cleanResult.isEmpty()) {
                        int score = Integer.parseInt(cleanResult);

                        if (score != 101) {
                            concentrationScores.add(score);
                            Log.d("CameraManager", "Added AI Score: " + score);
                        }
                        else
                        {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Please make sure your face is shown!", Toast.LENGTH_LONG).show();
                                    Log.d("CameraManager", "Face was not shown ");
                                }
                            });
                        }
                    }
                }
                catch (Exception e) {
                    Log.e("CameraManager", "Failed to parse score", e);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                Log.e("CameraManager", "Gemini failed", error);
            }
        });
    }

    /**
     * Converts an {@code ImageProxy} from CameraX into a processed {@code Bitmap}.
     * <p>
     * This helper method extracts the byte buffer, decodes it, and applies
     * necessary transformations including:
     * <p>
     * <li><b>Rotation:</b> Aligns the image based on the device's orientation.</li>
     * <li><b>Mirroring:</b> Flips the image horizontally to compensate for the
     * front camera's default mirror effect, ensuring natural analysis.</li>
     *
     * @param image The raw image proxy from the camera capture.
     * @return A correctly oriented and flipped Bitmap, or null if decoding fails.
     */
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Matrix matrix = new Matrix();
        // fix rotation
        matrix.postRotate(image.getImageInfo().getRotationDegrees());
        // flip horizontally (-1 on X-axis) to correct front camera mirror effect
        matrix.postScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}