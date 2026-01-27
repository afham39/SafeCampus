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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng KafetariaUitm = new LatLng(2.22495, 102.45743);
        LatLng securityPost = new LatLng(2.22239, 102.45335);
        LatLng clinic = new LatLng(2.22553, 102.45476);
        LatLng PTAR = new LatLng(2.22753, 102.45576);


        // 🔵 Show live incident markers
        listenForIncidentUpdates();

        // 📍 User location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                LatLng pos;
                if (location != null) {
                    LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 16));
                }

//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 17));
            });
        }


        mMap.addMarker(new MarkerOptions()
                .position(KafetariaUitm)
                .title("Kafetaria Uitm")
                .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions()
                .position(securityPost)
                .title("Security Post")
                .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_RED)));

        mMap.addMarker(new MarkerOptions()
                .position(clinic)
                .title("Campus Clinic"));

        mMap.addMarker(new MarkerOptions()
                .position(PTAR)
                .title("Libary"));


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(securityPost, 16));
    }




    // 🔥 REAL-TIME INCIDENT MARKERS FROM FIREBASE
    private void listenForIncidentUpdates() {
        incidentListener = db.collection("incidents")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {

                        GeoPoint gp = dc.getDocument().getGeoPoint("location");
                        String type = dc.getDocument().getString("type");
                        String desc = dc.getDocument().getString("description");

                        if (gp == null) continue;

                        LatLng pos = new LatLng(gp.getLatitude(), gp.getLongitude());

                        float color;
                        if ("Emergency".equals(type)) {
                            color = BitmapDescriptorFactory.HUE_RED;
                        } else if ("Accident".equals(type)) {
                            color = BitmapDescriptorFactory.HUE_ORANGE;
                        } else {
                            color = BitmapDescriptorFactory.HUE_BLUE;
                        }

                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title(type)
                                    .snippet(desc)
                                    .icon(BitmapDescriptorFactory.defaultMarker(color)));

                            Toast.makeText(MapActivity.this, "Incident loaded: " + desc, Toast.LENGTH_SHORT).show();
                        }
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
