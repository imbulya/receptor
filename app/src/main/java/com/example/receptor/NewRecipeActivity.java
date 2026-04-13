package com.example.receptor;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewRecipeActivity extends AppCompatActivity {

    private static final int TIME_MIN = 5;
    private static final int TIME_MAX = 240;

    private LinearLayout ingredientsContainer;
    private LinearLayout stepsContainer;
    private ScrollView scrollView;
    private TextView timeLabel;
    private TextView timeValue;
    private SeekBar timeSeek;
    private EditText nameInput;
    private EditText photoInput;
    private ImageView photoPreview;
    private ActivityResultLauncher<String> photoPicker;
    private final Map<String, List<TextView>> chipGroups = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_recipe);

        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_new_recipe);
        TextView addIngredientButton = findViewById(R.id.btn_add_ingredient);
        TextView addStepButton = findViewById(R.id.btn_add_step);
        Button saveButton = findViewById(R.id.btn_save_recipe);
        Button pickPhotoButton = findViewById(R.id.btn_pick_photo);

        ingredientsContainer = findViewById(R.id.ingredients_form_container);
        stepsContainer = findViewById(R.id.steps_form_container);
        scrollView = findViewById(R.id.scroll_new_recipe);
        timeLabel = findViewById(R.id.tv_time_label);
        timeValue = findViewById(R.id.tv_time_value);
        timeSeek = findViewById(R.id.sb_recipe_time);
        nameInput = findViewById(R.id.et_recipe_name);
        photoInput = findViewById(R.id.et_recipe_photo);
        photoPreview = findViewById(R.id.iv_recipe_photo_preview);

        photoPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) {
                return;
            }
            if (photoInput != null) {
                photoInput.setText(uri.toString());
            }
            updatePhotoPreview(uri.toString());
        });

        setupChips(findViewById(R.id.new_recipe_root));

        backButton.setOnClickListener(v -> finish());
        addIngredientButton.setOnClickListener(v -> addIngredientRow());
        addStepButton.setOnClickListener(v -> addStepRow());
        saveButton.setOnClickListener(v -> saveRecipe());
        pickPhotoButton.setOnClickListener(v -> photoPicker.launch("image/*"));

        if (photoInput != null) {
            photoInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePhotoPreview(s == null ? "" : s.toString().trim());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            });
        }

        if (timeSeek != null) {
            timeSeek.setMax(TIME_MAX);
        }
        timeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeLabels(Math.max(TIME_MIN, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        updateTimeLabels(Math.max(TIME_MIN, timeSeek.getProgress()));
        addIngredientRow();
        addStepRow();
    }

    private void applyInsets() {
        View root = findViewById(R.id.new_recipe_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addIngredientRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_new_ingredient_row, ingredientsContainer, false);
        TextView numberView = row.findViewById(R.id.tv_new_ingredient_number);
        EditText nameInput = row.findViewById(R.id.et_new_ingredient_name);
        EditText unitInput = row.findViewById(R.id.et_new_ingredient_unit);
        ImageButton removeButton = row.findViewById(R.id.btn_remove_ingredient);

        if (unitInput != null && unitInput.getText().length() == 0) {
            unitInput.setText(getString(R.string.ingredient_unit_hint));
        }
        if (removeButton != null) {
            removeButton.setOnClickListener(v -> {
                ingredientsContainer.removeView(row);
                updateIngredientRows();
            });
        }

        numberView.setText(String.valueOf(ingredientsContainer.getChildCount() + 1));
        ingredientsContainer.addView(row);
        updateIngredientRows();
        ingredientsContainer.requestLayout();
        nameInput.requestFocus();
        scrollToBottom();
    }

    private void addStepRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_new_step_row, stepsContainer, false);
        TextView numberView = row.findViewById(R.id.tv_new_step_number);
        EditText stepInput = row.findViewById(R.id.et_new_step_text);
        ImageButton removeButton = row.findViewById(R.id.btn_remove_step);
        if (removeButton != null) {
            removeButton.setOnClickListener(v -> {
                stepsContainer.removeView(row);
                updateStepRows();
            });
        }
        if (stepInput != null) {
            stepInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    scrollToView(row);
                }
            });
            stepInput.setOnClickListener(v -> scrollToView(row));
        }

        numberView.setText(String.valueOf(stepsContainer.getChildCount() + 1));
        stepsContainer.addView(row);
        updateStepRows();
        stepsContainer.requestLayout();
        stepInput.requestFocus();
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollView == null) {
            return;
        }
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void scrollToView(View target) {
        if (scrollView == null || target == null) {
            return;
        }
        scrollView.post(() -> scrollView.smoothScrollTo(0, target.getBottom()));
    }

    private void updateTimeLabels(int minutes) {
        timeLabel.setText(getString(R.string.recipe_time_label_dynamic, minutes));
        timeValue.setText(getString(R.string.recipe_time_short, minutes));
    }

    private void updatePhotoPreview(String url) {
        if (photoPreview == null) {
            return;
        }
        if (url == null || url.trim().isEmpty()) {
            photoPreview.setVisibility(View.GONE);
            return;
        }
        photoPreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.bg_section_card)
                .error(R.drawable.bg_section_card)
                .centerCrop()
                .into(photoPreview);
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
                chip.setOnClickListener(v -> selectChip(groupChips, chip));
                updateChipVisual(chip);
            }
        }
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

    private String getSelectedChipText(String group) {
        List<TextView> groupChips = chipGroups.get(group);
        if (groupChips == null) {
            return "";
        }
        for (TextView chip : groupChips) {
            if (chip.isSelected()) {
                return chip.getText() == null ? "" : chip.getText().toString();
            }
        }
        return "";
    }

    private void saveRecipe() {
        String name = nameInput == null || nameInput.getText() == null
                ? ""
                : nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            if (nameInput != null) {
                nameInput.setError(getString(R.string.recipe_name_required));
            }
            return;
        }

        String cuisine = getSelectedChipText("cuisine");
        String type = getSelectedChipText("dish");
        int timeMinutes = Math.max(TIME_MIN, timeSeek == null ? TIME_MIN : timeSeek.getProgress());
        String imageUrl = photoInput != null && photoInput.getText() != null
                ? photoInput.getText().toString().trim()
                : "";
        if (imageUrl.isEmpty()) {
            imageUrl = RecipeStore.buildAutoImageUrl(name, cuisine, type);
        }

        List<RecipeStore.Ingredient> ingredients = new ArrayList<>();
        if (ingredientsContainer != null) {
            for (int i = 0; i < ingredientsContainer.getChildCount(); i++) {
                View row = ingredientsContainer.getChildAt(i);
                EditText nameInput = row.findViewById(R.id.et_new_ingredient_name);
                EditText qtyInput = row.findViewById(R.id.et_new_ingredient_qty);
                EditText unitInput = row.findViewById(R.id.et_new_ingredient_unit);
                String ingName = nameInput != null && nameInput.getText() != null
                        ? nameInput.getText().toString().trim()
                        : "";
                if (ingName.isEmpty()) {
                    continue;
                }
                String qty = qtyInput != null && qtyInput.getText() != null
                        ? qtyInput.getText().toString().trim()
                        : "";
                String unit = unitInput != null && unitInput.getText() != null
                        ? unitInput.getText().toString().trim()
                        : "";
                String amount = (qty + " " + unit).trim();
                ingredients.add(new RecipeStore.Ingredient(RecipeStore.formatIngredientName(ingName), amount));
            }
        }

        List<String> steps = new ArrayList<>();
        if (stepsContainer != null) {
            for (int i = 0; i < stepsContainer.getChildCount(); i++) {
                View row = stepsContainer.getChildAt(i);
                EditText stepInput = row.findViewById(R.id.et_new_step_text);
                String text = stepInput != null && stepInput.getText() != null
                        ? stepInput.getText().toString().trim()
                        : "";
                if (text.isEmpty()) {
                    continue;
                }
                steps.add(text);
            }
        }

        RecipeStore.Recipe recipe = new RecipeStore.Recipe(
                "user_" + System.currentTimeMillis(),
                name,
                imageUrl,
                cuisine,
                type,
                timeMinutes,
                ingredients,
                steps
        );
        RecipeStore.addCustomRecipe(this, recipe);
        Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateIngredientRows() {
        int count = ingredientsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = ingredientsContainer.getChildAt(i);
            TextView numberView = row.findViewById(R.id.tv_new_ingredient_number);
            if (numberView != null) {
                numberView.setText(String.valueOf(i + 1));
            }
            ImageButton removeButton = row.findViewById(R.id.btn_remove_ingredient);
            if (removeButton != null) {
                removeButton.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void updateStepRows() {
        int count = stepsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View row = stepsContainer.getChildAt(i);
            TextView numberView = row.findViewById(R.id.tv_new_step_number);
            EditText stepInput = row.findViewById(R.id.et_new_step_text);
            if (numberView != null) {
                numberView.setText(String.valueOf(i + 1));
            }
            if (stepInput != null) {
                stepInput.setHint(getString(R.string.step_hint_dynamic, i + 1));
            }
            ImageButton removeButton = row.findViewById(R.id.btn_remove_step);
            if (removeButton != null) {
                removeButton.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
            }
        }
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
}
