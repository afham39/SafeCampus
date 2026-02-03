package com.example.safecampus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.location.Geocoder;
import android.location.Address;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;




public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI
    private TextView tvWelcome, tvLocation;
    private Button btnOpenMap, btnReport, btnRefresh, btnIncidentList;

    // Map & Location
    private GoogleMap mMap;
    private Marker userMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration incidentListener;


    private static final int LOCATION_REQ = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);

        // Get header view
        View headerView = navigationView.getHeaderView(0);
        TextView tvNavUsername = headerView.findViewById(R.id.tvNavUsername);

        // Set username
        String username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = "User";
        }


        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .putString("username", username)
                .apply();

        tvNavUsername.setText("Hi, " + username);

        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_report) {
                startActivity(new Intent(this, ReportIncidentActivity.class));
            }
            else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            }
            else if (id == R.id.nav_list) {
                startActivity(new Intent(this, ReportListActivity.class));
            }
            else if (id == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            }
            else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });


        // ---------- UI ----------

        tvLocation = findViewById(R.id.tvLocation);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnReport = findViewById(R.id.btnReport);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnIncidentList = findViewById(R.id.btnIncidentList);




        // ---------- Firebase ----------
        db = FirebaseFirestore.getInstance();

        // ---------- Location ----------
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null && mMap != null) {
                    updateUserLocation(location);
                }
            }
        };




        // ---------- Map ----------
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.homeMap);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ---------- Buttons ----------
        btnOpenMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));

        btnReport.setOnClickListener(v ->
                startActivity(new Intent(this, ReportIncidentActivity.class)));

        btnRefresh.setOnClickListener(v -> requestSingleLocation());

        btnIncidentList.setOnClickListener(v ->
                startActivity(new Intent(this, ReportListActivity.class)));


        // ---------- Permission ----------
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQ
            );
        }
    }

    // =====================================================
    // MAP READY
    // =====================================================
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        requestSingleLocation();
        startLocationUpdates();
        listenForIncidentUpdates();
    }

    // =====================================================
    // LOCATION
    // =====================================================
    private void requestSingleLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && mMap != null) {
                        updateUserLocation(location);
                    } else {
                        // Fallback campus
                        LatLng campus = new LatLng(6.4480, 100.5083);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campus, 15));
                        tvLocation.setText("Location: Campus");
                    }
                });
    }

    private void updateUserLocation(Location location) {
        LatLng user = new LatLng(location.getLatitude(), location.getLongitude());

        // Update marker
        if (userMarker == null) {
            userMarker = mMap.addMarker(
                    new MarkerOptions()
                            .position(user)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            );
        } else {
            userMarker.setPosition(user);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(user, 15));

        // ===== UPDATE LOCATION TEXT =====
        setLocationText(location);
    }


    private void setLocationText(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String area = address.getSubLocality();
                String city = address.getLocality();
                String country = address.getCountryName();

                StringBuilder locationText = new StringBuilder("Location: ");

                if (area != null) locationText.append(area).append(", ");
                if (city != null) locationText.append(city).append(", ");
                if (country != null) locationText.append(country);

                tvLocation.setText(locationText.toString());
            } else {
                // fallback
                tvLocation.setText(
                        "Location: " +
                                location.getLatitude() + ", " +
                                location.getLongitude()
                );
            }

        } catch (Exception e) {
            tvLocation.setText(
                    "Location: " +
                            location.getLatitude() + ", " +
                            location.getLongitude()
            );
        }
    }




    private void startLocationUpdates() {
        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 5000
                ).build();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    getMainLooper()
            );
        }
    }

    // =====================================================
    // FIRESTORE INCIDENT MARKERS (LIVE)
    // =====================================================
    private void listenForIncidentUpdates() {
        if (db == null) return;

        // Listen untuk INCIDENTS (Marker Merah)
        db.collection("incidents").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            // Nota: Jika anda guna mMap.clear(), semua marker hilang.
            // Jadi kita lukis semula semua marker dari kedua-dua collection.
            drawAllMarkers();
        });

        // Listen untuk LOCATIONS (Marker Biru)
        db.collection("locations").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            drawAllMarkers();
        });
    }

    private void drawAllMarkers() {
        if (mMap == null) return;
        mMap.clear(); // Bersihkan peta

        // Redraw User Marker
        if (userMarker != null) {
            mMap.addMarker(new MarkerOptions().position(userMarker.getPosition()).title("Anda di sini").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        // Ambil Incidents (MERAH)
        db.collection("incidents").get().addOnSuccessListener(snapshots -> {
            for (DocumentSnapshot doc : snapshots) {
                GeoPoint gp = doc.getGeoPoint("location");
                if (gp != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(gp.getLatitude(), gp.getLongitude()))
                            .title(doc.getString("type"))
                            .snippet(doc.getString("description"))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                }
            }
        });

        // Ambil Static Locations - Klinik/PTAR (BIRU)
        db.collection("locations").get().addOnSuccessListener(snapshots -> {
            for (DocumentSnapshot doc : snapshots) {
                Double lat = doc.getDouble("latitude");
                Double lng = doc.getDouble("longitude");
                if (lat != null && lng != null) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(doc.getString("name"))
                            .snippet(doc.getString("type"))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }
            }
        });
    }


    // =====================================================
    // LIFECYCLE
    // =====================================================
    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incidentListener != null) {
            incidentListener.remove();
        }
    }

    // =====================================================
    // PERMISSION RESULT
    // =====================================================
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQ &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            requestSingleLocation();
        }
    }
}
