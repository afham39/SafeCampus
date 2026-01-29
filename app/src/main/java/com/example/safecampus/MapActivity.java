package com.example.safecampus;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;

import androidx.core.app.ActivityCompat;
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

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 1. Fetch fixed locations (Klinik, etc.) -> BLUE MARKERS
        fetchStaticLocations();

        // 2. Listen for incidents -> RED MARKERS
        listenForIncidentUpdates();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 15));
                }
            });
        }
    }

    // FIX FOR POINT 2: Fetching from "locations" collection using latitude/longitude doubles
    private void fetchStaticLocations() {
        db.collection("locations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Matching your Firebase screenshot fields exactly
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String name = doc.getString("name");
                        String type = doc.getString("type");

                        if (lat != null && lng != null) {
                            LatLng pos = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(name)
                                    .snippet(type)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        }
                    }
                });
    }

    // FIX FOR POINT 2: Fetching "incidents" using GeoPoint "location"
    private void listenForIncidentUpdates() {
        incidentListener = db.collection("incidents")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            GeoPoint gp = doc.getGeoPoint("location"); // Field name is "location"
                            String type = doc.getString("type");
                            String desc = doc.getString("description");

                            if (gp != null) {
                                LatLng pos = new LatLng(gp.getLatitude(), gp.getLongitude());
                                mMap.addMarker(new MarkerOptions()
                                        .position(pos)
                                        .title(type)
                                        .snippet(desc)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incidentListener != null) incidentListener.remove();
    }
}