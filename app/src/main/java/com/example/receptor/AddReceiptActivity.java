package com.example.receptor;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
        Button addRowButton = findViewById(R.id.btn_add_row);
        Button confirmButton = findViewById(R.id.btn_confirm_receipt);
        itemsContainer = findViewById(R.id.receipt_items_container);
        emptyStateView = findViewById(R.id.tv_receipt_empty);

        backButton.setOnClickListener(v -> finish());

        scanButton.setOnClickListener(v -> {
            if (items.isEmpty()) {
                populateSampleItems();
            }
            refreshItems();
        });

        addRowButton.setOnClickListener(v -> {
            items.add(createEmptyItem());
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
        String[] sample = getResources().getStringArray(R.array.receipt_sample_items);
        for (String name : sample) {
            ReceiptItem item = createEmptyItem();
            item.name = name;
            items.add(item);
        }
    }

    private ReceiptItem createEmptyItem() {
        ReceiptItem item = new ReceiptItem();
        item.name = "";
        item.quantity = 1;
        item.expiryDate = repository.parseApiDate(repository.getDefaultExpiryDateForNewUnit());
        return item;
    }

    private void refreshItems() {
        itemsContainer.removeAllViews();
        if (items.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            return;
        }
        emptyStateView.setVisibility(View.GONE);
        for (ReceiptItem item : items) {
            itemsContainer.addView(createItemRow(item));
        }
    }

    private View createItemRow(ReceiptItem item) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_receipt_row, itemsContainer, false);
        EditText nameEdit = row.findViewById(R.id.et_receipt_name);
        EditText qtyEdit = row.findViewById(R.id.et_receipt_qty);
        TextView dateView = row.findViewById(R.id.tv_receipt_date);
        Button dateButton = row.findViewById(R.id.btn_pick_receipt_date);
        ImageButton removeButton = row.findViewById(R.id.btn_remove_receipt_row);

        nameEdit.setText(item.name);
        qtyEdit.setText(String.valueOf(item.quantity));
        dateView.setText(repository.formatExpiryDate(repository.toApiDate(item.expiryDate)));

        nameEdit.addTextChangedListener(SimpleTextWatcher.after(text -> item.name = text));
        qtyEdit.addTextChangedListener(SimpleTextWatcher.after(text -> {
            int value = 1;
            try {
                value = Integer.parseInt(text);
            } catch (Exception ignored) {
                value = 1;
            }
            if (value < 1) {
                value = 1;
            }
            item.quantity = value;
        }));

        dateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(item.expiryDate);
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
        });

        removeButton.setOnClickListener(v -> {
            items.remove(item);
            refreshItems();
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
            repository.addProductManual(
                    categoryId,
                    item.name.trim(),
                    item.quantity,
                    repository.toApiDate(item.expiryDate)
            );
        }
        finish();
    }

    private static final class ReceiptItem {
        String name;
        int quantity;
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
}
