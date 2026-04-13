package com.example.receptor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

public final class Ui {

    private Ui() {
    }

    public static int dp(Context context, float value) {
        if (context == null) {
            return 0;
        }
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    public static int color(Context context, int colorRes) {
        if (context == null) {
            return 0;
        }
        return ResourcesCompat.getColor(context.getResources(), colorRes, context.getTheme());
    }

    public static int parseColorSafe(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static GradientDrawable roundedRect(Context context, int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setColor(fillColor);
        return drawable;
    }

    public static GradientDrawable roundedRect(Context context, String fillColor, int radiusDp, int fallbackColor) {
        return roundedRect(context, parseColorSafe(fillColor, fallbackColor), radiusDp);
    }
}

