package com.circuitcraft.app.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Dark mode: follows the phone until you tap the sun / moon icon; then your choice is saved. */
public class ThemeHelper {
    private static final String PREFS = "settings";
    private static final String KEY = "night_mode";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Called once when the app process starts. */
    public static void apply(Context c) {
        AppCompatDelegate.setDefaultNightMode(
                prefs(c).getInt(KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    /** Save and apply light / dark; AppCompat re-creates the visible screen with the new theme. */
    public static void setNight(Context c, boolean night) {
        int mode = night ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        prefs(c).edit().putInt(KEY, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
