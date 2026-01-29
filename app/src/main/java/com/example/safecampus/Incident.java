package com.example.safecampus;

public class Incident {
    public String type;
    public String description;
    public String reportedBy;
    public String time;
    public String status;
    public double lat;
    public double lng;

    public Incident() {}

    public Incident(String type, String description, String reportedBy,
                    String time, String status, double lat, double lng) {
        this.type = type;
        this.description = description;
        this.reportedBy = reportedBy;
        this.time = time;
        this.status = status;
        this.lat = lat;
        this.lng = lng;
    }
}
