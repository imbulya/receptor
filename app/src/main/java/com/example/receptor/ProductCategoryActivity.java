package com.example.receptor;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private AppRepository repository;
    private AppRepository.Category category;
    private String categoryId;
    private String expandedProductId;
    private final List<AppRepository.Product> products = new ArrayList<>();

    private LinearLayout productsContainer;
    private ImageButton searchButton;
    private View searchContainer;
    private EditText searchInput;
    private String searchQuery = "";
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_category);

        repository = AppRepository.getInstance(this);
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);

        if (categoryId == null) {
            finish();
            return;
        }

        applyInsets();

        TextView titleView = findViewById(R.id.tv_product_category_title);
        titleView.setText(getString(R.string.category_unknown));

        productsContainer = findViewById(R.id.products_container);
        swipeRefresh = findViewById(R.id.swipe_refresh_products);

        ImageButton backButton = findViewById(R.id.btn_back_product_list);
        ImageButton fabButton = findViewById(R.id.fab_add_product);
        searchButton = findViewById(R.id.btn_search_product);
        searchContainer = findViewById(R.id.search_container);
        searchInput = findViewById(R.id.et_product_search);

        backButton.setOnClickListener(v -> finish());
        fabButton.setOnClickListener(v -> showAddNameDialog());
        if (fabButton != null) {
            GradientDrawable fabBg = new GradientDrawable();
            fabBg.setShape(GradientDrawable.RECTANGLE);
            fabBg.setCornerRadius(Ui.dp(this, 18));
            fabBg.setColor(getColor(R.color.section_green));
            fabButton.setBackground(fabBg);
        }
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> toggleSearch());
        }
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString();
                    renderProducts();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        loadCategoryAndProducts();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadCategoryAndProducts();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoryAndProducts();
    }

    private void applyInsets() {
        View root = findViewById(R.id.product_category_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadCategoryAndProducts() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        DataClient.fetchProductCategory(categoryId, new DataClient.Callback<DataClient.CategoryInfo>() {
            @Override
            public void onSuccess(DataClient.CategoryInfo data) {
                category = data == null ? null : data.category;
                updateHeader();
                loadProducts();
            }

            @Override
            public void onError(Exception error) {
                category = null;
                updateHeader();
                loadProducts();
            }
        });
    }

    private void loadProducts() {
        DataClient.fetchProductsByCategory(categoryId, new DataClient.Callback<List<AppRepository.Product>>() {
            @Override
            public void onSuccess(List<AppRepository.Product> data) {
                products.clear();
                if (data != null) {
                    products.addAll(data);
                }
                renderProducts();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onError(Exception error) {
                products.clear();
                renderProducts();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void renderProducts() {
        productsContainer.removeAllViews();
        if (category == null) {
            return;
        }
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<AppRepository.Product> filtered = new ArrayList<>(products);
        if (!query.isEmpty()) {
            filtered.removeIf(product -> !product.name.toLowerCase(Locale.ROOT).contains(query));
        }
        filtered.sort((a, b) -> {
            boolean aHas = !a.units.isEmpty();
            boolean bHas = !b.units.isEmpty();
            if (aHas && !bHas) {
                return -1;
            }
            if (!aHas && bHas) {
                return 1;
            }
            return a.name.compareToIgnoreCase(b.name);
        });

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(query.isEmpty() ? R.string.empty_category_products : R.string.search_empty));
            empty.setTextColor(Ui.color(this, R.color.text_secondary));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            empty.setPadding(Ui.dp(this, 12), Ui.dp(this, 30), Ui.dp(this, 12), Ui.dp(this, 30));
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            productsContainer.addView(empty);
            return;
        }

        for (AppRepository.Product product : filtered) {
            productsContainer.addView(createProductCard(product));
        }
    }

    private void updateHeader() {
        TextView titleView = findViewById(R.id.tv_product_category_title);
        if (category == null) {
            titleView.setText(getString(R.string.category_unknown));
            return;
        }
        String emoji = category.emoji == null ? "" : category.emoji;
        titleView.setText(emoji.isEmpty() ? category.name : (emoji + " " + category.name));
        ImageButton fabButton = findViewById(R.id.fab_add_product);
        if (fabButton != null) {
            GradientDrawable fabBg = new GradientDrawable();
            fabBg.setShape(GradientDrawable.RECTANGLE);
            fabBg.setCornerRadius(Ui.dp(this, 18));
            fabBg.setColor(Ui.parseColorSafe(category.textColor, getColor(R.color.section_green)));
            fabButton.setBackground(fabBg);
        }
    }

    private View createProductCard(AppRepository.Product product) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_product_card, productsContainer, false);
        MaterialCardView card = cardView.findViewById(R.id.card_product_root);
        LinearLayout infoClickArea = cardView.findViewById(R.id.product_info_click_area);
        TextView nameView = cardView.findViewById(R.id.tv_product_name);
        ImageView expandView = cardView.findViewById(R.id.iv_expand);
        LinearLayout qtyPill = cardView.findViewById(R.id.qty_pill);
        TextView minusButton = cardView.findViewById(R.id.btn_minus);
        TextView qtyView = cardView.findViewById(R.id.tv_qty);
        TextView plusButton = cardView.findViewById(R.id.btn_plus);
        LinearLayout detailsContainer = cardView.findViewById(R.id.details_container);

        int quantity = product.units.size();
        card.setCardBackgroundColor(Color.WHITE);

        nameView.setText(product.name);
        qtyView.setText(String.valueOf(quantity));

        qtyPill.setBackground(createRoundedDrawable(category.bgColor, 999));
        int categoryTextColor = Ui.parseColorSafe(category.textColor, Color.BLACK);
        qtyView.setTextColor(categoryTextColor);
        minusButton.setTextColor(categoryTextColor);
        plusButton.setTextColor(categoryTextColor);

        boolean isOut = quantity == 0;
        card.setAlpha(isOut ? 0.6f : 1f);

        minusButton.setEnabled(true);
        minusButton.setBackgroundResource(R.drawable.bg_qty_button);
        plusButton.setBackgroundResource(R.drawable.bg_qty_button);
        if (quantity == 0) {
            minusButton.setTextColor(getColor(R.color.alert_red));
        } else {
            minusButton.setTextColor(categoryTextColor);
        }

        boolean isExpanded = product.id.equals(expandedProductId) && quantity > 0;
        expandView.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);
        expandView.setRotation(isExpanded ? -90f : 90f);

        detailsContainer.removeAllViews();
        if (isExpanded) {
            detailsContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < product.units.size(); i++) {
                AppRepository.ProductUnit unit = product.units.get(i);
                detailsContainer.addView(createExpiryRow(unit, i));
            }
        } else {
            detailsContainer.setVisibility(View.GONE);
        }

        infoClickArea.setOnClickListener(v -> {
            if (quantity == 0) {
                return;
            }
            if (product.id.equals(expandedProductId)) {
                expandedProductId = null;
            } else {
                expandedProductId = product.id;
            }
            renderProducts();
        });

        plusButton.setOnClickListener(v -> {
            showAddUnitDialog(product);
        });

        minusButton.setOnClickListener(v -> {
            if (quantity == 0) {
                showRemoveProductDialog(product);
            } else {
                showRemoveUnitDialog(product);
            }
        });

        return card;
    }

    private View createExpiryRow(AppRepository.ProductUnit unit, int index) {
        AppRepository.ExpiryStatus status = repository.getExpiryStatus(unit.expiryDate);
        StatusStyle style = statusStyle(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = Ui.dp(this, 6);
        row.setLayoutParams(rowParams);
        row.setBackground(createRoundedDrawable(style.backgroundColor, 12));

        TextView dot = new TextView(this);
        dot.setText("●");
        dot.setTextColor(style.textColor);
        dot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        dotParams.rightMargin = Ui.dp(this, 10);
        dot.setLayoutParams(dotParams);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView title = new TextView(this);
        title.setText(getString(R.string.unit_label, index + 1, style.label));
        title.setTextColor(style.textColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText(getString(R.string.expiry_until, repository.formatExpiryDate(unit.expiryDate)));
        subtitle.setTextColor(style.textColor);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);

        textBox.addView(title);
        textBox.addView(subtitle);

        row.addView(dot);
        row.addView(textBox);

        return row;
    }

    private void showAddNameDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product_name, null);
        dialog.setContentView(view);

        EditText nameEdit = view.findViewById(R.id.et_product_name);
        Button saveButton = view.findViewById(R.id.btn_add_product_name);

        saveButton.setOnClickListener(v -> {
            String productName = nameEdit.getText() == null ? "" : nameEdit.getText().toString().trim();
            if (productName.isEmpty()) {
                nameEdit.setError(getString(R.string.product_name_required));
                return;
            }
            DataClient.addProduct(categoryId, productName, new DataClient.Callback<AppRepository.Product>() {
                @Override
                public void onSuccess(AppRepository.Product data) {
                    dialog.dismiss();
                    loadProducts();
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(ProductCategoryActivity.this, getString(R.string.product_exists), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showAddUnitDialog(AppRepository.Product product) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_unit_expiry, null);
        dialog.setContentView(view);

        TextView dateView = view.findViewById(R.id.tv_unit_date);
        Button addButton = view.findViewById(R.id.btn_add_unit);

        final Date[] selectedDate = {repository.parseApiDate(repository.getDefaultExpiryDateForNewUnit())};
        dateView.setText(repository.formatExpiryDate(repository.toApiDate(selectedDate[0])));

        dateView.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate[0]);
            DatePickerDialog pickerDialog = new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(Calendar.YEAR, year);
                        selected.set(Calendar.MONTH, month);
                        selected.set(Calendar.DAY_OF_MONTH, day);
                        selectedDate[0] = selected.getTime();
                        dateView.setText(repository.formatExpiryDate(repository.toApiDate(selectedDate[0])));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            pickerDialog.show();
        });

        addButton.setOnClickListener(v -> {
            DataClient.addProductUnit(product.id, repository.toApiDate(selectedDate[0]), new DataClient.Callback<AppRepository.ProductUnit>() {
                @Override
                public void onSuccess(AppRepository.ProductUnit data) {
                    dialog.dismiss();
                    loadProducts();
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showRemoveUnitDialog(AppRepository.Product product) {
        AppRepository.ProductUnit oldest = findOldestUnit(product);
        if (oldest == null) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_remove_unit_confirm, null);
        dialog.setContentView(view);

        TextView message = view.findViewById(R.id.tv_remove_message);
        Button confirmButton = view.findViewById(R.id.btn_confirm_remove);
        Button cancelButton = view.findViewById(R.id.btn_cancel_remove);

        message.setText(getString(R.string.remove_unit_message, repository.formatExpiryDate(oldest.expiryDate)));

        confirmButton.setOnClickListener(v -> {
            DataClient.deleteProductUnit(product.id, oldest.id, new DataClient.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    if (product.id.equals(expandedProductId)) {
                        AppRepository.Product refreshed = findProduct(product.id, products);
                        if (refreshed == null || refreshed.units.isEmpty()) {
                            expandedProductId = null;
                        }
                    }
                    dialog.dismiss();
                    loadProducts();
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showRemoveProductDialog(AppRepository.Product product) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_remove_product_confirm, null);
        dialog.setContentView(view);

        TextView message = view.findViewById(R.id.tv_remove_product_message);
        Button confirmButton = view.findViewById(R.id.btn_confirm_remove_product);
        Button cancelButton = view.findViewById(R.id.btn_cancel_remove_product);

        message.setText(getString(R.string.remove_product_message, product.name));

        confirmButton.setOnClickListener(v -> {
            DataClient.deleteProduct(product.id, new DataClient.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    if (product.id.equals(expandedProductId)) {
                        expandedProductId = null;
                    }
                    dialog.dismiss();
                    loadProducts();
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private AppRepository.ProductUnit findOldestUnit(AppRepository.Product product) {
        AppRepository.ProductUnit oldest = null;
        Date oldestDate = null;
        for (AppRepository.ProductUnit unit : product.units) {
            Date date = repository.parseApiDate(unit.expiryDate);
            if (oldestDate == null || date.before(oldestDate)) {
                oldestDate = date;
                oldest = unit;
            }
        }
        return oldest;
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

    private AppRepository.Product findProduct(String productId, List<AppRepository.Product> productList) {
        for (AppRepository.Product product : productList) {
            if (product.id.equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private StatusStyle statusStyle(AppRepository.ExpiryStatus status) {
        if (status == AppRepository.ExpiryStatus.EXPIRED) {
            return new StatusStyle(
                    Ui.color(this, R.color.alert_red),
                    Color.parseColor("#FDD8E0"),
                    getString(R.string.status_expired)
            );
        }
        if (status == AppRepository.ExpiryStatus.EXPIRING) {
            return new StatusStyle(
                    Ui.color(this, R.color.alert_yellow),
                    Color.parseColor("#F5E8CC"),
                    getString(R.string.status_expiring)
            );
        }
        return new StatusStyle(
                Ui.color(this, R.color.section_green),
                Color.parseColor("#D5EDD8"),
                getString(R.string.status_fresh)
        );
    }

    private GradientDrawable createRoundedDrawable(String fillColor, int radiusDp) {
        return Ui.roundedRect(this, fillColor, radiusDp, Color.WHITE);
    }

    private GradientDrawable createRoundedDrawable(int fillColor, int radiusDp) {
        return Ui.roundedRect(this, fillColor, radiusDp);
    }

    private static final class StatusStyle {
        final int textColor;
        final int backgroundColor;
        final String label;

        StatusStyle(int textColor, int backgroundColor, String label) {
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
            this.label = label;
        }
    }
}
