package com.circuitcraft.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

public class QuizTimerService extends Service {
    private static final String TAG = "QuizTimerService";
    public static final String ACTION_TICK = "com.circuitcraft.app.ACTION_TICK";
    public static final String EXTRA_SECONDS = "seconds";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int seconds = 0;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            seconds++;
            Intent tick = new Intent(ACTION_TICK);
            tick.setPackage(getPackageName());       
            tick.putExtra(EXTRA_SECONDS, seconds);
            sendBroadcast(tick);
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        seconds = 0;
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 1000);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;   
    }
}
