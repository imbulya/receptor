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

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int EXPIRY_WARNING_DAYS = 3;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final List<Product> products = new ArrayList<>();
    private final List<Recipe> recipes = new ArrayList<>();
    private int unitCounter = 1;

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

    private Recipe dayRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        applyInsets();
        bindViews();

        products.addAll(createInitialProducts());
        recipes.addAll(createInitialRecipes());

        greetingView.setText(buildGreeting());
        renderExpiryWidget(buildAlertItems(products, EXPIRY_WARNING_DAYS));

        dayRecipe = pickRecipeOfDay(recipes, products);
        renderRecipeOfDay(dayRecipe);

        bindClicks();
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
    }

    private void bindClicks() {
        settingsButton.setOnClickListener(v -> openPlaceholder("Настройки", getString(R.string.placeholder_note)));
        alertToProductsButton.setOnClickListener(v -> openProductsScreen());

        productsCard.setOnClickListener(v -> openProductsScreen());
        recipesCard.setOnClickListener(v -> openPlaceholder("Рецепты", getString(R.string.placeholder_note)));
        shoppingCard.setOnClickListener(v -> openPlaceholder("Список покупок", getString(R.string.placeholder_note)));

        recipeCard.setOnClickListener(v -> {
            if (dayRecipe == null) {
                return;
            }
            openPlaceholder(dayRecipe.name, "Откроем полноценный экран рецепта на следующем шаге.");
        });
    }

    private void openProductsScreen() {
        startActivity(new Intent(this, ProductCategoriesActivity.class));
    }

    private void openPlaceholder(String title, String note) {
        Intent intent = new Intent(this, PlaceholderActivity.class);
        intent.putExtra(PlaceholderActivity.EXTRA_TITLE, title);
        intent.putExtra(PlaceholderActivity.EXTRA_NOTE, note);
        startActivity(intent);
    }

    private void renderRecipeOfDay(Recipe recipe) {
        if (recipe == null) {
            recipeSectionContainer.setVisibility(View.GONE);
            return;
        }

        recipeSectionContainer.setVisibility(View.VISIBLE);
        recipeMatchView.setText(recipe.matchPercent + "%");
        recipeNameView.setText(recipe.name);
        recipeCuisineView.setText("🍴 " + recipe.cuisine);
        recipeTimeView.setText(recipe.timeMinutes + " " + getString(R.string.home_min_suffix));

        Glide.with(this)
                .load(recipe.imageUrl)
                .centerCrop()
                .into(recipeImageView);
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
        alertSummaryView.setTextColor(getColorCompat(hasExpired ? R.color.alert_red : R.color.alert_yellow));

        StringBuilder summary = new StringBuilder();
        if (expiredCount > 0) {
            summary.append(expiredCount).append(" просрочено");
        }
        if (expiredCount > 0 && expiringCount > 0) {
            summary.append(", ");
        }
        if (expiringCount > 0) {
            summary.append(expiringCount).append(" скоро истекает");
        }
        alertSummaryView.setText(summary.toString());

        alertRowsContainer.removeAllViews();
        int rowsToShow = Math.min(4, alertItems.size());
        for (int i = 0; i < rowsToShow; i++) {
            alertRowsContainer.addView(createAlertRow(alertItems.get(i), i > 0));
        }

        if (alertItems.size() > 4) {
            int moreCount = alertItems.size() - 4;
            alertMoreView.setText("и ещё " + moreCount + " продукт(а)...");
            alertMoreView.setVisibility(View.VISIBLE);
        } else {
            alertMoreView.setVisibility(View.GONE);
        }
    }

    private View createAlertRow(AlertItem item, boolean withTopMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.bg_alert_row);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (withTopMargin) {
            rowParams.topMargin = dp(5);
        }
        row.setLayoutParams(rowParams);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.rightMargin = dp(10);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(item.status == ExpiryStatus.EXPIRED
                ? R.drawable.bg_status_dot_red
                : R.drawable.bg_status_dot_yellow);

        TextView productName = new TextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        productName.setLayoutParams(nameParams);
        productName.setText(item.product.name);
        productName.setTextColor(getColorCompat(R.color.text_primary));
        productName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        productName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        TextView statusText = new TextView(this);
        statusText.setTextColor(getColorCompat(item.status == ExpiryStatus.EXPIRED ? R.color.alert_red : R.color.alert_yellow));
        statusText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

        long diffDays = daysBetweenToday(item.expiryDate);
        if (item.status == ExpiryStatus.EXPIRED) {
            statusText.setText("просрочен " + Math.abs(diffDays) + " дн.");
        } else {
            statusText.setText(diffDays == 0 ? "сегодня" : diffDays + " дн.");
        }

        row.addView(dot);
        row.addView(productName);
        row.addView(statusText);

        return row;
    }

    private String buildGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            return "Доброй ночи! 🌙";
        }
        if (hour < 12) {
            return "Доброе утро! ☀️";
        }
        if (hour < 17) {
            return "Добрый день! 🌿";
        }
        if (hour < 21) {
            return "Добрый вечер! 🍂";
        }
        return "Доброй ночи! 🌙";
    }

    private Recipe pickRecipeOfDay(List<Recipe> recipeList, List<Product> productList) {
        if (recipeList.isEmpty()) {
            return null;
        }

        Recipe best = null;
        int bestMatch = -1;

        for (Recipe recipe : recipeList) {
            int match = calculateMatchPercentage(recipe, productList);
            if (match > bestMatch) {
                best = recipe;
                bestMatch = match;
            }
        }

        if (best != null) {
            best.matchPercent = bestMatch;
        }
        return best;
    }

    private int calculateMatchPercentage(Recipe recipe, List<Product> productList) {
        List<String> availableNames = new ArrayList<>();
        for (Product product : productList) {
            if (!product.units.isEmpty()) {
                availableNames.add(product.name.toLowerCase(Locale.ROOT));
            }
        }

        int total = recipe.ingredients.size();
        if (total == 0) {
            return 0;
        }

        int matched = 0;
        for (String ingredient : recipe.ingredients) {
            String ingredientName = ingredient.toLowerCase(Locale.ROOT);
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

    private List<AlertItem> buildAlertItems(List<Product> productList, int warningDays) {
        List<AlertItem> alertItems = new ArrayList<>();

        for (Product product : productList) {
            if (product.units.isEmpty()) {
                continue;
            }

            ExpiryStatus worstStatus = ExpiryStatus.FRESH;
            for (ProductUnit unit : product.units) {
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
            for (ProductUnit unit : product.units) {
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

    private String addDaysFromToday(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return DATE_FORMAT.format(calendar.getTime());
    }

    private List<ProductUnit> units(int... days) {
        List<ProductUnit> list = new ArrayList<>();
        for (int day : days) {
            list.add(new ProductUnit("u" + unitCounter++, addDaysFromToday(day)));
        }
        return list;
    }

    private List<Product> createInitialProducts() {
        List<Product> list = new ArrayList<>();

        list.add(new Product("p1", "Помидоры", "vegetables", units(3, -1)));
        list.add(new Product("p2", "Огурцы", "vegetables", units(7)));
        list.add(new Product("p3", "Морковь", "vegetables", units(14, 14)));
        list.add(new Product("p4", "Лук репчатый", "vegetables", units(30)));
        list.add(new Product("p5", "Перец болгарский", "vegetables", units(5)));
        list.add(new Product("p6", "Чеснок", "vegetables", units(60)));
        list.add(new Product("p7", "Шпинат", "vegetables", new ArrayList<>()));
        list.add(new Product("p8", "Брокколи", "vegetables", new ArrayList<>()));
        list.add(new Product("p9", "Яблоки", "fruits", units(10, 10)));
        list.add(new Product("p10", "Бананы", "fruits", units(2)));
        list.add(new Product("p11", "Лимон", "fruits", units(15)));
        list.add(new Product("p12", "Курица", "meat", units(3)));
        list.add(new Product("p13", "Говядина", "meat", units(-2)));
        list.add(new Product("p14", "Свинина", "meat", new ArrayList<>()));
        list.add(new Product("p15", "Молоко", "dairy", units(5)));
        list.add(new Product("p16", "Яйца", "dairy", units(21, 21)));
        list.add(new Product("p17", "Сыр", "dairy", units(14)));
        list.add(new Product("p18", "Сметана", "dairy", units(7)));
        list.add(new Product("p19", "Макароны", "grains", units(180)));
        list.add(new Product("p20", "Рис", "grains", units(365)));
        list.add(new Product("p21", "Гречка", "grains", units(270)));
        list.add(new Product("p22", "Лосось", "seafood", units(2)));
        list.add(new Product("p23", "Оливковое масло", "condiments", units(180)));
        list.add(new Product("p24", "Томатная паста", "condiments", units(90)));
        list.add(new Product("p25", "Базилик свежий", "condiments", units(4)));

        return list;
    }

    private List<Recipe> createInitialRecipes() {
        String pastaImage = "https://images.unsplash.com/photo-1768204037592-92308f35120c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwYXN0YSUyMGNhcmJvbmFyYSUyMGNvenklMjBkaW5uZXJ8ZW58MXx8fHwxNzcxOTU3MTkyfDA&ixlib=rb-4.1.0&q=80&w=1080";
        String soupImage = "https://images.unsplash.com/photo-1625940947631-908aa92ef5e7?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx2ZWdldGFibGUlMjBzb3VwJTIwaG9tZW1hZGUlMjB3YXJtfGVufDF8fHx8MTc3MTk1NzE5NXww&ixlib=rb-4.1.0&q=80&w=1080";
        String avocadoImage = "https://images.unsplash.com/photo-1623691751128-ade6f7e59003?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhdm9jYWRvJTIwdG9hc3QlMjBicmVha2Zhc3QlMjBoZWFsdGh5fGVufDF8fHx8MTc3MTkzNjQ4NHww&ixlib=rb-4.1.0&q=80&w=1080";
        String saladImage = "https://images.unsplash.com/photo-1769481614068-47cfb4d1f125?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxncmVlayUyMHNhbGFkJTIwZnJlc2glMjBtZWRpdGVycmFuZWFufGVufDF8fHx8MTc3MTk1NzE5OHww&ixlib=rb-4.1.0&q=80&w=1080";
        String smoothieImage = "https://images.unsplash.com/photo-1662611284583-f34180194370?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjaGlja2VuJTIwc3RpciUyMGZyeSUyMHZlZ2V0YWJsZXMlMjB3b2t8ZW58MXx8fHwxNzcxODYwNTkwfDA&ixlib=rb-4.1.0&q=80&w=1080";

        List<Recipe> list = new ArrayList<>();
        list.add(new Recipe("r1", "Паста с томатным соусом", pastaImage, "Итальянская", 25,
                Arrays.asList("Макароны", "Помидоры", "Томатная паста", "Чеснок", "Оливковое масло", "Базилик свежий")));
        list.add(new Recipe("r2", "Куриный суп", soupImage, "Русская", 60,
                Arrays.asList("Курица", "Морковь", "Лук репчатый", "Картофель", "Зелень")));
        list.add(new Recipe("r3", "Тост с авокадо", avocadoImage, "Интернациональная", 10,
                Arrays.asList("Хлеб цельнозерновой", "Авокадо", "Яйца", "Лимон", "Соль")));
        list.add(new Recipe("r4", "Салат с курицей", saladImage, "Интернациональная", 20,
                Arrays.asList("Курица", "Огурцы", "Помидоры", "Перец болгарский", "Сметана")));
        list.add(new Recipe("r5", "Смузи-боул", smoothieImage, "Интернациональная", 10,
                Arrays.asList("Бананы", "Яблоки", "Молоко", "Мёд", "Гречка")));

        return list;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    private int getColorCompat(int colorRes) {
        return getResources().getColor(colorRes, getTheme());
    }

    private enum ExpiryStatus {
        FRESH,
        EXPIRING,
        EXPIRED
    }

    private static final class ProductUnit {
        final String id;
        final String expiryDate;

        ProductUnit(String id, String expiryDate) {
            this.id = id;
            this.expiryDate = expiryDate;
        }
    }

    private static final class Product {
        final String id;
        final String name;
        final String categoryId;
        final List<ProductUnit> units;

        Product(String id, String name, String categoryId, List<ProductUnit> units) {
            this.id = id;
            this.name = name;
            this.categoryId = categoryId;
            this.units = units;
        }
    }

    private static final class Recipe {
        final String id;
        final String name;
        final String imageUrl;
        final String cuisine;
        final int timeMinutes;
        final List<String> ingredients;
        int matchPercent;

        Recipe(String id, String name, String imageUrl, String cuisine, int timeMinutes, List<String> ingredients) {
            this.id = id;
            this.name = name;
            this.imageUrl = imageUrl;
            this.cuisine = cuisine;
            this.timeMinutes = timeMinutes;
            this.ingredients = ingredients;
            this.matchPercent = 0;
        }
    }

    private static final class AlertItem {
        final Product product;
        final ExpiryStatus status;
        final String expiryDate;

        AlertItem(Product product, ExpiryStatus status, String expiryDate) {
            this.product = product;
            this.status = status;
            this.expiryDate = expiryDate;
        }
    }
}