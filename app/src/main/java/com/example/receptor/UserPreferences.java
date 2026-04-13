package com.example.receptor;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserPreferences {

    private static final String PREFS_NAME = "receptor_prefs";
    private static final String KEY_EXPIRY_DAYS = "expiry_warning_days";

    private UserPreferences() {
    }

    public static int getExpiryWarningDays(Context context, int fallback) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_EXPIRY_DAYS, fallback);
    }

    public static void setExpiryWarningDays(Context context, int days) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_EXPIRY_DAYS, days).apply();
    }
}
