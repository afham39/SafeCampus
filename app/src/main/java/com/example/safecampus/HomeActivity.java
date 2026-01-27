package com.example.safecampus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.*;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // UI
    private TextView tvWelcome, tvLocation;
    private Button btnOpenMap, btnReport, btnRefresh;

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

        // ---------- UI ----------
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLocation = findViewById(R.id.tvLocation);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnReport = findViewById(R.id.btnReport);
        btnRefresh = findViewById(R.id.btnRefresh);

        String username = getIntent().getStringExtra("username");
        tvWelcome.setText("Hello, " + username);

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
        LatLng user = new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );

        if (userMarker == null) {
            userMarker = mMap.addMarker(
                    new MarkerOptions()
                            .position(user)
                            .title("You are here")
            );
        } else {
            userMarker.setPosition(user);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(user, 17));
        tvLocation.setText("Your current location: GPS");
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

        incidentListener = db.collection("incidents")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || mMap == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        GeoPoint gp =
                                dc.getDocument().getGeoPoint("location");
                        String desc = dc.getDocument().getString("description");

                        if (gp == null) continue;

                        LatLng pos = new LatLng(
                                gp.getLatitude(),
                                gp.getLongitude()
                        );

                        mMap.addMarker( new MarkerOptions()
                                        .position(pos)
                                        .title(desc)
                                        .icon(BitmapDescriptorFactory
                                                .defaultMarker(
                                                        BitmapDescriptorFactory.HUE_RED))
                        );
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
