package com.example.lockinapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

public class TimerService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "timer_service_channel";
    private CountDownTimer countDownTimer;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private CameraManager cameraManager;
    private Vibrator vibrator;
    private boolean isDistracted = false;
    private String cameraId;


    @Override
    public void onCreate() {
        super.onCreate();
        // initialize sensors and hardware
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try
        {
            cameraId = cameraManager.getCameraIdList()[0]; // get the main camera flash
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // register the sensor listener
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // calculate the total acceleration force = a in physics
            double acceleration = Math.sqrt(x*x + y*y + z*z);

            // if phone is moving significantly
            if (acceleration > 15) { // 15 is a threshold for significant movement
                Log.d("Distraction", "Acceleration detected: " + acceleration);
                startDistractionAlert();
            }
        }
    }

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
            cameraManager.setTorchMode(cameraId, true);

            // use a handler to schedule the "turn off" action after a delay
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraManager.setTorchMode(cameraId, false);
                        isDistracted = false;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int minutes = intent.getIntExtra("DURATION_MIN", 0);
        long millis = (long) minutes * 60 * 1000;

        createNotificationChannel();

        // build initial notification to satisfy foreground requirements
        Notification notification = buildNotification("Your study session is starting...");
        startForeground(1, notification);

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long m = millisUntilFinished / 60000;
                long s = (millisUntilFinished % 60000) / 1000;
                updateNotification("Remaining: " + String.format("%02d:%02d", m, s));
                String timeLeft = String.format("%02d:%02d", m, s);

                // broadcast to activity
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

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(1, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LockIn Timer")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
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
    public void onDestroy() {
        sensorManager.unregisterListener(this);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}