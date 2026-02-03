package com.example.safecampus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);






        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        // Get header view
        View headerView = navigationView.getHeaderView(0);
        TextView tvNavUsername = headerView.findViewById(R.id.tvNavUsername);

        toolbar.setNavigationOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START)
        );

        // Set username
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "User");

        tvNavUsername.setText("Hi, " + username);


        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }


}
