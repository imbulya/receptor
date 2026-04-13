package com.example.receptor;

import android.content.Intent;
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
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ProductCategoriesActivity extends AppCompatActivity {

    private static final String[] EMOJI_OPTIONS = new String[]{};

    private static final ColorOption[] COLOR_OPTIONS = new ColorOption[]{
            new ColorOption("#FDD8E0", "#9E3050"),
            new ColorOption("#FDE8D2", "#A04010"),
            new ColorOption("#FFF0CC", "#806010"),
            new ColorOption("#D5EDD8", "#2D6E35"),
            new ColorOption("#D8EEF8", "#1A5C82"),
            new ColorOption("#EDD5F5", "#6A2A82"),
            new ColorOption("#D5F0F5", "#1A6A7A"),
            new ColorOption("#F5E8CC", "#7A5010")
    };

    private GridLayout categoriesGrid;
    private SwipeRefreshLayout swipeRefresh;
    private final List<DataClient.CategoryInfo> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_categories);

        applyInsets();

        categoriesGrid = findViewById(R.id.categories_grid);
        swipeRefresh = findViewById(R.id.swipe_refresh_categories);
        ImageButton backButton = findViewById(R.id.btn_back_categories);
        ImageButton addButton = findViewById(R.id.fab_add_category);

        backButton.setOnClickListener(v -> finish());
        addButton.setOnClickListener(v -> showCreateCategorySheet());

        loadCategories();

        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                loadCategories();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void applyInsets() {
        View root = findViewById(R.id.product_categories_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadCategories() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        DataClient.fetchProductCategories(new DataClient.Callback<List<DataClient.CategoryInfo>>() {
            @Override
            public void onSuccess(List<DataClient.CategoryInfo> data) {
                categories.clear();
                if (data != null) {
                    categories.addAll(data);
                }
                renderCategories();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onError(Exception error) {
                categories.clear();
                renderCategories();
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
    }

    private void renderCategories() {
        categoriesGrid.removeAllViews();
        for (DataClient.CategoryInfo info : categories) {
            AppRepository.Category category = info.category;
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_category_card, categoriesGrid, false);
            MaterialCardView card = cardView.findViewById(R.id.card_category_root);
            FrameLayout emojiBox = cardView.findViewById(R.id.category_emoji_box);
            TextView emojiView = cardView.findViewById(R.id.tv_category_emoji);
            TextView countView = cardView.findViewById(R.id.tv_category_count);
            TextView nameView = cardView.findViewById(R.id.tv_category_name);

            int count = info.inStockCount;

            emojiView.setText(category.emoji);
            nameView.setText(category.name);

            emojiBox.setBackground(createRoundedDrawable(category.bgColor, 16));

            if (count > 0) {
                countView.setText(String.valueOf(count));
                countView.setTextColor(Ui.parseColorSafe(category.textColor, Color.BLACK));
                countView.setBackground(createRoundedDrawable(category.bgColor, 10));
                countView.setVisibility(View.VISIBLE);
            } else {
                countView.setVisibility(View.GONE);
            }

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductCategoryActivity.class);
                intent.putExtra(ProductCategoryActivity.EXTRA_CATEGORY_ID, category.id);
                startActivity(intent);
            });

            card.setOnLongClickListener(v -> {
                if (info.isSystem) {
                    return false;
                }
                showRemoveCategoryDialog(category);
                return true;
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = Ui.dp(this, 176);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1), Ui.dp(this, 1));
            cardView.setLayoutParams(params);

            categoriesGrid.addView(cardView);
        }
    }

    private void showCreateCategorySheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        dialog.setContentView(sheet);

        ImageButton closeButton = sheet.findViewById(R.id.sheet_close_btn);
        EditText nameInput = sheet.findViewById(R.id.input_category_name);
        GridLayout emojiGrid = sheet.findViewById(R.id.grid_emoji);
        LinearLayout colorGrid = sheet.findViewById(R.id.layout_colors);
        EditText emojiInput = sheet.findViewById(R.id.input_category_emoji);
        FrameLayout previewEmojiBox = sheet.findViewById(R.id.preview_emoji_box);
        TextView previewEmoji = sheet.findViewById(R.id.tv_preview_emoji);
        TextView previewName = sheet.findViewById(R.id.tv_preview_name);
        Button createButton = sheet.findViewById(R.id.btn_create_category);

        final String[] selectedEmoji = {""};
        final ColorOption[] selectedColor = {COLOR_OPTIONS[3]};

        closeButton.setOnClickListener(v -> dialog.dismiss());

        emojiGrid.setVisibility(View.GONE);
        populateColorOptions(emojiGrid, colorGrid, selectedEmoji, selectedColor, previewEmojiBox, previewEmoji, previewName, nameInput);
        updatePreview(previewEmojiBox, previewEmoji, previewName, selectedEmoji[0], selectedColor[0], nameInput.getText().toString());

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview(previewEmojiBox, previewEmoji, previewName, selectedEmoji[0], selectedColor[0], s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if (emojiInput != null) {
            emojiInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    selectedEmoji[0] = s == null ? "" : s.toString().trim();
                    updatePreview(previewEmojiBox, previewEmoji, previewName, selectedEmoji[0], selectedColor[0], textOf(nameInput));
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        createButton.setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            if (name.isEmpty()) {
                nameInput.setError(getString(R.string.category_name_required));
                return;
            }
            String emoji = selectedEmoji[0] == null || selectedEmoji[0].trim().isEmpty()
                    ? getString(R.string.emoji_placeholder)
                    : selectedEmoji[0].trim();
            DataClient.createProductCategory(name, emoji, selectedColor[0].bgColor, selectedColor[0].textColor,
                    new DataClient.Callback<DataClient.CategoryInfo>() {
                        @Override
                        public void onSuccess(DataClient.CategoryInfo data) {
                            dialog.dismiss();
                            loadCategories();
                        }

                        @Override
                        public void onError(Exception error) {
                            dialog.dismiss();
                        }
                    });
        });

        dialog.show();
    }

    private void showRemoveCategoryDialog(AppRepository.Category category) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_remove_category_confirm, null);
        dialog.setContentView(sheet);

        TextView message = sheet.findViewById(R.id.tv_remove_category_message);
        Button confirmButton = sheet.findViewById(R.id.btn_confirm_remove_category);
        Button cancelButton = sheet.findViewById(R.id.btn_cancel_remove_category);

        message.setText(getString(R.string.remove_category_message, category.name));

        confirmButton.setOnClickListener(v -> {
            DataClient.deleteProductCategory(category.id, new DataClient.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    dialog.dismiss();
                    loadCategories();
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

    

    private void populateColorOptions(
            GridLayout emojiGrid,
            LinearLayout colorGrid,
            String[] selectedEmoji,
            ColorOption[] selectedColor,
            FrameLayout previewEmojiBox,
            TextView previewEmoji,
            TextView previewName,
            EditText nameInput
    ) {
        colorGrid.removeAllViews();

        for (ColorOption colorOption : COLOR_OPTIONS) {
            TextView option = new TextView(this);
            option.setText(colorOption == selectedColor[0] ? "✓" : "");
            option.setGravity(Gravity.CENTER);
            option.setTextColor(Ui.parseColorSafe(colorOption.textColor, Color.BLACK));
            option.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36));
            params.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
            option.setLayoutParams(params);

            boolean isSelected = colorOption == selectedColor[0];
            option.setBackground(createSelectableCellDrawable(
                    colorOption.bgColor,
                    isSelected ? colorOption.textColor : "#00000000",
                    10,
                    isSelected ? 2.5f : 0f
            ));

            option.setOnClickListener(v -> {
                selectedColor[0] = colorOption;
                populateColorOptions(emojiGrid, colorGrid, selectedEmoji, selectedColor, previewEmojiBox, previewEmoji, previewName, nameInput);
                updatePreview(previewEmojiBox, previewEmoji, previewName, selectedEmoji[0], selectedColor[0], textOf(nameInput));
            });

            colorGrid.addView(option);
        }
    }

    private void updatePreview(
            FrameLayout previewEmojiBox,
            TextView previewEmoji,
            TextView previewName,
            String emoji,
            ColorOption color,
            String name
    ) {
        previewEmojiBox.setBackground(createRoundedDrawable(color.bgColor, 14));
        String safeEmoji = emoji == null || emoji.trim().isEmpty()
                ? getString(R.string.emoji_placeholder)
                : emoji.trim();
        previewEmoji.setText(safeEmoji);
        previewName.setText(name == null || name.trim().isEmpty() ? getString(R.string.category_name_default) : name.trim());
    }

    private GradientDrawable createRoundedDrawable(String fillColor, int radiusDp) {
        return Ui.roundedRect(this, fillColor, radiusDp, Color.WHITE);
    }

    private GradientDrawable createSelectableCellDrawable(String fillColor, String strokeColor, int radiusDp, float strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(Ui.dp(this, radiusDp));
        drawable.setColor(Ui.parseColorSafe(fillColor, Color.WHITE));
        if (strokeDp > 0f) {
            drawable.setStroke(Ui.dp(this, strokeDp), Ui.parseColorSafe(strokeColor, Color.TRANSPARENT));
        }
        return drawable;
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private static final class ColorOption {
        final String bgColor;
        final String textColor;

        ColorOption(String bgColor, String textColor) {
            this.bgColor = bgColor;
            this.textColor = textColor;
        }
    }
}


