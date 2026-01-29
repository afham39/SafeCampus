package com.example.safecampus;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class IncidentAdapter extends ArrayAdapter<Incident> {

    public IncidentAdapter(Context context, List<Incident> incidents) {
        super(context, 0, incidents);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_incident, parent, false);
        }

        Incident incident = getItem(position);

        TextView tvEmoji = convertView.findViewById(R.id.tvEmoji);
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvSub = convertView.findViewById(R.id.tvSub);

        if (incident != null) {

            // 🔥 Emoji based on type
            String emoji = "📍";
            switch (incident.type) {
                case "Emergency":
                    emoji = "🚨";
                    break;
                case "Accident":
                    emoji = "🚗";
                    break;
                case "Security":
                    emoji = "🛡️";
                    break;
                case "Crime":
                    emoji = "🚓";
                    break;
                case "Damage":
                    emoji = "🛠️";
                    break;
            }

            tvEmoji.setText(emoji);
            tvTitle.setText(incident.type + " - " + incident.description);
            tvSub.setText(incident.time + " by " + incident.reportedBy);

            // 📍 Open Google Maps when clicked
            convertView.setOnClickListener(v -> {
                String uri = "geo:" + incident.lat + "," + incident.lng +
                        "?q=" + incident.lat + "," + incident.lng +
                        "(" + incident.type + ")";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                getContext().startActivity(intent);
            });
        }

        return convertView;
    }
}
