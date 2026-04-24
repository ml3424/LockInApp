package com.example.lockinapp.Broadcasts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * The type Timer service.
 */
public class TimerService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "timer_service_channel";
    private CountDownTimer countDownTimer;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private CameraManager cameraManager;
    private Vibrator vibrator;
    private boolean isDistracted = false;
    private String cameraId;

    /**
     * Listener for distraction events triggered by external components.
     * <p>
     * When a {@code "TRIGGER_DISTRACTION"} broadcast is received, it initiates
     * the {@link #startDistractionAlert()} flow to bring the user's focus back.
     */
    private BroadcastReceiver distractionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("TRIGGER_DISTRACTION".equals(intent.getAction())) {
                startDistractionAlert();
            }
        }
    };

    /**
     * Initializes the service and its hardware integration layer.
     * <p>
     * This method sets up the physical monitoring tools:
     * <ul>
     * <li><b>Movement:</b> Initializes the Accelerometer to detect if the phone is picked up.</li>
     * <li><b>Feedback:</b> Sets up the {@code Vibrator} and identifies the primary
     * camera ID to control the LED flash for alerts.</li>
     * <li><b>Communication:</b> Registers a local receiver to respond to
     * distraction signals without freezing the main process.</li>
     * </ul>
     */
    @Override
    public void onCreate() {
        super.onCreate();

        LocalBroadcastManager.getInstance(this).registerReceiver(distractionReceiver, new IntentFilter("TRIGGER_DISTRACTION"));
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);


        try
        {
            cameraId = cameraManager.getCameraIdList()[0]; // get the main camera flash
        }
        catch (Exception e) {
            Log.e("CameraError", "Failed to get camera ID", e);
        }

        // register the sensor listener
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Performs cleanup before the service is destroyed.
     * <p>
     * This ensures that the accelerometer listener is unregistered to save battery,
     * unregisters the broadcast receiver
     * and that any active {@code CountDownTimer} is canceled to prevent
     * memory leaks or background crashes.
     */
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(distractionReceiver);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    /**
     * Processes real-time accelerometer data to detect device movement.
     * <p>
     * This method calculates the total acceleration vector magnitude using this formula: {@code sqrt(x² + y² + z²)}.
     * If needed triggers a distraction alert.
     *
     * @param event The {@code SensorEvent} containing X, Y, and Z acceleration values.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // calculate the total acceleration force = a in physics
            double acceleration = Math.sqrt(x*x + y*y + z*z);

            if (acceleration > 13) { // 13 is a threshold for significant movement
                Log.d("Distraction", "Acceleration detected: " + acceleration);
                startDistractionAlert();
            }
        }
    }

    /**
     * Triggers a multi-sensory alert to refocus the user when a distraction is detected.
     * <p>
     * This method provides haptic feedback (vibration) and a visual cue (flashlight blink).
     * It handles different Android API levels for vibration and uses a {@code Handler}
     * to automatically reset the alert state and turn off the torch after a short delay.
     */
    private void startDistractionAlert() {
        if (isDistracted) return; // avoid multiple triggers
        isDistracted = true;

        // start vibrating
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            else
            {
                vibrator.vibrate(1000);
            }
        }

        // blink the flashlight
        try {
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, true);

                // use a handler to schedule the "turn off" action after a delay
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cameraManager.setTorchMode(cameraId, false);
                            isDistracted = false;
                        } catch (Exception e) {
                            Log.e("TimerService", "Flashlight error: Could not turn off torch", e);
                        }
                    }
                }, 500);
            }
            else isDistracted = false;
        }
        catch (Exception e) {
            Log.e("TimerService", "Error controlling flashlight", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Handles the service start request and initializes the countdown timer.
     * <p>
     * This method transitions the service to the Foreground to ensure it
     * persists even when the app is in the background. It sets up a
     * {@code CountDownTimer} that:
     * <ul>
     * <li>Updates the persistent notification every second.</li>
     * <li>Broadcasts the remaining time locally to sync with the UI.</li>
     * <li>Stops the service and notifies the system upon completion.</li>
     * </ul>
     *
     * @param intent The intent containing {@code DURATION_MIN} for the session.
     * @return {@code START_NOT_STICKY} to prevent automatic restart if killed by the system.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int minutes = intent.getIntExtra("DURATION_MIN", 0);
        long millis = (long) minutes * 60 * 1000;

        createNotiChannel();

        Notification notification = buildNotification("Your study session is starting...");
        startForeground(1, notification);

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long min = millisUntilFinished / 60000;
                long sec = (millisUntilFinished % 60000) / 1000;

                String timeLeft = String.format("%02d:%02d", min, sec);
                updateNotification("Remaining: " + timeLeft);

                Intent updateIntent = new Intent("TIMER_UPDATE");
                updateIntent.putExtra("TIME_LEFT", timeLeft);
                LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(updateIntent);
            }

            @Override
            public void onFinish() {
                updateNotification("Session finished! Well done.");
                stopForeground(false);

                LocalBroadcastManager.getInstance(TimerService.this).sendBroadcast(new Intent("TIMER_FINISHED"));
                stopSelf();
            }
        }.start();

        return START_NOT_STICKY;
    }

    /**
     * Updates the existing foreground notification with new status text.
     * <p>
     * This allows the user to track their study progress (e.g., time remaining)
     * directly from the system tray or lock screen without reopening the app.
     *
     * @param text The new message or time string to display in the notification.
     */
    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, buildNotification(text));
        }
    }

    /**
     * Constructs a persistent notification to display the current session status.
     * <p>
     * The notification is set to {@code setOngoing(true)}, preventing the user
     * from accidentally dismissing it while the study session is active.
     * This is a requirement for maintaining the service in the Foreground.
     *
     * @param text The current status msg or time remaining to display.
     * @return A configured {@code Notification} object ready for display.
     */
    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LockIn Timer")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    /**
     * Creates a notification channel for Android Oreo (API 26) and above.
     * <p>
     * This method sets the importance to {@code IMPORTANCE_LOW} to ensure
     * that periodic timer updates do not trigger intrusive sounds or
     * visual "heads-up" interruptions, maintaining a quiet study environment.
     */
    private void createNotiChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Study Timer", NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}