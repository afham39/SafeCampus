package com.example.safecampus;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import com.google.firebase.firestore.*;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseFirestore db;
    private ListenerRegistration incidentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance(); // ✅ REQUIRED
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // C:/Users/user/AndroidStudioProjects/SafeCampus/app/src/main/java/com/example/safecampus/MapActivity.java

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;// 1. Fetch fixed locations from Firebase (replaces hardcoded LatLngs)
        fetchStaticLocations();

        // 2. Show live incident markers
        listenForIncidentUpdates();

        // 📍 User location setup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 16));
                }
            });
        }
    }

    // New method to fetch your campus locations from Firestore
    private void fetchStaticLocations() {
        db.collection("locations") // Assuming your collection is named "locations"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        GeoPoint gp = doc.getGeoPoint("position");
                        String name = doc.getString("name");
                        String type = doc.getString("type"); // e.g., "Facility", "Security"

                        if (gp != null && name != null) {
                            LatLng pos = new LatLng(gp.getLatitude(), gp.getLongitude());

                            // Customize marker color based on type if needed
                            float color = BitmapDescriptorFactory.HUE_BLUE;
                            if ("Security".equalsIgnoreCase(type)) color = BitmapDescriptorFactory.HUE_RED;

                            mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading locations", Toast.LENGTH_SHORT).show());
    }

    // 🔥 REAL-TIME INCIDENT MARKERS FROM FIREBASE
    private void listenForIncidentUpdates() {
        incidentListener = db.collection("incidents")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || mMap == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {

                        if (dc.getType() != DocumentChange.Type.ADDED) continue;

                        Object locationObj = dc.getDocument().get("location");
                        if (!(locationObj instanceof GeoPoint)) continue;

                        GeoPoint gp = (GeoPoint) locationObj;
                        String desc = dc.getDocument().getString("description");
                        String type = dc.getDocument().getString("type");

                        if (type == null) type = "Other";
                        if (desc == null) desc = "";

                        LatLng pos = new LatLng(gp.getLatitude(), gp.getLongitude());

                        float color;
                        switch (type) {
                            case "Emergency":
                                color = BitmapDescriptorFactory.HUE_RED;
                                break;
                            case "Accident":
                                color = BitmapDescriptorFactory.HUE_ORANGE;
                                break;
                            default:
                                color = BitmapDescriptorFactory.HUE_BLUE;
                        }

                        mMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(type)
                                .snippet(desc)
                                .icon(BitmapDescriptorFactory.defaultMarker(color))
                        );
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incidentListener != null) {
            incidentListener.remove();
        }
    }
}
