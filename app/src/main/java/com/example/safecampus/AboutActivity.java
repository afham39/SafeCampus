package com.example.safecampus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Link to GitHub
        TextView tvGithub = findViewById(R.id.tvGithub);
        tvGithub.setOnClickListener(v -> {
            // Replace with your actual GitHub link
            String githubUrl = "https://github.com/yourusername/SafeCampus";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl));
            startActivity(intent);
        });

        // Link to Admin/Contact (Optional: opens email)
        TextView tvAdmin = findViewById(R.id.tvAdmin); // Ensure this ID exists in your XML
        if (tvAdmin != null) {
            tvAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:admin@example.com"));
                startActivity(intent);
            });
        }
    }
}