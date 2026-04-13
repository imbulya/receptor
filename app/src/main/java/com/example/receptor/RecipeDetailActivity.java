package com.example.receptor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.HashSet;
import java.util.Set;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_recipe_title";
    public static final String EXTRA_IMAGE_URL = "extra_recipe_image_url";
    public static final String EXTRA_CUISINE = "extra_recipe_cuisine";
    public static final String EXTRA_TYPE = "extra_recipe_type";
    public static final String EXTRA_TIME = "extra_recipe_time";
    public static final String EXTRA_MATCH = "extra_recipe_match";
    public static final String EXTRA_ID = "extra_recipe_id";

    private AppRepository repository;
    private LinearLayout ingredientsContainer;
    private LinearLayout stepsContainer;
    private Button addMissingButton;
    private TextView matchView;
    private SwipeRefreshLayout swipeRefresh;
    private String recipeTitle;
    private String recipeId;
    private String fallbackImageUrl;
    private RecipeStore.Recipe currentRecipe;
    private final List<RecipeStore.Ingredient> missingIngredients = new ArrayList<>();
    private final List<AppRepository.Product> remoteProducts = new ArrayList<>();
    private ImageView imageView;
    private TextView titleView;
    private TextView cuisineView;
    private TextView typeView;
    private TextView timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail);

        repository = AppRepository.getInstance(this);
        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_recipe_detail);
        imageView = findViewById(R.id.iv_detail_image);
        titleView = findViewById(R.id.tv_detail_title);
        matchView = findViewById(R.id.tv_detail_match);
        cuisineView = findViewById(R.id.tv_detail_cuisine);
        typeView = findViewById(R.id.tv_detail_type);
        timeView = findViewById(R.id.tv_detail_time);

        ingredientsContainer = findViewById(R.id.ingredients_container);
        stepsContainer = findViewById(R.id.steps_container);
        addMissingButton = findViewById(R.id.btn_add_missing_shopping);
        swipeRefresh = findViewById(R.id.swipe_refresh_recipe_detail);

        backButton.setOnClickListener(v -> finish());
        addMissingButton.setOnClickListener(v -> addMissingToShoppingList());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String cuisine = getIntent().getStringExtra(EXTRA_CUISINE);
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        int timeMinutes = getIntent().getIntExtra(EXTRA_TIME, 20);
        int matchPercent = getIntent().getIntExtra(EXTRA_MATCH, 100);
        recipeId = getIntent().getStringExtra(EXTRA_ID);
        fallbackImageUrl = imageUrl;

        recipeTitle = title == null ? "" : title;
        currentRecipe = RecipeStore.findRecipeByTitle(this, recipeTitle);
        if (currentRecipe == null) {
            currentRecipe = new RecipeStore.Recipe(
                    "custom_" + System.currentTimeMillis(),
                    recipeTitle,
                    imageUrl,
                    cuisine == null ? "" : cuisine,
                    type == null ? "" : type,
                    timeMinutes,
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        matchView.setText(getString(R.string.recipe_match_pct, matchPercent));
        updateHeader();

        renderIngredients(currentRecipe.ingredients);
        renderSteps(currentRecipe.steps);
        updateMatchPercent();

        fetchRemoteRecipe();
        loadRemoteProducts();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                refreshRecipeDetails();
                swipeRefresh.setRefreshing(false);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecipeDetails();
        loadRemoteProducts();
    }

    private void applyInsets() {
        View root = findViewById(R.id.recipe_detail_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void refreshRecipeDetails() {
        if (isRemoteId(recipeId)) {
            fetchRemoteRecipe();
            return;
        }
        if (recipeTitle == null) {
            return;
        }
        RecipeStore.Recipe refreshed = RecipeStore.findRecipeByTitle(this, recipeTitle);
        if (refreshed != null) {
            currentRecipe = refreshed;
        }
        if (currentRecipe == null) {
            return;
        }
        renderIngredients(currentRecipe.ingredients);
        renderSteps(currentRecipe.steps);
        updateMatchPercent();
    }

    private void fetchRemoteRecipe() {
        if (!isRemoteId(recipeId)) {
            return;
        }
        DataClient.fetchRecipeDetail(recipeId, new DataClient.Callback<RecipeStore.Recipe>() {
            @Override
            public void onSuccess(RecipeStore.Recipe data) {
                if (data == null) {
                    return;
                }
                currentRecipe = data;
                recipeTitle = currentRecipe.title == null ? "" : currentRecipe.title;
                updateHeader();
                renderIngredients(currentRecipe.ingredients);
                renderSteps(currentRecipe.steps);
                updateMatchPercent();
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void updateHeader() {
        if (currentRecipe == null) {
            return;
        }
        titleView.setText(currentRecipe.title == null ? "" : currentRecipe.title);
        String cuisine = currentRecipe.cuisine == null ? "" : currentRecipe.cuisine;
        String type = currentRecipe.type == null ? "" : currentRecipe.type;
        boolean showCuisine = !cuisine.trim().isEmpty();
        boolean showType = !type.trim().isEmpty() && !type.equalsIgnoreCase(cuisine);
        cuisineView.setText(cuisine);
        cuisineView.setVisibility(showCuisine ? View.VISIBLE : View.GONE);
        typeView.setText(type);
        typeView.setVisibility(showType ? View.VISIBLE : View.GONE);
        timeView.setText(getString(R.string.recipe_time_short, currentRecipe.timeMinutes));
        String imageToLoad = currentRecipe.imageUrl == null || currentRecipe.imageUrl.trim().isEmpty()
                ? fallbackImageUrl
                : currentRecipe.imageUrl;
        int placeholder = RecipeUi.getPlaceholderResId(currentRecipe.type);
        imageView.setImageResource(placeholder);
        imageView.setTag(currentRecipe.id);
        Glide.with(this)
                .load(imageToLoad)
                .placeholder(placeholder)
                .error(placeholder)
                .centerCrop()
                .into(imageView);

        // No external image API: use provided imageUrl or placeholders only.
    }

    private void loadRemoteProducts() {
        DataClient.fetchAllProducts(new DataClient.Callback<List<AppRepository.Product>>() {
            @Override
            public void onSuccess(List<AppRepository.Product> data) {
                remoteProducts.clear();
                if (data != null) {
                    remoteProducts.addAll(data);
                }
                updateMatchPercent();
            }

            @Override
            public void onError(Exception error) {
                remoteProducts.clear();
            }
        });
    }

    private boolean isRemoteId(String id) {
        if (id == null) {
            return false;
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void updateMatchPercent() {
        if (matchView == null) {
            return;
        }
        int percent = currentRecipe == null
                ? 0
                : calculateMatchPercent(currentRecipe.ingredients);
        if (currentRecipe != null) {
            currentRecipe.matchPercent = percent;
        }
        matchView.setText(getString(R.string.recipe_match_pct, percent));
    }

    private int calculateMatchPercent(List<RecipeStore.Ingredient> ingredients) {
        List<String> availableNames = getAvailableProductNames();
        int total = ingredients.size();
        if (total == 0) {
            return 0;
        }
        int matched = 0;
        for (RecipeStore.Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.name == null) {
                continue;
            }
            String ingredientName = ingredient.name.toLowerCase(Locale.ROOT);
            boolean hasMatch = false;
            for (String productName : availableNames) {
                if (productName.contains(ingredientName) || ingredientName.contains(productName)) {
                    hasMatch = true;
                    break;
                }
            }
            if (hasMatch) {
                matched++;
            }
        }
        return Math.round((matched * 100f) / total);
    }

    private void renderIngredients(List<RecipeStore.Ingredient> ingredients) {
        ingredientsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<String> availableNames = getAvailableProductNames();
        missingIngredients.clear();
        for (RecipeStore.Ingredient ingredient : ingredients) {
            View row = inflater.inflate(R.layout.item_ingredient_row, ingredientsContainer, false);
            FrameLayout statusBg = row.findViewById(R.id.ingredient_status_bg);
            TextView statusIcon = row.findViewById(R.id.tv_ingredient_check);
            TextView statusText = row.findViewById(R.id.tv_ingredient_status);
            TextView nameView = row.findViewById(R.id.tv_ingredient_name);
            TextView amountView = row.findViewById(R.id.tv_ingredient_amount);
            nameView.setText(RecipeStore.formatIngredientName(ingredient.name));
            amountView.setText(ingredient.amount);
            boolean available = isIngredientAvailable(ingredient.name, availableNames);
            if (available) {
                statusBg.setBackgroundResource(R.drawable.bg_circle_green);
                statusIcon.setText(getString(R.string.ingredient_check_icon));
                statusIcon.setTextColor(ContextCompat.getColor(this, R.color.section_green));
                statusText.setText(getString(R.string.ingredient_in_stock));
                statusText.setTextColor(ContextCompat.getColor(this, R.color.section_green));
            } else {
                statusBg.setBackgroundResource(R.drawable.bg_circle_red);
                statusIcon.setText(getString(R.string.ingredient_cross_icon));
                statusIcon.setTextColor(ContextCompat.getColor(this, R.color.alert_red));
                statusText.setText(getString(R.string.ingredient_missing));
                statusText.setTextColor(ContextCompat.getColor(this, R.color.alert_red));
                missingIngredients.add(ingredient);
            }
            ingredientsContainer.addView(row);
        }
        updateAddMissingButton();
    }

    private void renderSteps(List<String> steps) {
        stepsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < steps.size(); i++) {
            View row = inflater.inflate(R.layout.item_step_row, stepsContainer, false);
            TextView numberView = row.findViewById(R.id.tv_step_number);
            TextView textView = row.findViewById(R.id.tv_step_text);
            numberView.setText(String.valueOf(i + 1));
            textView.setText(steps.get(i));
            stepsContainer.addView(row);
        }
    }

    private List<String> getAvailableProductNames() {
        List<String> names = new ArrayList<>();
        List<AppRepository.Product> source = remoteProducts.isEmpty()
                ? repository.getAllProducts()
                : remoteProducts;
        for (AppRepository.Product product : source) {
            if (!product.units.isEmpty()) {
                names.add(product.name.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private boolean isIngredientAvailable(String ingredientName, List<String> availableNames) {
        if (ingredientName == null) {
            return false;
        }
        String normalized = ingredientName.toLowerCase(Locale.ROOT);
        for (String productName : availableNames) {
            if (productName.contains(normalized) || normalized.contains(productName)) {
                return true;
            }
        }
        return false;
    }


    private void updateAddMissingButton() {
        if (addMissingButton == null) {
            return;
        }
        boolean hasMissing = !missingIngredients.isEmpty();
        addMissingButton.setEnabled(hasMissing);
        addMissingButton.setAlpha(hasMissing ? 1f : 0.5f);
    }

    private void addMissingToShoppingList() {
        if (missingIngredients.isEmpty()) {
            Toast.makeText(this, getString(R.string.shopping_missing_none), Toast.LENGTH_SHORT).show();
            return;
        }
        DataClient.fetchShoppingItems(new DataClient.Callback<List<DataClient.ShoppingItem>>() {
            @Override
            public void onSuccess(List<DataClient.ShoppingItem> data) {
                Set<String> existing = new HashSet<>();
                if (data != null) {
                    for (DataClient.ShoppingItem item : data) {
                        if (item.name != null) {
                            existing.add(item.name.toLowerCase(Locale.ROOT));
                        }
                    }
                }
                int added = 0;
                for (RecipeStore.Ingredient ingredient : missingIngredients) {
                    if (ingredient.name == null || ingredient.name.trim().isEmpty()) {
                        continue;
                    }
                    String key = ingredient.name.toLowerCase(Locale.ROOT);
                    if (existing.contains(key)) {
                        continue;
                    }
                    existing.add(key);
                    added++;
                    String qty = getString(R.string.shopping_qty_default);
                    DataClient.addShoppingItem(ingredient.name, qty, new DataClient.Callback<DataClient.ShoppingItem>() {
                        @Override
                        public void onSuccess(DataClient.ShoppingItem data) {
                        }

                        @Override
                        public void onError(Exception error) {
                        }
                    });
                }
                if (added > 0) {
                    Toast.makeText(RecipeDetailActivity.this, getString(R.string.shopping_missing_added), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RecipeDetailActivity.this, getString(R.string.shopping_missing_already), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(RecipeDetailActivity.this, getString(R.string.shopping_missing_already), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

