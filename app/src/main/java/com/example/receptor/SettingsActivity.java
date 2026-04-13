package com.example.receptor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 14;
    private static final float GUIDE_ROTATION_EXPANDED = -90f;
    private static final float GUIDE_ROTATION_COLLAPSED = 90f;

    private TextView valueBadge;
    private TextView infoText;
    private SeekBar thresholdSeek;
    private AppRepository repository;
    private int currentDays;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        repository = AppRepository.getInstance(this);

        applyInsets();

        ImageButton backButton = findViewById(R.id.btn_back_settings);
        valueBadge = findViewById(R.id.tv_threshold_value);
        infoText = findViewById(R.id.tv_threshold_info);
        thresholdSeek = findViewById(R.id.sb_threshold);
        View updateRecipesButton = findViewById(R.id.btn_update_recipes);
        View clearDataButton = findViewById(R.id.btn_clear_data);

        backButton.setOnClickListener(v -> finish());
        if (updateRecipesButton != null) {
            updateRecipesButton.setOnClickListener(v -> {
                RecipeStore.refreshBaseRecipes(this);
                Toast.makeText(this, getString(R.string.settings_update_done), Toast.LENGTH_SHORT).show();
            });
        }
        if (clearDataButton != null) {
            clearDataButton.setOnClickListener(v -> showClearDataDialog());
        }

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                }
        );
        requestNotificationPermission();

        int stored = UserPreferences.getExpiryWarningDays(this, repository.getExpiryWarningDays());
        int initial = clamp(stored, MIN_DAYS, MAX_DAYS);
        repository.setExpiryWarningDays(initial);
        thresholdSeek.setMax(MAX_DAYS - MIN_DAYS);
        thresholdSeek.setProgress(initial - MIN_DAYS);
        updateThresholdUi(initial);

        thresholdSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = MIN_DAYS + progress;
                updateThresholdUi(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        setupGuideToggles();
    }

    private void applyInsets() {
        View root = findViewById(R.id.settings_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateThresholdUi(int value) {
        currentDays = value;
        repository.setExpiryWarningDays(value);
        UserPreferences.setExpiryWarningDays(this, value);
        ExpiryNotificationWorker.schedule(this);
        valueBadge.setText(getString(R.string.settings_day_value, value));
        infoText.setText(getString(R.string.settings_info_text, value));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_clear_confirm_title)
                .setMessage(R.string.settings_clear_confirm_message)
                .setPositiveButton(R.string.settings_clear_confirm_action, (dialog, which) -> {
                    repository.resetInventory();
        DataClient.clearShoppingList(this);
                    RecipeStore.clearCustomRecipes(this);
                    Toast.makeText(this, getString(R.string.settings_clear_done), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupGuideToggles() {
        GuideSection products = new GuideSection(
                findViewById(R.id.guide_products_header),
                findViewById(R.id.guide_products_body),
                findViewById(R.id.guide_products_chevron)
        );
        GuideSection recipes = new GuideSection(
                findViewById(R.id.guide_recipes_header),
                findViewById(R.id.guide_recipes_body),
                findViewById(R.id.guide_recipes_chevron)
        );
        GuideSection shopping = new GuideSection(
                findViewById(R.id.guide_shopping_header),
                findViewById(R.id.guide_shopping_body),
                findViewById(R.id.guide_shopping_chevron)
        );
        GuideSection home = new GuideSection(
                findViewById(R.id.guide_home_header),
                findViewById(R.id.guide_home_body),
                findViewById(R.id.guide_home_chevron)
        );

        GuideSection[] sections = new GuideSection[]{products, recipes, shopping, home};
        for (GuideSection section : sections) {
            if (section.header == null || section.body == null || section.chevron == null) {
                continue;
            }
            section.header.setOnClickListener(v -> toggleGuideSection(section, sections));
            updateGuideChevron(section, section.body.getVisibility() == View.VISIBLE);
        }
    }

    private void toggleGuideSection(GuideSection target, GuideSection[] sections) {
        boolean expanding = target.body.getVisibility() != View.VISIBLE;
        for (GuideSection section : sections) {
            if (section.header == null || section.body == null || section.chevron == null) {
                continue;
            }
            boolean show = section == target && expanding;
            section.body.setVisibility(show ? View.VISIBLE : View.GONE);
            updateGuideChevron(section, show);
        }
    }

    private void updateGuideChevron(GuideSection section, boolean expanded) {
        if (section.chevron == null) {
            return;
        }
        section.chevron.setRotation(expanded ? GUIDE_ROTATION_EXPANDED : GUIDE_ROTATION_COLLAPSED);
    }

    private static final class GuideSection {
        final View header;
        final View body;
        final View chevron;

        GuideSection(View header, View body, View chevron) {
            this.header = header;
            this.body = body;
            this.chevron = chevron;
        }
    }
}
