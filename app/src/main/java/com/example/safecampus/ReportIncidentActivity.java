package com.example.safecampus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;

import android.location.Geocoder;
import android.location.Address;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import android.view.View;




import java.util.HashMap;
import java.util.Map;

public class ReportIncidentActivity extends AppCompatActivity implements OnMapReadyCallback{

    Spinner spinnerType;
    EditText etDescription;
    Button btnSubmit;

    FirebaseFirestore db;
    FusedLocationProviderClient fusedLocationClient;

    GoogleMap mMap;
    TextView tvLocation, tvTime;
    ProgressBar progressSubmit;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_incident);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        progressSubmit = findViewById(R.id.progressSubmit);


// Header username
        View headerView = navigationView.getHeaderView(0);
        TextView tvNavUsername = headerView.findViewById(R.id.tvNavUsername);

// Load saved username (important fix)
        String username = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("username", "User");

        tvNavUsername.setText("Hi, " + username);

// Open drawer
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

// Navigation actions
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_report) {
                drawerLayout.closeDrawers(); // already here
            } else if (id == R.id.nav_list) {
                startActivity(new Intent(this, ReportListActivity.class));
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


        spinnerType = findViewById(R.id.spinnerType);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Spinner options
        String[] types = {"Emergency", "Accident", "Security" , "Crime", "Damage"};
        spinnerType.setAdapter(
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        types)
        );

        tvLocation = findViewById(R.id.tvLocation);
        tvTime = findViewById(R.id.tvTime);

// Show current date & time
        String currentTime = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        ).format(new Date());
        tvTime.setText("Time: " + currentTime);

// Map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);


        btnSubmit.setOnClickListener(v -> submitIncident());


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) return;

                    LatLng userLatLng = new LatLng(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("You are here"));

                    mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                    );

                    // Get readable address
                    showAddress(location.getLatitude(), location.getLongitude());
                });
    }

    private void showAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                tvLocation.setText("Location: " + address.getAddressLine(0));
            }
        } catch (Exception e) {
            tvLocation.setText("Location: Unable to detect");
        }
    }



    private void submitIncident() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = user.getEmail();

        btnSubmit.setEnabled(false);
        progressSubmit.setVisibility(View.VISIBLE);



        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location == null) {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String type = spinnerType.getSelectedItem().toString();
                    String desc = etDescription.getText().toString().trim();

                    if (desc.isEmpty()) {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GeoPoint geoPoint = new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    );

                    Map<String, Object> incident = new HashMap<>();
                    incident.put("type", type);
                    incident.put("description", desc);
                    incident.put("location", geoPoint);
                    incident.put("status", "active");
                    incident.put("timestamp", Timestamp.now());
                    incident.put("reportedBy", userEmail);

                    db.collection("incidents")
                            .add(incident)
                            .addOnSuccessListener(doc -> {
                                progressSubmit.setVisibility(View.GONE);
                                btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Incident reported", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressSubmit.setVisibility(View.GONE);
                                btnSubmit.setEnabled(true);
                                Toast.makeText(this, "Failed to report incident", Toast.LENGTH_SHORT).show();
                            });

                });
    }

}
