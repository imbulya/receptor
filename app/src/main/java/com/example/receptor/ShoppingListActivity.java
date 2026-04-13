package com.example.receptor;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ShoppingListActivity extends AppCompatActivity {

    private AppRepository repository;
    private LinearLayout itemsContainer;
    private TextView countChip;
    private SwipeRefreshLayout swipeRefresh;

    private final List<DataClient.ShoppingItem> items = new ArrayList<>();
    private final List<DataClient.CategoryInfo> categories = new ArrayList<>();
    private final List<AppRepository.Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);

        repository = AppRepository.getInstance(this);
        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_shopping);
        ImageButton addButton = findViewById(R.id.fab_add_shopping);
        itemsContainer = findViewById(R.id.shopping_container);
        countChip = findViewById(R.id.tv_shopping_count);
        swipeRefresh = findViewById(R.id.swipe_refresh_shopping);

        backButton.setOnClickListener(v -> finish());
        addButton.setOnClickListener(v -> showAddDialog());

        loadAllData();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadAllData();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllData();
    }

    private void applyInsets() {
        View root = findViewById(R.id.shopping_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadAllData() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        loadCategories();
        loadProducts();
        loadItems();
    }

    private void loadItems() {
        DataClient.fetchShoppingItems(new DataClient.Callback<List<DataClient.ShoppingItem>>() {
            @Override
            public void onSuccess(List<DataClient.ShoppingItem> data) {
                items.clear();
                if (data != null) {
                    items.addAll(data);
                }
                renderItems();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onError(Exception error) {
                items.clear();
                renderItems();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void loadCategories() {
        DataClient.fetchProductCategories(new DataClient.Callback<List<DataClient.CategoryInfo>>() {
            @Override
            public void onSuccess(List<DataClient.CategoryInfo> data) {
                categories.clear();
                if (data != null) {
                    categories.addAll(data);
                }
            }

            @Override
            public void onError(Exception error) {
                categories.clear();
            }
        });
    }

    private void loadProducts() {
        DataClient.fetchAllProducts(new DataClient.Callback<List<AppRepository.Product>>() {
            @Override
            public void onSuccess(List<AppRepository.Product> data) {
                allProducts.clear();
                if (data != null) {
                    allProducts.addAll(data);
                }
            }

            @Override
            public void onError(Exception error) {
                allProducts.clear();
            }
        });
    }

    private void renderItems() {
        countChip.setText(getString(R.string.shopping_count, items.size()));
        itemsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (DataClient.ShoppingItem item : items) {
            View row = inflater.inflate(R.layout.item_shopping_row, itemsContainer, false);
            TextView nameView = row.findViewById(R.id.tv_shopping_name);
            TextView qtyView = row.findViewById(R.id.tv_shopping_qty);
            FrameLayout checkBox = row.findViewById(R.id.shopping_check_box);
            ImageButton deleteButton = row.findViewById(R.id.btn_delete_item);
            View root = row.findViewById(R.id.shopping_item_root);

            nameView.setText(item.name);
            qtyView.setText(item.quantity);

            checkBox.setOnClickListener(v -> showPurchaseDialog(item));
            if (root != null) {
                root.setOnClickListener(v -> showEditDialog(item));
            }
            deleteButton.setOnClickListener(v -> {
                deleteItem(item);
            });

            itemsContainer.addView(row);
        }
    }

    private void deleteItem(DataClient.ShoppingItem item) {
        DataClient.deleteShoppingItem(item.id, new DataClient.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                loadItems();
            }

            @Override
            public void onError(Exception error) {
            }
        });
    }

    private void showPurchaseDialog(DataClient.ShoppingItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_purchase_confirm, null);
        dialog.setContentView(sheet);

        TextView titleView = sheet.findViewById(R.id.tv_purchase_title);
        TextView noteView = sheet.findViewById(R.id.tv_purchase_note);
        TextView dateView = sheet.findViewById(R.id.tv_purchase_date);
        TextView categoryLabel = sheet.findViewById(R.id.tv_purchase_category_label);
        View categoryScroll = sheet.findViewById(R.id.hs_purchase_categories);
        LinearLayout categoryContainer = sheet.findViewById(R.id.layout_purchase_categories);
        Button cancelButton = sheet.findViewById(R.id.btn_cancel_purchase);
        Button confirmButton = sheet.findViewById(R.id.btn_confirm_purchase);

        titleView.setText(getString(R.string.shopping_purchased));
        noteView.setText(getString(R.string.shopping_purchased_note, item.name));

        final Date[] selectedDate = {null};
        final String[] selectedCategoryId = {null};

        AppRepository.Product existingProduct = findProductByName(item.name);
        categoryLabel.setVisibility(View.VISIBLE);
        categoryScroll.setVisibility(View.VISIBLE);
        setupCategoryChips(categoryContainer, selectedCategoryId, existingProduct == null ? null : existingProduct.categoryId);

        View.OnClickListener dateClick = v -> {
            Calendar calendar = Calendar.getInstance();
            if (selectedDate[0] != null) {
                calendar.setTime(selectedDate[0]);
            }
            DatePickerDialog pickerDialog = new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(Calendar.YEAR, year);
                        picked.set(Calendar.MONTH, month);
                        picked.set(Calendar.DAY_OF_MONTH, day);
                        selectedDate[0] = picked.getTime();
                        dateView.setText(repository.formatExpiryDate(repository.toApiDate(selectedDate[0])));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            pickerDialog.show();
        };

        dateView.setOnClickListener(dateClick);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        confirmButton.setOnClickListener(v -> {
            if (selectedDate[0] == null) {
                Toast.makeText(this, getString(R.string.shopping_date_required), Toast.LENGTH_SHORT).show();
                return;
            }
            int quantity = parseQuantity(item.quantity);
            String expiryDate = repository.toApiDate(selectedDate[0]);

            String categoryId = selectedCategoryId[0];
            if (categoryId == null || categoryId.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.shopping_category_required), Toast.LENGTH_SHORT).show();
                return;
            }

            DataClient.addProductManual(categoryId, item.name, quantity, expiryDate, new DataClient.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    DataClient.deleteShoppingItem(item.id, new DataClient.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean data) {
                            loadItems();
                            dialog.dismiss();
                            Toast.makeText(ShoppingListActivity.this, getString(R.string.shopping_added_to_products), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception error) {
                            dialog.dismiss();
                        }
                    });
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showAddDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null);
        dialog.setContentView(sheet);

        EditText nameEdit = sheet.findViewById(R.id.et_add_name);
        EditText qtyEdit = sheet.findViewById(R.id.et_add_qty);
        TextView titleView = sheet.findViewById(R.id.tv_add_shopping_title);
        Button addButton = sheet.findViewById(R.id.btn_add_to_list);

        if (titleView != null) {
            titleView.setText(getString(R.string.add_to_list));
        }

        addButton.setOnClickListener(v -> {
            String name = nameEdit.getText() == null ? "" : nameEdit.getText().toString().trim();
            String qty = qtyEdit.getText() == null ? "" : qtyEdit.getText().toString().trim();
            if (name.isEmpty()) {
                nameEdit.setError(getString(R.string.product_name_required));
                return;
            }
            if (qty.isEmpty()) {
                qtyEdit.setError(getString(R.string.shopping_qty_required));
                return;
            }
            DataClient.addShoppingItem(name, qty, new DataClient.Callback<DataClient.ShoppingItem>() {
                @Override
                public void onSuccess(DataClient.ShoppingItem data) {
                    dialog.dismiss();
                    loadItems();
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showEditDialog(DataClient.ShoppingItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_add_shopping_item, null);
        dialog.setContentView(sheet);

        TextView titleView = sheet.findViewById(R.id.tv_add_shopping_title);
        EditText nameEdit = sheet.findViewById(R.id.et_add_name);
        EditText qtyEdit = sheet.findViewById(R.id.et_add_qty);
        Button addButton = sheet.findViewById(R.id.btn_add_to_list);

        if (titleView != null) {
            titleView.setText(getString(R.string.shopping_edit_title));
        }
        addButton.setText(getString(R.string.save));

        nameEdit.setText(item.name);
        qtyEdit.setText(item.quantity);

        addButton.setOnClickListener(v -> {
            String name = nameEdit.getText() == null ? "" : nameEdit.getText().toString().trim();
            String qty = qtyEdit.getText() == null ? "" : qtyEdit.getText().toString().trim();
            if (name.isEmpty()) {
                nameEdit.setError(getString(R.string.product_name_required));
                return;
            }
            if (qty.isEmpty()) {
                qtyEdit.setError(getString(R.string.shopping_qty_required));
                return;
            }
            DataClient.updateShoppingItem(item.id, name, qty, null, new DataClient.Callback<DataClient.ShoppingItem>() {
                @Override
                public void onSuccess(DataClient.ShoppingItem data) {
                    dialog.dismiss();
                    loadItems();
                }

                @Override
                public void onError(Exception error) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private int parseQuantity(String raw) {
        try {
            String digits = raw.replaceAll("[^0-9]", "");
            int value = Integer.parseInt(digits);
            return Math.max(1, value);
        } catch (Exception ignored) {
            return 1;
        }
    }

    private AppRepository.Product findProductByName(String name) {
        if (name == null) {
            return null;
        }
        for (AppRepository.Product product : allProducts) {
            if (product.name != null && product.name.equalsIgnoreCase(name.trim())) {
                return product;
            }
        }
        return null;
    }

    private void setupCategoryChips(LinearLayout container, String[] selectedCategoryId, String preferredCategoryId) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        List<DataClient.CategoryInfo> categoryInfos = new ArrayList<>(categories);
        List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < categoryInfos.size(); i++) {
            AppRepository.Category category = categoryInfos.get(i).category;
            TextView chip = new TextView(this);
            chip.setBackgroundResource(R.drawable.bg_chip_selector);
            chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
            chip.setText(category.emoji + " " + category.name);
            chip.setTextSize(14f);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setTag(category.id);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                params.leftMargin = Ui.dp(this, 8);
            }
            chip.setLayoutParams(params);
            chips.add(chip);
            container.addView(chip);
        }

        int selectedIndex = 0;
        if (preferredCategoryId != null) {
            for (int i = 0; i < chips.size(); i++) {
                Object tag = chips.get(i).getTag();
                if (tag != null && preferredCategoryId.equals(tag.toString())) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        for (int i = 0; i < chips.size(); i++) {
            TextView chip = chips.get(i);
            boolean selected = i == selectedIndex;
            updateCategoryChip(chip, selected);
            if (selected) {
                selectedCategoryId[0] = (String) chip.getTag();
            }
            chip.setOnClickListener(v -> {
                for (TextView otherChip : chips) {
                    updateCategoryChip(otherChip, otherChip == chip);
                }
                selectedCategoryId[0] = (String) chip.getTag();
            });
        }
    }

    private void updateCategoryChip(TextView chip, boolean selected) {
        chip.setSelected(selected);
        chip.setTextColor(getColor(selected ? R.color.section_orange : R.color.text_primary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

}






