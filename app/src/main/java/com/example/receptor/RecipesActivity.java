package com.example.receptor;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipesActivity extends AppCompatActivity {

    private LinearLayout recipesContainer;
    private LinearLayout filtersBlock;
    private LinearLayout dishChipsContainer;
    private AppRepository repository;
    private LinearLayout onlyMyProducts;
    private TextView onlyMyProductsText;
    private final List<RecipeStore.Recipe> allRecipes = new ArrayList<>();
    private final Map<String, List<TextView>> chipGroups = new HashMap<>();
    private final List<AppRepository.Product> remoteProducts = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private View searchContainer;
    private EditText searchInput;
    private View loadingPlaceholder;
    private boolean isLoading;
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipes);

        repository = AppRepository.getInstance(this);
        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_recipes);
        ImageButton filterButton = findViewById(R.id.btn_filter_recipes);
        ImageButton searchButton = findViewById(R.id.btn_search_recipes);
        ImageButton addButton = findViewById(R.id.btn_add_recipe);
        onlyMyProducts = findViewById(R.id.btn_only_my_products);
        onlyMyProductsText = findViewById(R.id.tv_only_my_products);
        recipesContainer = findViewById(R.id.recipes_container);
        filtersBlock = findViewById(R.id.filters_block);
        dishChipsContainer = findViewById(R.id.dish_chips_container);
        swipeRefresh = findViewById(R.id.swipe_refresh_recipes);
        searchContainer = findViewById(R.id.recipes_search_container);
        searchInput = findViewById(R.id.et_recipe_search);
        loadingPlaceholder = findViewById(R.id.recipes_loading);
        TextView resetFiltersButton = findViewById(R.id.btn_reset_filters);

        backButton.setOnClickListener(v -> finish());
        filterButton.setOnClickListener(v -> toggleFilters());
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> toggleSearch());
        }
        addButton.setOnClickListener(v -> startActivity(new Intent(this, NewRecipeActivity.class)));
        onlyMyProducts.setOnClickListener(v -> toggleOnlyMyProducts(onlyMyProducts, onlyMyProductsText));
        if (resetFiltersButton != null) {
            resetFiltersButton.setOnClickListener(v -> resetFilters());
        }

        loadRemoteRecipes();

        if (searchInput != null) {
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            });
        }

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadRemoteRecipes();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRemoteRecipes();
    }

    private void loadRemoteRecipes() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        setLoading(true);
        DataClient.fetchRecipes(new DataClient.Callback<List<RecipeStore.Recipe>>() {
            @Override
            public void onSuccess(List<RecipeStore.Recipe> data) {
                allRecipes.clear();
                if (data != null) {
                    allRecipes.addAll(data);
                }
                renderDishChips(allRecipes);
                setupChips(filtersBlock);
                refreshRecipes();
                setLoading(false);
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onError(Exception error) {
                allRecipes.clear();
                renderDishChips(allRecipes);
                setupChips(filtersBlock);
                refreshRecipes();
                setLoading(false);
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                android.widget.Toast.makeText(RecipesActivity.this,
                        R.string.recipes_load_error,
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        loadRemoteProducts();
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        boolean show = loading && allRecipes.isEmpty();
        if (loadingPlaceholder != null) {
            loadingPlaceholder.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recipesContainer != null) {
            recipesContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void loadRemoteProducts() {
        DataClient.fetchAllProducts(new DataClient.Callback<List<AppRepository.Product>>() {
            @Override
            public void onSuccess(List<AppRepository.Product> data) {
                remoteProducts.clear();
                if (data != null) {
                    remoteProducts.addAll(data);
                }
                refreshRecipes();
            }

            @Override
            public void onError(Exception error) {
                remoteProducts.clear();
            }
        });
    }

    private void applyInsets() {
        View root = findViewById(R.id.recipes_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void toggleFilters() {
        if (filtersBlock.getVisibility() == View.VISIBLE) {
            filtersBlock.setVisibility(View.GONE);
        } else {
            filtersBlock.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSearch() {
        if (searchContainer == null || searchInput == null) {
            return;
        }
        if (searchContainer.getVisibility() == View.VISIBLE) {
            searchInput.setText("");
            searchContainer.setVisibility(View.GONE);
        } else {
            searchContainer.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        }
    }

    private void toggleOnlyMyProducts(LinearLayout container, TextView label) {
        boolean selected = !container.isSelected();
        container.setSelected(selected);
        label.setTextColor(getColor(selected ? R.color.section_orange : R.color.text_secondary));
        applyFilters();
    }

    private void resetFilters() {
        for (List<TextView> groupChips : chipGroups.values()) {
            if (groupChips.isEmpty()) {
                continue;
            }
            TextView first = groupChips.get(0);
            selectChip(groupChips, first);
        }
        if (onlyMyProducts != null) {
            onlyMyProducts.setSelected(false);
        }
        if (onlyMyProductsText != null) {
            onlyMyProductsText.setTextColor(getColor(R.color.text_secondary));
        }
        applyFilters();
    }

    private void renderRecipes(List<RecipeStore.Recipe> recipes) {
        recipesContainer.removeAllViews();
        for (RecipeStore.Recipe recipe : recipes) {
            recipesContainer.addView(createRecipeCard(recipe));
        }
    }

    private void setupChips(View root) {
        List<TextView> chips = new ArrayList<>();
        collectChips(root, chips);

        chipGroups.clear();
        for (TextView chip : chips) {
            Object tag = chip.getTag();
            if (tag == null) {
                continue;
            }
            String group = tag.toString();
            chipGroups.computeIfAbsent(group, key -> new ArrayList<>()).add(chip);
        }

        for (List<TextView> groupChips : chipGroups.values()) {
            boolean hasSelected = false;
            for (TextView chip : groupChips) {
                if (chip.isSelected()) {
                    hasSelected = true;
                    break;
                }
            }
            if (!hasSelected && !groupChips.isEmpty()) {
                groupChips.get(0).setSelected(true);
            }
            for (TextView chip : groupChips) {
                chip.setOnClickListener(v -> {
                    selectChip(groupChips, chip);
                    applyFilters();
                });
                updateChipVisual(chip);
            }
        }
        applyFilters();
    }

    private void renderDishChips(List<RecipeStore.Recipe> recipes) {
        if (dishChipsContainer == null) {
            return;
        }
        dishChipsContainer.removeAllViews();

        List<String> types = new ArrayList<>();
        for (RecipeStore.Recipe recipe : recipes) {
            if (recipe == null || recipe.type == null) {
                continue;
            }
            String type = recipe.type.trim();
            if (!type.isEmpty() && !types.contains(type)) {
                types.add(type);
            }
        }
        types.sort(String::compareToIgnoreCase);

        List<String> allChips = new ArrayList<>();
        allChips.add(getString(R.string.recipes_chip_all));
        allChips.addAll(types);

        LinearLayout currentRow = null;
        int indexInRow = 0;
        for (String label : allChips) {
            if (currentRow == null || indexInRow >= 3) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                if (dishChipsContainer.getChildCount() > 0) {
                    rowParams.topMargin = Ui.dp(this, 8);
                }
                currentRow.setLayoutParams(rowParams);
                dishChipsContainer.addView(currentRow);
                indexInRow = 0;
            }
            TextView chip = createChip(label, "dish", indexInRow > 0);
            currentRow.addView(chip);
            indexInRow++;
        }
    }

    private TextView createChip(String label, String tag, boolean withMargin) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTag(tag);
        chip.setBackgroundResource(R.drawable.bg_chip_selector);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setTextColor(getColor(R.color.text_primary));
        chip.setTextSize(14);
        chip.setMaxLines(1);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        if (withMargin) {
            params.leftMargin = Ui.dp(this, 8);
        }
        chip.setLayoutParams(params);
        return chip;
    }

    private void applyFilters() {
        String dish = getSelectedChipText("dish");
        String time = getSelectedChipText("time");
        boolean onlyMy = onlyMyProducts != null && onlyMyProducts.isSelected();
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        String allLabel = getString(R.string.recipes_chip_all);
        String anyLabel = getString(R.string.recipes_chip_any);

        List<RecipeStore.Recipe> filtered = new ArrayList<>();
        for (RecipeStore.Recipe recipe : allRecipes) {
            if (onlyMy && recipe.matchPercent < 60) {
                continue;
            }
            if (dish != null && !dish.equals(allLabel) && !dish.equalsIgnoreCase(recipe.type)) {
                continue;
            }
            if (time != null && !time.equals(anyLabel)) {
                int limit = parseTimeLimit(time);
                if (limit > 0 && recipe.timeMinutes > limit) {
                    continue;
                }
            }
            if (!query.isEmpty() && (recipe.title == null
                    || !recipe.title.toLowerCase(Locale.ROOT).contains(query))) {
                continue;
            }
            filtered.add(recipe);
        }
        boolean defaultFilters = !onlyMy
                && (dish == null || dish.equals(allLabel))
                && (time == null || time.equals(anyLabel))
                && query.isEmpty();
        if (defaultFilters) {
            filtered.sort((a, b) -> Integer.compare(b.matchPercent, a.matchPercent));
        }
        renderRecipes(filtered);
    }

    private void refreshRecipes() {
        for (RecipeStore.Recipe recipe : allRecipes) {
            recipe.matchPercent = calculateMatchPercent(recipe.ingredients);
        }
        applyFilters();
    }

    private int parseTimeLimit(String label) {
        if (label == null) {
            return 0;
        }
        if (label.equals(getString(R.string.recipes_chip_15))) {
            return 15;
        }
        if (label.equals(getString(R.string.recipes_chip_30))) {
            return 30;
        }
        if (label.equals(getString(R.string.recipes_chip_60))) {
            return 60;
        }
        return 0;
    }

    private String getSelectedChipText(String group) {
        List<TextView> groupChips = chipGroups.get(group);
        if (groupChips == null) {
            return null;
        }
        for (TextView chip : groupChips) {
            if (chip.isSelected()) {
                return chip.getText() == null ? null : chip.getText().toString();
            }
        }
        return null;
    }

    private void selectChip(List<TextView> groupChips, TextView selected) {
        for (TextView chip : groupChips) {
            chip.setSelected(chip == selected);
            updateChipVisual(chip);
        }
    }

    private void updateChipVisual(TextView chip) {
        boolean selected = chip.isSelected();
        chip.setTextColor(getColor(selected ? R.color.section_orange : R.color.text_primary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void collectChips(View view, List<TextView> out) {
        if (view instanceof TextView && view.getTag() != null) {
            out.add((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectChips(group.getChildAt(i), out);
            }
        }
    }

    private View createRecipeCard(RecipeStore.Recipe recipe) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_recipe_card, recipesContainer, false);
        ImageView imageView = card.findViewById(R.id.iv_recipe);
        TextView debugView = card.findViewById(R.id.tv_recipe_image_debug);
        TextView matchView = card.findViewById(R.id.tv_recipe_match);
        TextView timeView = card.findViewById(R.id.tv_recipe_time);
        TextView titleView = card.findViewById(R.id.tv_recipe_title);
        TextView cuisineView = card.findViewById(R.id.tv_recipe_cuisine);
        TextView typeView = card.findViewById(R.id.tv_recipe_type);

        titleView.setText(recipe.title);
        String dishType = recipe.type == null ? "" : recipe.type;
        String cuisine = recipe.cuisine == null ? "" : recipe.cuisine;
        boolean showDish = !dishType.trim().isEmpty();
        boolean showCuisine = !cuisine.trim().isEmpty();
        cuisineView.setText(cuisine);
        cuisineView.setVisibility(showCuisine ? View.VISIBLE : View.GONE);
        typeView.setText(dishType);
        typeView.setVisibility(showDish ? View.VISIBLE : View.GONE);
        timeView.setText(getString(R.string.recipe_time_short, recipe.timeMinutes));
        matchView.setText(getString(R.string.recipe_match_pct, recipe.matchPercent));

        String imageUrl = recipe.imageUrl;
        int placeholder = RecipeUi.getPlaceholderResId(recipe.type);
        imageView.setImageResource(placeholder);
        if (debugView != null) {
            debugView.setVisibility(View.GONE);
        }
        imageView.setTag(recipe.id);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (debugView != null) {
                            debugView.setText("Image load failed");
                            debugView.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (debugView != null) {
                            debugView.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .into(imageView);

        // No external image API: use provided imageUrl or placeholders only.

        card.setOnClickListener(v -> openDetails(recipe));

        return card;
    }

    private void openDetails(RecipeStore.Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_ID, recipe.id);
        intent.putExtra(RecipeDetailActivity.EXTRA_TITLE, recipe.title);
        intent.putExtra(RecipeDetailActivity.EXTRA_IMAGE_URL, recipe.imageUrl);
        intent.putExtra(RecipeDetailActivity.EXTRA_CUISINE, recipe.cuisine);
        intent.putExtra(RecipeDetailActivity.EXTRA_TYPE, recipe.type);
        intent.putExtra(RecipeDetailActivity.EXTRA_TIME, recipe.timeMinutes);
        intent.putExtra(RecipeDetailActivity.EXTRA_MATCH, recipe.matchPercent);
        startActivity(intent);
    }



    private int calculateMatchPercent(List<RecipeStore.Ingredient> ingredients) {
        List<String> availableNames = new ArrayList<>();
        List<AppRepository.Product> source = remoteProducts.isEmpty()
                ? repository.getAllProducts()
                : remoteProducts;
        for (AppRepository.Product product : source) {
            if (!product.units.isEmpty()) {
                availableNames.add(product.name.toLowerCase(Locale.ROOT));
            }
        }
        if (ingredients.isEmpty()) {
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
        return Math.round((matched * 100f) / ingredients.size());
    }

}

