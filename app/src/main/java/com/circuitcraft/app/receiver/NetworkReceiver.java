package com.circuitcraft.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkReceiver extends BroadcastReceiver {

    public interface Listener {
        void onNetworkChanged(boolean online);
    }

    private final Listener listener;

    public NetworkReceiver(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        listener.onNetworkChanged(isOnline(context));
    }

    @SuppressWarnings("deprecation")
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
