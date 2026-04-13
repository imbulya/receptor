package com.example.receptor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PlaceholderActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_NOTE = "extra_note";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_placeholder);

        View root = findViewById(R.id.placeholder_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int padding = Ui.dp(this, 20);
            v.setPadding(systemBars.left + padding, systemBars.top + padding, systemBars.right + padding, systemBars.bottom + padding);
            return insets;
        });

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String note = getIntent().getStringExtra(EXTRA_NOTE);

        TextView titleView = findViewById(R.id.tv_placeholder_title);
        TextView noteView = findViewById(R.id.tv_placeholder_note);
        ImageButton backButton = findViewById(R.id.btn_back);

        titleView.setText(title == null || title.isEmpty() ? getString(R.string.app_name) : title);
        noteView.setText(note == null || note.isEmpty() ? getString(R.string.placeholder_note) : note);

        backButton.setOnClickListener(v -> finish());
    }
}
