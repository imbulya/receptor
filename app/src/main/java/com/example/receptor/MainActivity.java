package com.example.receptor;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private AppRepository repository;
    private final List<AppRepository.Product> products = new ArrayList<>();
    private final List<RecipeStore.Recipe> recipes = new ArrayList<>();

    private TextView greetingView;
    private TextView alertSummaryView;
    private TextView alertMoreView;
    private TextView recipeMatchView;
    private TextView recipeNameView;
    private TextView recipeCuisineView;
    private TextView recipeTimeView;

    private LinearLayout alertCardContainer;
    private LinearLayout alertFreshContent;
    private LinearLayout alertWarningContent;
    private LinearLayout alertRowsContainer;
    private LinearLayout recipeSectionContainer;

    private View alertIconContainer;

    private Button alertToProductsButton;

    private ImageButton settingsButton;
    private ImageView recipeImageView;
    private View recipeCard;
    private View productsCard;
    private View recipesCard;
    private View shoppingCard;
    private SwipeRefreshLayout swipeRefresh;

    private RecipeStore.Recipe dayRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        applyInsets();
        bindViews();

        repository = AppRepository.getInstance(this);

        refreshHomeData();

        ExpiryNotificationWorker.schedule(this);

        bindClicks();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                refreshHomeData();
                swipeRefresh.setRefreshing(false);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHomeData();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        greetingView = findViewById(R.id.tv_greeting);
        alertSummaryView = findViewById(R.id.tv_alert_summary);
        alertMoreView = findViewById(R.id.tv_alert_more);
        recipeMatchView = findViewById(R.id.tv_match_pct);
        recipeNameView = findViewById(R.id.tv_recipe_name);
        recipeCuisineView = findViewById(R.id.tv_recipe_cuisine);
        recipeTimeView = findViewById(R.id.tv_recipe_time);

        alertCardContainer = findViewById(R.id.alert_card_container);
        alertFreshContent = findViewById(R.id.alert_fresh_content);
        alertWarningContent = findViewById(R.id.alert_warning_content);
        alertRowsContainer = findViewById(R.id.alert_rows_container);
        recipeSectionContainer = findViewById(R.id.recipe_section_container);

        alertIconContainer = findViewById(R.id.alert_icon_container);

        alertToProductsButton = findViewById(R.id.btn_to_products);
        settingsButton = findViewById(R.id.btn_settings);
        recipeImageView = findViewById(R.id.iv_recipe);
        recipeCard = findViewById(R.id.recipe_card);
        productsCard = findViewById(R.id.card_products);
        recipesCard = findViewById(R.id.card_recipes);
        shoppingCard = findViewById(R.id.card_shopping);
        swipeRefresh = findViewById(R.id.swipe_refresh_main);
    }

    private void bindClicks() {
        settingsButton.setOnClickListener(v -> openSettingsScreen());
        alertToProductsButton.setOnClickListener(v -> openProductsScreen());
        if (alertCardContainer != null) {
            alertCardContainer.setOnClickListener(v -> openProductsScreen());
        }

        productsCard.setOnClickListener(v -> openProductsScreen());
        recipesCard.setOnClickListener(v -> openRecipesScreen());
        shoppingCard.setOnClickListener(v -> openShoppingList());

        recipeCard.setOnClickListener(v -> {
            if (dayRecipe == null) {
                return;
            }
            openRecipeDetails(dayRecipe);
        });
    }

    private void openProductsScreen() {
        startActivity(new Intent(this, ProductCategoriesActivity.class));
    }

    private void openRecipesScreen() {
        startActivity(new Intent(this, RecipesActivity.class));
    }

    private void openShoppingList() {
        startActivity(new Intent(this, ShoppingListActivity.class));
    }

    private void openSettingsScreen() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openRecipeDetails(RecipeStore.Recipe recipe) {
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

    private void openPlaceholder(String title, String note) {
        Intent intent = new Intent(this, PlaceholderActivity.class);
        intent.putExtra(PlaceholderActivity.EXTRA_TITLE, title);
        intent.putExtra(PlaceholderActivity.EXTRA_NOTE, note);
        startActivity(intent);
    }

    private void refreshHomeData() {
        greetingView.setText(buildGreeting());
        int warningDays = UserPreferences.getExpiryWarningDays(this, repository.getExpiryWarningDays());
        repository.setExpiryWarningDays(warningDays);
        loadRemoteProducts(warningDays);
        loadRemoteRecipes();
    }

    private void loadRemoteProducts(int warningDays) {
        DataClient.fetchAllProducts(new DataClient.Callback<List<AppRepository.Product>>() {
            @Override
            public void onSuccess(List<AppRepository.Product> data) {
                products.clear();
                if (data != null) {
                    products.addAll(data);
                }
                renderExpiryWidget(buildAlertItems(products, warningDays));
                updateRecipeOfDay();
            }

            @Override
            public void onError(Exception error) {
                products.clear();
                renderExpiryWidget(buildAlertItems(products, warningDays));
                updateRecipeOfDay();
            }
        });
    }

    private void loadRemoteRecipes() {
        DataClient.fetchRecipes(new DataClient.Callback<List<RecipeStore.Recipe>>() {
            @Override
            public void onSuccess(List<RecipeStore.Recipe> data) {
                recipes.clear();
                if (data != null) {
                    recipes.addAll(data);
                }
                updateRecipeOfDay();
            }

            @Override
            public void onError(Exception error) {
                recipes.clear();
                updateRecipeOfDay();
            }
        });
    }

    private void updateRecipeOfDay() {
        dayRecipe = pickRecipeOfDay(recipes, products);
        renderRecipeOfDay(dayRecipe);
    }

    private void renderRecipeOfDay(RecipeStore.Recipe recipe) {
        if (recipe == null) {
            recipeSectionContainer.setVisibility(View.VISIBLE);
            recipeMatchView.setText(getString(R.string.recipe_match_placeholder));
            recipeNameView.setText(getString(R.string.home_recipe_placeholder_title));
            recipeCuisineView.setText(getString(R.string.home_recipe_placeholder_cuisine));
            recipeTimeView.setText(getString(R.string.home_recipe_placeholder_time));
            recipeCard.setClickable(false);
            recipeCard.setFocusable(false);
            Glide.with(this)
                    .load(R.drawable.ic_section_recipes)
                    .centerCrop()
                    .into(recipeImageView);
            return;
        }

        recipeSectionContainer.setVisibility(View.VISIBLE);
        recipeMatchView.setText(getString(R.string.recipe_match_pct, recipe.matchPercent));
        recipeNameView.setText(recipe.title);
        String dishType = recipe.type == null ? "" : recipe.type;
        String cuisine = recipe.cuisine == null ? "" : recipe.cuisine;
        String subtitle = !dishType.trim().isEmpty() ? dishType : cuisine;
        recipeCuisineView.setText(getString(R.string.home_recipe_cuisine, subtitle));
        recipeTimeView.setText(getString(R.string.recipe_time_short, recipe.timeMinutes));
        recipeCard.setClickable(true);
        recipeCard.setFocusable(true);

        int placeholder = RecipeUi.getPlaceholderResId(recipe.type);
        String imageUrl = recipe.imageUrl;
        recipeImageView.setImageResource(placeholder);
        recipeImageView.setTag(recipe.id);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .centerCrop()
                .into(recipeImageView);

        // No external image API: use provided imageUrl or placeholders only.
    }

    private void renderExpiryWidget(List<AlertItem> alertItems) {
        if (alertItems.isEmpty()) {
            alertCardContainer.setBackgroundResource(R.drawable.bg_alert_card_green);
            alertFreshContent.setVisibility(View.VISIBLE);
            alertWarningContent.setVisibility(View.GONE);
            return;
        }

        alertFreshContent.setVisibility(View.GONE);
        alertWarningContent.setVisibility(View.VISIBLE);

        int expiredCount = 0;
        int expiringCount = 0;
        for (AlertItem item : alertItems) {
            if (item.status == ExpiryStatus.EXPIRED) {
                expiredCount++;
            } else if (item.status == ExpiryStatus.EXPIRING) {
                expiringCount++;
            }
        }

        boolean hasExpired = expiredCount > 0;
        alertCardContainer.setBackgroundResource(hasExpired ? R.drawable.bg_alert_card_red : R.drawable.bg_alert_card_yellow);
        alertIconContainer.setBackgroundResource(hasExpired ? R.drawable.bg_alert_icon_red : R.drawable.bg_alert_icon_yellow);
        alertSummaryView.setTextColor(Ui.color(this, hasExpired ? R.color.alert_red : R.color.alert_yellow));

        StringBuilder summary = new StringBuilder();
        if (expiredCount > 0) {
            summary.append(getString(R.string.alert_summary_expired, expiredCount));
        }
        if (expiredCount > 0 && expiringCount > 0) {
            summary.append(", ");
        }
        if (expiringCount > 0) {
            summary.append(getString(R.string.alert_summary_expiring, expiringCount));
        }
        alertSummaryView.setText(summary.toString());

        alertRowsContainer.removeAllViews();
        int rowsToShow = Math.min(4, alertItems.size());
        for (int i = 0; i < rowsToShow; i++) {
            alertRowsContainer.addView(createAlertRow(alertItems.get(i), i > 0));
        }

        if (alertItems.size() > 4) {
            int moreCount = alertItems.size() - 4;
            alertMoreView.setText(getString(R.string.alert_more_count, moreCount));
            alertMoreView.setVisibility(View.VISIBLE);
        } else {
            alertMoreView.setVisibility(View.GONE);
        }
    }

    private View createAlertRow(AlertItem item, boolean withTopMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.bg_alert_row);
        row.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (withTopMargin) {
            rowParams.topMargin = Ui.dp(this, 5);
        }
        row.setLayoutParams(rowParams);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(Ui.dp(this, 10), Ui.dp(this, 10));
        dotParams.rightMargin = Ui.dp(this, 10);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(item.status == ExpiryStatus.EXPIRED
                ? R.drawable.bg_status_dot_red
                : R.drawable.bg_status_dot_yellow);

        TextView productName = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        productName.setLayoutParams(nameParams);
        productName.setText(item.product.name);
        productName.setTextColor(Ui.color(this, R.color.text_primary));
        productName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        productName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        TextView statusText = new TextView(this);
        statusText.setTextColor(Ui.color(this, item.status == ExpiryStatus.EXPIRED ? R.color.alert_red : R.color.alert_yellow));
        statusText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

        long diffDays = daysBetweenToday(item.expiryDate);
        if (item.status == ExpiryStatus.EXPIRED) {
            statusText.setText(getString(R.string.alert_status_expired, Math.abs(diffDays)));
        } else if (diffDays == 0) {
            statusText.setText(getString(R.string.alert_status_today));
        } else {
            statusText.setText(getString(R.string.alert_status_days, diffDays));
        }

        row.addView(dot);
        row.addView(productName);
        row.addView(statusText);

        return row;
    }

    private String buildGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            return getString(R.string.greeting_night);
        }
        if (hour < 12) {
            return getString(R.string.greeting_morning);
        }
        if (hour < 17) {
            return getString(R.string.greeting_day);
        }
        if (hour < 21) {
            return getString(R.string.greeting_evening);
        }
        return getString(R.string.greeting_night);
    }

    private RecipeStore.Recipe pickRecipeOfDay(List<RecipeStore.Recipe> recipeList, List<AppRepository.Product> productList) {
        if (recipeList.isEmpty()) {
            return null;
        }

        RecipeStore.Recipe best = null;
        int bestMatch = -1;

        for (RecipeStore.Recipe recipe : recipeList) {
            if (recipe.ingredients == null || recipe.ingredients.size() < 2) {
                continue;
            }
            int match = calculateMatchPercentage(recipe, productList);
            if (match > bestMatch) {
                best = recipe;
                bestMatch = match;
            }
        }

        if (best != null && bestMatch > 0) {
            best.matchPercent = bestMatch;
            return best;
        }
        return null;
    }

    private int calculateMatchPercentage(RecipeStore.Recipe recipe, List<AppRepository.Product> productList) {
        List<String> availableNames = new ArrayList<>();
        for (AppRepository.Product product : productList) {
            if (!product.units.isEmpty()) {
                availableNames.add(product.name.toLowerCase(Locale.ROOT));
            }
        }

        int total = recipe.ingredients.size();
        if (total == 0) {
            return 0;
        }

        int matched = 0;
        for (RecipeStore.Ingredient ingredient : recipe.ingredients) {
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

    private List<AlertItem> buildAlertItems(List<AppRepository.Product> productList, int warningDays) {
        List<AlertItem> alertItems = new ArrayList<>();

        for (AppRepository.Product product : productList) {
            if (product.units.isEmpty()) {
                continue;
            }

            ExpiryStatus worstStatus = ExpiryStatus.FRESH;
            for (AppRepository.ProductUnit unit : product.units) {
                ExpiryStatus status = getExpiryStatus(unit.expiryDate, warningDays);
                if (status == ExpiryStatus.EXPIRED) {
                    worstStatus = ExpiryStatus.EXPIRED;
                    break;
                }
                if (status == ExpiryStatus.EXPIRING) {
                    worstStatus = ExpiryStatus.EXPIRING;
                }
            }

            if (worstStatus == ExpiryStatus.FRESH) {
                continue;
            }

            String worstExpiryDate = "";
            for (AppRepository.ProductUnit unit : product.units) {
                if (getExpiryStatus(unit.expiryDate, warningDays) == worstStatus) {
                    worstExpiryDate = unit.expiryDate;
                    break;
                }
            }

            alertItems.add(new AlertItem(product, worstStatus, worstExpiryDate));
        }

        alertItems.sort((a, b) -> {
            if (a.status == ExpiryStatus.EXPIRED && b.status != ExpiryStatus.EXPIRED) {
                return -1;
            }
            if (b.status == ExpiryStatus.EXPIRED && a.status != ExpiryStatus.EXPIRED) {
                return 1;
            }
            return Long.compare(startOfDayMillis(parseDate(a.expiryDate)), startOfDayMillis(parseDate(b.expiryDate)));
        });

        return alertItems;
    }

    private ExpiryStatus getExpiryStatus(String expiryDate, int warningDays) {
        long diffDays = daysBetweenToday(expiryDate);
        if (diffDays < 0) {
            return ExpiryStatus.EXPIRED;
        }
        if (diffDays <= warningDays) {
            return ExpiryStatus.EXPIRING;
        }
        return ExpiryStatus.FRESH;
    }

    private long daysBetweenToday(String expiryDate) {
        Date today = new Date();
        Date expiry = parseDate(expiryDate);
        long diffMs = startOfDayMillis(expiry) - startOfDayMillis(today);
        return Math.round(diffMs / 86400000d);
    }

    private long startOfDayMillis(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private Date parseDate(String value) {
        try {
            Date parsed = DATE_FORMAT.parse(value);
            return parsed != null ? parsed : new Date();
        } catch (ParseException e) {
            return new Date();
        }
    }

    private enum ExpiryStatus {
        FRESH,
        EXPIRING,
        EXPIRED
    }

    private static final class AlertItem {
        final AppRepository.Product product;
        final ExpiryStatus status;
        final String expiryDate;

        AlertItem(AppRepository.Product product, ExpiryStatus status, String expiryDate) {
            this.product = product;
            this.status = status;
            this.expiryDate = expiryDate;
        }
    }
}
