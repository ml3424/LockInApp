package com.example.lockinapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "timer_service_channel";
    private CountDownTimer countDownTimer;

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