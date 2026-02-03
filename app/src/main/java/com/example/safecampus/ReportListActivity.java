package com.example.safecampus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.widget.SearchView;



import java.util.ArrayList;

public class ReportListActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private ArrayList<Incident> incidentList;
    private IncidentAdapter adapter;


    ArrayList<Incident> fullList = new ArrayList<>();

    private androidx.appcompat.widget.SearchView searchView;
    private Spinner filterSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        searchView = findViewById(R.id.searchView);
        filterSpinner = findViewById(R.id.filterSpinner);

        // Toolbar drawer
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        // Filter options
        String[] filters = {"All", "Emergency", "Accident", "Security", "Crime", "Damage"};
        filterSpinner.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        filters)
        );


        // Header username
        View headerView = navigationView.getHeaderView(0);
        TextView tvNavUsername = headerView.findViewById(R.id.tvNavUsername);
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "User");
        tvNavUsername.setText("Hi, " + username);

        toolbar.setNavigationOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportIncidentActivity.class));
            } else if (id == R.id.nav_list) {
                drawerLayout.closeDrawers(); // already here
            } else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        listView = findViewById(R.id.reportListView);
        db = FirebaseFirestore.getInstance();
        incidentList = new ArrayList<>();
        adapter = new IncidentAdapter(this, incidentList);
        listView.setAdapter(adapter);

        fetchReportsFromFirebase();

        // Open Google Maps on click
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Incident incident = incidentList.get(position);
            String uri = "geo:" + incident.lat + "," + incident.lng +
                    "?q=" + incident.lat + "," + incident.lng;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) {
                applyFilters();
                return true;
            }
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

    }

    private void fetchReportsFromFirebase() {
        db.collection("incidents")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {
                    fullList.clear();
                    for (DocumentSnapshot doc : query) {

                        if (doc.getGeoPoint("location") == null) continue;

                        fullList.add(new Incident(
                                doc.getString("type"),
                                doc.getString("description"),
                                doc.getString("reportedBy"),
                                doc.getTimestamp("timestamp") != null
                                        ? doc.getTimestamp("timestamp").toDate().toString()
                                        : "",
                                doc.getString("status"),
                                doc.getGeoPoint("location").getLatitude(),
                                doc.getGeoPoint("location").getLongitude()
                        ));
                    }
                    applyFilters();
                });
    }

    private void applyFilters() {
        String query = searchView.getQuery().toString().toLowerCase();
        String selectedType = filterSpinner.getSelectedItem().toString();

        incidentList.clear();

        for (Incident i : fullList) {
            boolean matchText =
                    i.type.toLowerCase().contains(query) ||
                            i.description.toLowerCase().contains(query);

            boolean matchType =
                    selectedType.equals("All") || i.type.equalsIgnoreCase(selectedType);

            if (matchText && matchType) {
                incidentList.add(i);
            }
        }
        adapter.notifyDataSetChanged();
    }


}

