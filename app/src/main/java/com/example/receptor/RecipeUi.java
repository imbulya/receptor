package com.example.receptor;

import java.util.Locale;

public final class RecipeUi {

    private RecipeUi() {
    }

    public static int getPlaceholderResId(String type) {
        if (type == null) {
            return R.drawable.recipe_placeholder_main;
        }
        String value = type.trim().toLowerCase(Locale.ROOT);
        if (value.contains("\u0437\u0430\u0432\u0442\u0440\u0430\u043a")) {
            return R.drawable.recipe_placeholder_breakfast;
        }
        if (value.contains("\u0441\u0443\u043f")) {
            return R.drawable.recipe_placeholder_soup;
        }
        if (value.contains("\u0432\u044b\u043f\u0435\u0447")) {
            return R.drawable.recipe_placeholder_baking;
        }
        if (value.contains("\u0434\u0435\u0441\u0435\u0440\u0442")) {
            return R.drawable.recipe_placeholder_dessert;
        }
        return R.drawable.recipe_placeholder_main;
    }

    public static boolean isPlaceholderUrl(String url) {
        if (url == null) {
            return true;
        }
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return true;
        }
        return trimmed.contains("unsplash.com")
                || trimmed.contains("picsum.photos");
    }
}

