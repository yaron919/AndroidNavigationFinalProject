package com.example.yashual.androidnavigationfinalproject;

public class Point {

    private double lat;
    private double lan;

    public Point(Double lat, Double lan){
        this.lat = lat;
        this.lan = lan;
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
