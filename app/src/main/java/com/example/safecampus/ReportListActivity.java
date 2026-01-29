package com.example.safecampus;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class ReportListActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseFirestore db;
    private ArrayList<String> reportList;
    private ArrayAdapter<String> adapter;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        // 1. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Link the ListView from XML
        listView = findViewById(R.id.reportListView);

        // 3. Setup the List and Adapter
        reportList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reportList);
        listView.setAdapter(adapter);

        // 4. Fetch the data
        fetchReportsFromFirebase();
    }

    private void fetchReportsFromFirebase() {
        db.collection("incidents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String desc = doc.getString("description");

                        // Combine type and description to show in the list
                        if (type != null && desc != null) {
                            reportList.add(type + "\n" + desc);
                        } else if (type != null) {
                            reportList.add(type);
                        }
                    }
                    // Refresh the list to show the new data
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                });
    }
}