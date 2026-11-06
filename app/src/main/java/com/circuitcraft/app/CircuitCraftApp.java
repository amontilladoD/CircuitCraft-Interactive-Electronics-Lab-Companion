package com.circuitcraft.app;

import android.app.Application;

import com.circuitcraft.app.ui.ThemeHelper;


public class CircuitCraftApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.apply(this);
    }
}
