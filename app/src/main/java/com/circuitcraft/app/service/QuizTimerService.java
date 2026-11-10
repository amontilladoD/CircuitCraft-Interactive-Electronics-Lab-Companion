package com.circuitcraft.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.circuitcraft.app.R;
import com.circuitcraft.app.ui.QuizActivity;

import java.util.Locale;


public class QuizTimerService extends Service {
    private static final String TAG = "QuizTimerService";
    public static final String ACTION_TICK = "com.circuitcraft.app.ACTION_TICK";
    public static final String EXTRA_SECONDS = "seconds";
    private static final String CHANNEL_ID = "quiz_timer";
    private static final int NOTIFICATION_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private NotificationManager notificationManager;
    private int seconds = 0;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            seconds++;
            Intent tick = new Intent(ACTION_TICK);
            tick.setPackage(getPackageName());           // keep the broadcast inside our app
            tick.putExtra(EXTRA_SECONDS, seconds);
            sendBroadcast(tick);
            notificationManager.notify(NOTIFICATION_ID, buildNotification(seconds));   // update the tray
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Quiz timer", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows the running time of your quiz");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        seconds = 0;
        startForeground(NOTIFICATION_ID, buildNotification(0));   // must be called within 5 s of start
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 1000);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(int sec) {
        Intent open = new Intent(this, QuizActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("CircuitCraft quiz in progress")
                .setContentText(String.format(Locale.US, "Elapsed time %02d:%02d - tap to return", sec / 60, sec % 60))
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        stopForeground(true);                 // remove the notification
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;   // started service, not bound
    }
}
