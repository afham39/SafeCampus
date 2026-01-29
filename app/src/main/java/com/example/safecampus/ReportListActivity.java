package com.example.safecampus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ReportListActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private ArrayList<Incident> incidentList;
    private IncidentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        listView = findViewById(R.id.reportListView);
        db = FirebaseFirestore.getInstance();

        incidentList = new ArrayList<>();
        adapter = new IncidentAdapter(this, incidentList);
        listView.setAdapter(adapter);

        fetchReportsFromFirebase();

        // CLICK → GOOGLE MAPS
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Incident incident = incidentList.get(position);

            String uri = "geo:" + incident.lat + "," + incident.lng +
                    "?q=" + incident.lat + "," + incident.lng + "(Incident Location)";

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });
    }

    private void fetchReportsFromFirebase() {
        db.collection("incidents")
                .whereEqualTo("status", "active") // 🔥 FILTER
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    incidentList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String desc = doc.getString("description");
                        String email = doc.getString("reportedBy");
                        String status = doc.getString("status");

                        double lat = doc.getGeoPoint("location").getLatitude();
                        double lng = doc.getGeoPoint("location").getLongitude();

                        String time = "";
                        if (doc.getTimestamp("timestamp") != null) {
                            time = doc.getTimestamp("timestamp").toDate().toString();
                        }

                        incidentList.add(new Incident(
                                type, desc, email, time, status, lat, lng
                        ));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show()
                );
    }
}
