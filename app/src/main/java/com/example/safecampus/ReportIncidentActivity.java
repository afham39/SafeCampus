package com.example.safecampus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;


import java.util.HashMap;
import java.util.Map;

public class ReportIncidentActivity extends AppCompatActivity {

    Spinner spinnerType;
    EditText etDescription;
    Button btnSubmit;

    FirebaseFirestore db;
    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_incident);

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

        btnSubmit.setOnClickListener(v -> submitIncident());
    }

    private void submitIncident() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

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

                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                    Map<String, Object> incident = new HashMap<>();
                    incident.put("type", type);
                    incident.put("description", desc);
                    incident.put("location", geoPoint);
                    incident.put("status", "active"); // auto active
                    incident.put("timestamp", Timestamp.now()); // auto time


                    db.collection("incidents")
                            .add(incident)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(this, "Incident reported", Toast.LENGTH_SHORT).show();
                                finish(); // go back
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to report incident", Toast.LENGTH_SHORT).show()
                            );
                });
    }
}
