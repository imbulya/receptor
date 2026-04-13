package com.example.receptor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public final class RecipeStore {

    private static final String PREFS_NAME = "receptor_recipes";
    private static final String KEY_CUSTOM = "custom_recipes";
    private static final String KEY_BASE_VERSION = "base_version";
    private static final int CURRENT_BASE_VERSION = 6;
    private static final String BASE_RECIPES_ASSET = "recipes.json";
    private static final String BASE_RECIPES_FILE = "recipes_base.json";
    private static final String CUSTOM_RECIPES_FILE = "recipes_custom.json";

    private RecipeStore() {
    }

    public static List<Recipe> getAllRecipes(Context context) {
        List<Recipe> result = new ArrayList<>();
        result.addAll(loadBaseRecipes(context));
        result.addAll(loadCustomRecipes(context));
        return result;
    }

    public static List<Recipe> getCustomRecipes(Context context) {
        return loadCustomRecipes(context);
    }

    public static Recipe findRecipeByTitle(Context context, String title) {
        if (title == null) {
            return null;
        }
        String normalized = normalizeTitle(title);
        for (Recipe recipe : getAllRecipes(context)) {
            if (recipe.title != null && normalized.equals(normalizeTitle(recipe.title))) {
                return recipe;
            }
        }
        return null;
    }

    public static void addCustomRecipe(Context context, Recipe recipe) {
        if (context == null || recipe == null) {
            return;
        }
        List<Recipe> current = loadCustomRecipes(context);
        current.add(recipe);
        persist(context, current);
    }

    public static void refreshBaseRecipes(Context context) {
        if (context == null) {
            return;
        }
        try (InputStream stream = context.getAssets().open(BASE_RECIPES_ASSET)) {
            File outFile = new File(context.getFilesDir(), BASE_RECIPES_FILE);
            try (FileOutputStream outputStream = new FileOutputStream(outFile, false)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_BASE_VERSION, CURRENT_BASE_VERSION).apply();
        } catch (IOException ignored) {
        }
    }

    public static void clearCustomRecipes(Context context) {
        if (context == null) {
            return;
        }
        File file = new File(context.getFilesDir(), CUSTOM_RECIPES_FILE);
        if (file.exists()) {
            file.delete();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CUSTOM).apply();
    }

    private static List<Recipe> loadCustomRecipes(Context context) {
        List<Recipe> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        File file = new File(context.getFilesDir(), CUSTOM_RECIPES_FILE);
        if (file.exists()) {
            return readRecipesFromFile(file);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CUSTOM, null);
        if (json == null) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(parseRecipe(obj));
            }
            persist(context, result);
            prefs.edit().remove(KEY_CUSTOM).apply();
        } catch (JSONException ignored) {
        }
        return result;
    }

    private static void persist(Context context, List<Recipe> recipes) {
        if (context == null) {
            return;
        }
        writeRecipesToFile(new File(context.getFilesDir(), CUSTOM_RECIPES_FILE), recipes);
    }

    public static void persistBaseRecipes(Context context, List<Recipe> recipes) {
        if (context == null) {
            return;
        }
        writeRecipesToFile(new File(context.getFilesDir(), BASE_RECIPES_FILE), recipes);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_BASE_VERSION, CURRENT_BASE_VERSION).apply();
    }

    private static List<Recipe> loadBaseRecipes(Context context) {
        if (context == null) {
            return new ArrayList<>();
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int baseVersion = prefs.getInt(KEY_BASE_VERSION, 0);
        if (baseVersion != CURRENT_BASE_VERSION) {
            try (InputStream stream = context.getAssets().open(BASE_RECIPES_ASSET)) {
                List<Recipe> fromAsset = readRecipesFromStream(stream);
                if (!fromAsset.isEmpty()) {
                    persistBaseRecipes(context, fromAsset);
                    prefs.edit().putInt(KEY_BASE_VERSION, CURRENT_BASE_VERSION).apply();
                    return fromAsset;
                }
            } catch (IOException ignored) {
            }
        }
        File override = new File(context.getFilesDir(), BASE_RECIPES_FILE);
        if (override.exists()) {
            List<Recipe> fromFile = readRecipesFromFile(override);
            if (hasNonAssetImages(fromFile)) {
                try (InputStream stream = context.getAssets().open(BASE_RECIPES_ASSET)) {
                    List<Recipe> fromAsset = readRecipesFromStream(stream);
                    if (!fromAsset.isEmpty()) {
                        persistBaseRecipes(context, fromAsset);
                        return fromAsset;
                    }
                } catch (IOException ignored) {
                }
            }
            return fromFile;
        }
        try (InputStream stream = context.getAssets().open(BASE_RECIPES_ASSET)) {
            return readRecipesFromStream(stream);
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    private static List<Recipe> readRecipesFromFile(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return readRecipesFromStream(stream);
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    private static List<Recipe> readRecipesFromStream(InputStream stream) {
        List<Recipe> result = new ArrayList<>();
        if (stream == null) {
            return result;
        }
        try {
            String json = readJsonFromStream(stream);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    result.add(parseRecipe(obj));
                }
            }
        } catch (JSONException | IOException ignored) {
        }
        return result;
    }

    private static String readJsonFromStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        String json = builder.toString();
        return stripBom(json);
    }

    private static JSONObject toJson(Recipe recipe) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", recipe.id);
            obj.put("title", recipe.title);
            obj.put("imageUrl", recipe.imageUrl);
            obj.put("cuisine", recipe.cuisine);
            obj.put("type", recipe.type);
            obj.put("timeMinutes", recipe.timeMinutes);
            JSONArray ingredients = new JSONArray();
            for (Ingredient ingredient : recipe.ingredients) {
                JSONObject ing = new JSONObject();
                ing.put("name", ingredient.name);
                ing.put("amount", ingredient.amount);
                ingredients.put(ing);
            }
            obj.put("ingredients", ingredients);
            JSONArray steps = new JSONArray();
            for (String step : recipe.steps) {
                steps.put(step);
            }
            obj.put("steps", steps);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private static Recipe parseRecipe(JSONObject obj) {
        String id = obj.optString("id");
        String title = obj.optString("title");
        String imageUrl = obj.optString("imageUrl");
        String cuisine = obj.optString("cuisine");
        String type = obj.optString("type");
        imageUrl = ensureImageUrl(imageUrl);
        int timeMinutes = obj.optInt("timeMinutes", 20);
        List<Ingredient> ingredients = new ArrayList<>();
        JSONArray ingArray = obj.optJSONArray("ingredients");
        if (ingArray != null) {
            for (int i = 0; i < ingArray.length(); i++) {
                JSONObject ing = ingArray.optJSONObject(i);
                if (ing == null) {
                    continue;
                }
                ingredients.add(new Ingredient(
                        formatIngredientName(ing.optString("name")),
                        ing.optString("amount")
                ));
            }
        }
        List<String> steps = new ArrayList<>();
        JSONArray stepArray = obj.optJSONArray("steps");
        if (stepArray != null) {
            for (int i = 0; i < stepArray.length(); i++) {
                steps.add(stepArray.optString(i));
            }
        }
        return new Recipe(id, title, imageUrl, cuisine, type, timeMinutes, ingredients, steps);
    }

    private static String ensureImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return "";
        }
        String trimmed = imageUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("unsplash.com")) {
            return "";
        }
        if (lower.contains("picsum.photos") || lower.contains("loremflickr.com")) {
            return "";
        }
        return trimmed;
    }

    private static void writeRecipesToFile(File file, List<Recipe> recipes) {
        JSONArray array = new JSONArray();
        if (recipes != null) {
            for (Recipe recipe : recipes) {
                array.put(toJson(recipe));
            }
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream outputStream = new FileOutputStream(tmp, false)) {
            outputStream.write(array.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return;
        }
        if (file.exists() && !file.delete()) {
            return;
        }
        tmp.renameTo(file);
    }

    private static String stripBom(String json) {
        if (json == null || json.isEmpty()) {
            return "";
        }
        if (json.charAt(0) == '\uFEFF') {
            return json.substring(1);
        }
        return json;
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.trim().toLowerCase(Locale.ROOT);
    }

    public static String formatIngredientName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int firstCodePoint = trimmed.codePointAt(0);
        int firstLen = Character.charCount(firstCodePoint);
        String first = new String(Character.toChars(Character.toUpperCase(firstCodePoint)));
        if (trimmed.length() == firstLen) {
            return first;
        }
        return first + trimmed.substring(firstLen);
    }

    public static String buildAutoImageUrl(String title, String cuisine, String type) {
        return "";
    }

    private static boolean isLocalImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return false;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("file:///android_asset/")
                || lower.startsWith("android.resource://")
                || lower.startsWith("content://")
                || lower.startsWith("file://");
    }


    private static boolean hasNonAssetImages(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return true;
        }
        for (Recipe recipe : recipes) {
            if (recipe == null) {
                continue;
            }
            if (recipe.imageUrl == null || recipe.imageUrl.trim().isEmpty()) {
                return true;
            }
            if (!isLocalImageUrl(recipe.imageUrl)) {
                return true;
            }
        }
        return false;
    }

    public static final class Recipe {
        public final String id;
        public final String title;
        public String imageUrl;
        public final String cuisine;
        public final String type;
        public final int timeMinutes;
        public final List<Ingredient> ingredients;
        public final List<String> steps;
        public int matchPercent;

        public Recipe(String id, String title, String imageUrl, String cuisine, String type, int timeMinutes, List<Ingredient> ingredients, List<String> steps) {
            this.id = id;
            this.title = title;
            this.imageUrl = imageUrl;
            this.cuisine = cuisine;
            this.type = type;
            this.timeMinutes = timeMinutes;
            this.ingredients = ingredients == null ? new ArrayList<>() : ingredients;
            this.steps = steps == null ? new ArrayList<>() : steps;
            this.matchPercent = 0;
        }
    }

    public static final class Ingredient {
        public final String name;
        public final String amount;

        public Ingredient(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}

