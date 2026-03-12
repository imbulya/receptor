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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ProductCategoriesActivity extends AppCompatActivity {

    private static final String[] EMOJI_OPTIONS = new String[]{"🥗", "🧁", "🥪", "🍜", "🥘", "🫕", "🥐", "🧆", "🍱", "🫙", "🍕", "🥩", "🧀", "🍓", "🥑"};

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

    private AppRepository repository;
    private GridLayout categoriesGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_categories);

        repository = AppRepository.getInstance();

        applyInsets();

        categoriesGrid = findViewById(R.id.categories_grid);
        ImageButton backButton = findViewById(R.id.btn_back_categories);
        ImageButton addButton = findViewById(R.id.fab_add_category);

        backButton.setOnClickListener(v -> finish());
        addButton.setOnClickListener(v -> showCreateCategorySheet());

        renderCategories();
    }

    private void applyInsets() {
        View root = findViewById(R.id.product_categories_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void renderCategories() {
        categoriesGrid.removeAllViews();
        List<AppRepository.Category> categories = repository.getVisibleCategories();

        for (AppRepository.Category category : categories) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_category_card, categoriesGrid, false);
            MaterialCardView card = cardView.findViewById(R.id.card_category_root);
            FrameLayout emojiBox = cardView.findViewById(R.id.category_emoji_box);
            TextView emojiView = cardView.findViewById(R.id.tv_category_emoji);
            TextView countView = cardView.findViewById(R.id.tv_category_count);
            TextView nameView = cardView.findViewById(R.id.tv_category_name);

            int count = repository.getInStockCategoryCount(category.id);

            emojiView.setText(category.emoji);
            nameView.setText(category.name);

            emojiBox.setBackground(createRoundedDrawable(category.bgColor, 14));

            if (count > 0) {
                countView.setText(String.valueOf(count));
                countView.setTextColor(parseColorSafe(category.textColor, Color.BLACK));
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

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
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
        GridLayout colorGrid = sheet.findViewById(R.id.grid_colors);
        FrameLayout previewEmojiBox = sheet.findViewById(R.id.preview_emoji_box);
        TextView previewEmoji = sheet.findViewById(R.id.tv_preview_emoji);
        TextView previewName = sheet.findViewById(R.id.tv_preview_name);
        Button createButton = sheet.findViewById(R.id.btn_create_category);

        final String[] selectedEmoji = {"🥗"};
        final ColorOption[] selectedColor = {COLOR_OPTIONS[3]};

        closeButton.setOnClickListener(v -> dialog.dismiss());

        populateEmojiOptions(emojiGrid, selectedEmoji, selectedColor, previewEmojiBox, previewEmoji, previewName, nameInput);
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

        createButton.setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            if (name.isEmpty()) {
                nameInput.setError(getString(R.string.category_name_required));
                return;
            }
            repository.addCategory(name, selectedEmoji[0], selectedColor[0].bgColor, selectedColor[0].textColor);
            dialog.dismiss();
            renderCategories();
        });

        dialog.show();
    }

    private void populateEmojiOptions(
            GridLayout emojiGrid,
            String[] selectedEmoji,
            ColorOption[] selectedColor,
            FrameLayout previewEmojiBox,
            TextView previewEmoji,
            TextView previewName,
            EditText nameInput
    ) {
        emojiGrid.removeAllViews();

        for (String emoji : EMOJI_OPTIONS) {
            TextView option = new TextView(this);
            option.setText(emoji);
            option.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            option.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(48);
            params.height = dp(48);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            option.setLayoutParams(params);

            boolean isSelected = emoji.equals(selectedEmoji[0]);
            option.setBackground(createSelectableCellDrawable(
                    isSelected ? selectedColor[0].bgColor : "#FAFAF8",
                    isSelected ? selectedColor[0].textColor : "#14000000",
                    12,
                    isSelected ? 2.5f : 1.2f
            ));

            option.setOnClickListener(v -> {
                selectedEmoji[0] = emoji;
                populateEmojiOptions(emojiGrid, selectedEmoji, selectedColor, previewEmojiBox, previewEmoji, previewName, nameInput);
                updatePreview(previewEmojiBox, previewEmoji, previewName, selectedEmoji[0], selectedColor[0], textOf(nameInput));
            });

            emojiGrid.addView(option);
        }
    }

    private void populateColorOptions(
            GridLayout emojiGrid,
            GridLayout colorGrid,
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
            option.setTextColor(parseColorSafe(colorOption.textColor, Color.BLACK));
            option.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(36);
            params.height = dp(36);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
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
                populateEmojiOptions(emojiGrid, selectedEmoji, selectedColor, previewEmojiBox, previewEmoji, previewName, nameInput);
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
        previewEmoji.setText(emoji);
        previewName.setText(name == null || name.trim().isEmpty() ? getString(R.string.category_name_default) : name.trim());
    }

    private GradientDrawable createRoundedDrawable(String fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(parseColorSafe(fillColor, Color.WHITE));
        return drawable;
    }

    private GradientDrawable createSelectableCellDrawable(String fillColor, String strokeColor, int radiusDp, float strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(parseColorSafe(fillColor, Color.WHITE));
        if (strokeDp > 0f) {
            drawable.setStroke(dp(strokeDp), parseColorSafe(strokeColor, Color.TRANSPARENT));
        }
        return drawable;
    }

    private int parseColorSafe(String value, int fallback) {
        try {
            return Color.parseColor(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
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