package com.circuitcraft.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;


public class SystemEventReceiver extends BroadcastReceiver {

    public interface Listener {
        void onBatteryLow();
        void onBatteryOkay();
        void onAirplaneMode(boolean on);
    }


    public abstract static class Simple implements Listener {
        @Override public void onBatteryLow() { }
        @Override public void onBatteryOkay() { }
        @Override public void onAirplaneMode(boolean on) { }
    }

    private final Listener listener;

    public SystemEventReceiver(Listener listener) {
        this.listener = listener;
    }

    public static IntentFilter filter() {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_BATTERY_LOW);
        f.addAction(Intent.ACTION_BATTERY_OKAY);
        f.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        return f;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case Intent.ACTION_BATTERY_LOW:
                listener.onBatteryLow();
                break;
            case Intent.ACTION_BATTERY_OKAY:
                listener.onBatteryOkay();
                break;
            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                listener.onAirplaneMode(intent.getBooleanExtra("state", false));
                break;
            default:
                break;
        }
    }

    public static boolean isAirplaneOn(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }


    public static boolean isBatteryLow(Context c) {
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return false;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        return level >= 0 && scale > 0 && !charging && (level * 100f / scale) <= 15f;
    }
}
