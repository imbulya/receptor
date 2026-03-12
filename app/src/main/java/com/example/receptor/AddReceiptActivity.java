package com.example.receptor;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddReceiptActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";

    private AppRepository repository;
    private String categoryId;

    private LinearLayout itemsContainer;
    private TextView emptyStateView;
    private LinearLayout scanBlock;
    private LinearLayout bannerView;
    private TextView bannerText;

    private final List<ReceiptItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_receipt);

        repository = AppRepository.getInstance();
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        if (categoryId == null) {
            finish();
            return;
        }

        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_receipt);
        Button scanButton = findViewById(R.id.btn_scan_receipt);
        Button confirmButton = findViewById(R.id.btn_confirm_receipt);
        itemsContainer = findViewById(R.id.receipt_items_container);
        emptyStateView = findViewById(R.id.tv_receipt_empty);
        scanBlock = findViewById(R.id.scan_block);
        bannerView = findViewById(R.id.receipt_banner);
        bannerText = findViewById(R.id.tv_receipt_banner);

        backButton.setOnClickListener(v -> finish());

        scanButton.setOnClickListener(v -> {
            items.clear();
            populateSampleItems();
            refreshItems();
        });

        confirmButton.setOnClickListener(v -> handleConfirm());

        refreshItems();
    }

    private void applyInsets() {
        View root = findViewById(R.id.receipt_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void populateSampleItems() {
        String[] names = getResources().getStringArray(R.array.receipt_sample_names);
        String[] categories = getResources().getStringArray(R.array.receipt_sample_categories);
        String[] icons = getResources().getStringArray(R.array.receipt_sample_icons);
        int count = Math.min(names.length, Math.min(categories.length, icons.length));
        for (int i = 0; i < count; i++) {
            addSample(names[i], categories[i], icons[i]);
        }
    }

    private void refreshItems() {
        itemsContainer.removeAllViews();
        if (items.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            scanBlock.setVisibility(View.VISIBLE);
            bannerView.setVisibility(View.GONE);
            return;
        }
        emptyStateView.setVisibility(View.GONE);
        scanBlock.setVisibility(View.GONE);
        bannerView.setVisibility(View.VISIBLE);
        bannerText.setText(getString(R.string.receipt_banner, items.size()));
        for (ReceiptItem item : items) {
            itemsContainer.addView(createItemRow(item));
        }
    }

    private View createItemRow(ReceiptItem item) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_receipt_row, itemsContainer, false);
        FrameLayout iconBox = row.findViewById(R.id.receipt_icon_box);
        TextView iconView = row.findViewById(R.id.tv_receipt_icon);
        EditText nameEdit = row.findViewById(R.id.et_receipt_name);
        TextView categoryView = row.findViewById(R.id.tv_receipt_category);
        TextView dateView = row.findViewById(R.id.tv_receipt_date);
        ImageButton dateButton = row.findViewById(R.id.btn_pick_receipt_date);
        ImageButton removeButton = row.findViewById(R.id.btn_remove_receipt_row);
        ImageButton editButton = row.findViewById(R.id.btn_edit_receipt_row);

        AppRepository.Category category = repository.getCategoryById(item.categoryId);

        iconView.setText(item.iconEmoji == null ? "*" : item.iconEmoji);
        if (category != null) {
            iconBox.setBackground(createRoundedDrawable(category.bgColor, 999));
            categoryView.setText(category.name);
        } else {
            categoryView.setText(getString(R.string.category_unknown));
        }

        nameEdit.setText(item.name);
        nameEdit.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameEdit.addTextChangedListener(SimpleTextWatcher.after(text -> item.name = text));

        dateView.setText(item.expiryDate == null
                ? getString(R.string.receipt_date_hint)
                : repository.formatExpiryDate(repository.toApiDate(item.expiryDate)));

        View.OnClickListener dateClick = v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(item.expiryDate == null ? new Date() : item.expiryDate);
            DatePickerDialog pickerDialog = new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(Calendar.YEAR, year);
                        selected.set(Calendar.MONTH, month);
                        selected.set(Calendar.DAY_OF_MONTH, day);
                        item.expiryDate = selected.getTime();
                        dateView.setText(repository.formatExpiryDate(repository.toApiDate(item.expiryDate)));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            pickerDialog.show();
        };

        dateButton.setOnClickListener(dateClick);
        row.findViewById(R.id.receipt_date_field).setOnClickListener(dateClick);

        removeButton.setOnClickListener(v -> {
            items.remove(item);
            refreshItems();
        });

        editButton.setOnClickListener(v -> {
            nameEdit.requestFocus();
            nameEdit.setSelection(nameEdit.getText() == null ? 0 : nameEdit.getText().length());
        });

        return row;
    }

    private void handleConfirm() {
        if (items.isEmpty()) {
            Toast.makeText(this, getString(R.string.receipt_no_items), Toast.LENGTH_SHORT).show();
            return;
        }
        for (ReceiptItem item : items) {
            if (item.name == null || item.name.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.product_name_required), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (ReceiptItem item : items) {
            if (item.expiryDate == null) {
                Toast.makeText(this, getString(R.string.receipt_expiry_required), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (ReceiptItem item : items) {
            repository.addProductManual(
                    item.categoryId,
                    item.name.trim(),
                    1,
                    repository.toApiDate(item.expiryDate)
            );
        }
        finish();
    }

    private static final class ReceiptItem {
        String name;
        String categoryId;
        String iconEmoji;
        Date expiryDate;
    }

    private static final class SimpleTextWatcher implements android.text.TextWatcher {
        private final java.util.function.Consumer<String> after;

        private SimpleTextWatcher(java.util.function.Consumer<String> after) {
            this.after = after;
        }

        static SimpleTextWatcher after(java.util.function.Consumer<String> after) {
            return new SimpleTextWatcher(after);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            after.accept(s == null ? "" : s.toString().trim());
        }
    }

    private void addSample(String name, String categoryId, String icon) {
        ReceiptItem item = new ReceiptItem();
        item.name = name;
        item.categoryId = categoryId;
        item.iconEmoji = icon;
        item.expiryDate = null;
        items.add(item);
    }

    private GradientDrawable createRoundedDrawable(String fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(parseColorSafe(fillColor, Color.WHITE));
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
}
