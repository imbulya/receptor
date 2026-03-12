package com.example.receptor;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProductCategoryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private AppRepository repository;
    private AppRepository.Category category;
    private String categoryId;
    private String expandedProductId;

    private LinearLayout productsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_category);

        repository = AppRepository.getInstance();
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);

        if (categoryId == null) {
            finish();
            return;
        }

        category = repository.getCategoryById(categoryId);
        if (category == null) {
            finish();
            return;
        }

        applyInsets();

        TextView titleView = findViewById(R.id.tv_product_category_title);
        titleView.setText(category.name);

        productsContainer = findViewById(R.id.products_container);

        ImageButton backButton = findViewById(R.id.btn_back_product_list);
        ImageButton fabButton = findViewById(R.id.fab_add_product);

        backButton.setOnClickListener(v -> finish());
        fabButton.setOnClickListener(v -> showAddMenu());

        renderProducts();
    }

    private void applyInsets() {
        View root = findViewById(R.id.product_category_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void renderProducts() {
        List<AppRepository.Product> products = repository.getProductsByCategory(categoryId);
        products.sort((a, b) -> {
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

        productsContainer.removeAllViews();

        if (products.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.empty_category_products));
            empty.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            empty.setPadding(dp(12), dp(30), dp(12), dp(30));
            empty.setGravity(Gravity.CENTER_HORIZONTAL);
            productsContainer.addView(empty);
            return;
        }

        for (AppRepository.Product product : products) {
            productsContainer.addView(createProductCard(product));
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

        nameView.setText(product.name);
        qtyView.setText(String.valueOf(quantity));

        qtyPill.setBackground(createRoundedDrawable(category.bgColor, 999));
        int categoryTextColor = parseColorSafe(category.textColor, Color.BLACK);
        qtyView.setTextColor(categoryTextColor);
        minusButton.setTextColor(categoryTextColor);
        plusButton.setTextColor(categoryTextColor);

        minusButton.setAlpha(quantity == 0 ? 0.35f : 1f);
        minusButton.setEnabled(quantity > 0);

        boolean isExpanded = product.id.equals(expandedProductId) && quantity > 0;
        expandView.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);
        expandView.setRotation(isExpanded ? 90f : 0f);

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
            repository.addUnit(product.id, repository.getDefaultExpiryDateForNewUnit());
            renderProducts();
        });

        minusButton.setOnClickListener(v -> {
            repository.removeUnit(product.id);
            if (product.id.equals(expandedProductId)) {
                AppRepository.Product refreshed = findProduct(product.id, repository.getProductsByCategory(categoryId));
                if (refreshed == null || refreshed.units.isEmpty()) {
                    expandedProductId = null;
                }
            }
            renderProducts();
        });

        return card;
    }

    private View createExpiryRow(AppRepository.ProductUnit unit, int index) {
        AppRepository.ExpiryStatus status = repository.getExpiryStatus(unit.expiryDate);
        StatusStyle style = statusStyle(status);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(6);
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
        dotParams.rightMargin = dp(10);
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

    private void showAddMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product_options, null);
        dialog.setContentView(view);

        Button addManualButton = view.findViewById(R.id.btn_add_manual);
        Button addReceiptButton = view.findViewById(R.id.btn_add_receipt);

        addManualButton.setOnClickListener(v -> {
            dialog.dismiss();
            showManualAddDialog();
        });

        addReceiptButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, PlaceholderActivity.class);
            intent.putExtra(PlaceholderActivity.EXTRA_TITLE, getString(R.string.add_from_receipt));
            intent.putExtra(PlaceholderActivity.EXTRA_NOTE, getString(R.string.receipt_placeholder_note));
            startActivity(intent);
        });

        dialog.show();
    }

    private void showManualAddDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_manual_product, null);
        dialog.setContentView(view);

        EditText nameEdit = view.findViewById(R.id.et_manual_name);
        EditText qtyEdit = view.findViewById(R.id.et_manual_quantity);
        TextView dateView = view.findViewById(R.id.tv_manual_date);
        Button pickDateButton = view.findViewById(R.id.btn_pick_date);
        Button saveButton = view.findViewById(R.id.btn_save_manual);

        final Date[] selectedDate = {repository.parseApiDate(repository.getDefaultExpiryDateForNewUnit())};
        dateView.setText(repository.formatExpiryDate(repository.toApiDate(selectedDate[0])));

        pickDateButton.setOnClickListener(v -> {
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

        saveButton.setOnClickListener(v -> {
            String productName = nameEdit.getText() == null ? "" : nameEdit.getText().toString().trim();
            if (productName.isEmpty()) {
                nameEdit.setError(getString(R.string.product_name_required));
                return;
            }

            int quantity = 1;
            try {
                String qtyText = qtyEdit.getText() == null ? "1" : qtyEdit.getText().toString().trim();
                quantity = Integer.parseInt(qtyText);
            } catch (Exception ignored) {
                quantity = 1;
            }
            if (quantity < 1) {
                quantity = 1;
            }

            repository.addProductManual(
                    categoryId,
                    productName,
                    quantity,
                    repository.toApiDate(selectedDate[0])
            );

            dialog.dismiss();
            renderProducts();
        });

        dialog.show();
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
                    getResources().getColor(R.color.alert_red, getTheme()),
                    Color.parseColor("#FDD8E0"),
                    getString(R.string.status_expired)
            );
        }
        if (status == AppRepository.ExpiryStatus.EXPIRING) {
            return new StatusStyle(
                    getResources().getColor(R.color.alert_yellow, getTheme()),
                    Color.parseColor("#F5E8CC"),
                    getString(R.string.status_expiring)
            );
        }
        return new StatusStyle(
                getResources().getColor(R.color.section_green, getTheme()),
                Color.parseColor("#D5EDD8"),
                getString(R.string.status_fresh)
        );
    }

    private GradientDrawable createRoundedDrawable(String fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(parseColorSafe(fillColor, Color.WHITE));
        return drawable;
    }

    private GradientDrawable createRoundedDrawable(int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(fillColor);
        return drawable;
    }

    private int parseColorSafe(String value, int fallback) {
        try {
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
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