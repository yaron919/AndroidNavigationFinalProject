package com.example.yashual.androidnavigationfinalproject;

import android.location.Location;

public class SafePoint {

    private double lat;
    private double lan;

    public SafePoint(Double lat, Double lan){
        this.lat = lat;
        this.lan = lan;
    }

    public SafePoint(Location location){
        this.lan=location.getLongitude();
        this.lat=location.getLatitude();
    }
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLan() {
        return lan;
    }

    public void setLan(double lan) {
        this.lan = lan;
    }
}
