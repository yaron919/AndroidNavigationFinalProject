package com.example.yashual.androidnavigationfinalproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private ImageView exitNavigationBtn;
    private LatLng destinationPosition;
    private DatabaseHelper databaseHelper;
    private int redAlertID;
    private boolean fromNotification = false;
    private int timeToDistance;
    private TextView addressTextView;
    private TextView distanceTextView;
    private TextView timerTextView;
    private LocationManager locationManager;
    private Polyline line;
    private CountDownTimer timer;
    private MediaPlayer mp ;
    private MediaPlayer sound2;
    private MediaPlayer sound3 ;
    private MediaPlayer sound4 ;
    private MediaPlayer sound5 ;
    private MediaPlayer sound6 ;
    private String headline = "";
    private boolean soundOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        updateView(Paper.book().read("language"));
        initialLocationManager();
        soundOn = Paper.book().read("sound").equals("True");
        headline = getResources().getString(R.string.instructions_headline);
        exitNavigationBtn = findViewById(R.id.exitNavigationBtn);
        addressTextView = findViewById(R.id.address_text_view);
        distanceTextView = findViewById(R.id.distance_text_view);
        timerTextView = findViewById(R.id.timer_text_view);
        databaseHelper = new DatabaseHelper(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mp = MediaPlayer.create(this, R.raw.to_the_point);
        sound2 = MediaPlayer.create(this, R.raw.two);
        sound3 = MediaPlayer.create(this, R.raw.three);
        sound4 = MediaPlayer.create(this, R.raw.four);
        sound5 = MediaPlayer.create(this, R.raw.five);
        sound6 = MediaPlayer.create(this, R.raw.six);
        exitNavigationBtn.setOnClickListener(v -> finish());
        Intent intent = getIntent();
        if (getIntent().hasExtra("destLng") && getIntent().hasExtra("destLat")){
            destinationPosition = new LatLng(intent.getDoubleExtra("destLat", 31.900051),
                    intent.getDoubleExtra("destLng", 34.806620));
        }
        if (getIntent().hasExtra("AlertID") && getIntent().hasExtra("timeToDistance")) {
            timeToDistance = intent.getIntExtra("timeToDistance", 99);
            redAlertID = intent.getIntExtra("AlertID", -1);
            if (redAlertID != -1)
                fromNotification = true;
            startTimer(timeToDistance);
        }else{
            timerTextView.setText("");
        }
    }

    private void startTimer(int time){
        timer = new CountDownTimer(time*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeStr = "";
                int min = seconds / 60;
                if (min < 10)
                {
                    timeStr += "0";
                }
                timeStr += min + ":";
                int sec = seconds % 60;
                if (sec < 10){
                    timeStr += "0";
                }
                timeStr += sec;
                Log.d(TAG, "onTick: sound is "+ soundOn);
                if (soundOn){
                    if (seconds > 30) {
                        sound2.start();
                    }else if (seconds <= 30 && seconds >20){
                        sound4.start();
                    }else if (seconds <= 20 && seconds >15){
                        sound3.start();
                    }else if (seconds <= 15 && seconds >5){
                        sound6.start();
                    }else if (seconds <=5){
                        sound5.start();
                    }
                }
                timerTextView.setText(timeStr);
            }
            public void onFinish() {
                timerTextView.setText("");
                showNoSafePointMessage();
            }
        }.start();

    }
    private void showNoSafePointMessage(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(headline);
        alertDialog.setMessage(getResources().getString(R.string.instructions));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
        if (soundOn){
            final MediaPlayer mp = MediaPlayer.create(this, R.raw.to_the_point);
            mp.start();
        }
    }
    @SuppressLint("MissingPermission")
    private void initialLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(locationManager != null)
        {
            boolean gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkIsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(gpsIsEnabled)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
            else if(networkIsEnabled)
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
            else
            {
                //Show an error dialog that GPS is disabled.
            }
        }
        else
        {
            //Show a generic error dialog since LocationManager is null for some reason
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // TODO: Before enabling the My Location layer, you must request
        // location permission from the user. This sample does not include
        // a request for location permission.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        Location myLocation = getLastBestLocation();
        LatLng myLatLng = new LatLng(myLocation.getLatitude(),
                myLocation.getLongitude());
        distanceTextView.setText(""+getDistanceBetweenTwoPoints(myLatLng,destinationPosition));
        CameraPosition myPosition = new CameraPosition.Builder()
                .target(myLatLng).zoom(20).bearing(90).tilt(30).build();
        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(myPosition));
        mMap.addMarker(new MarkerOptions().
                position(destinationPosition).
                icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));
        line = mMap.addPolyline(new PolylineOptions().
                add(myLatLng).
                add(destinationPosition).
                width(5).
                color(Color.CYAN));
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(destinationPosition.latitude, destinationPosition.longitude, 1);
            addressTextView.setText(addresses.get(0).getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void updateView(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView(Paper.book().read("language"));
    }

    @Override
    public void onPause() {
        super.onPause();
//        if(timer != null){
//            timer.cancel();
//        }
        if(locationManager != null)
        {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(timer != null){
            timer.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    private Location getLastBestLocation() {
        LocationManager mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"No GPS - Turn on");
        }
        Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            return locationGPS;
        }
        else {
            return locationNet;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng locationLatLng = new LatLng(location.getLatitude(),location.getLongitude());
            int distance = getDistanceBetweenTwoPoints(locationLatLng,destinationPosition);
            if (distance<8){
                if (fromNotification)
                    ConnectionServer.Arrive(redAlertID);
                finish();
            }else {
                distanceTextView.setText("" + distance);
                line.remove();
                line = mMap.addPolyline(new PolylineOptions().
                        add(locationLatLng).
                        add(destinationPosition).
                        width(5).
                        color(Color.CYAN));
                CameraPosition myPosition = new CameraPosition.Builder()
                        .target(locationLatLng).zoom(20).bearing(90).tilt(30).build();
                mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(myPosition));
            }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
//        Toast.makeText(this, "status changed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
//        Toast.makeText(this, "provider enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
//        Toast.makeText(this, "provider disabled", Toast.LENGTH_SHORT).show();
    }

    public int getDistanceBetweenTwoPoints(LatLng p1, LatLng p2) {
        double R = 6371000; // m
        double dLat = Math.toRadians(p2.latitude - p1.latitude);
        double dLon = Math.toRadians(p2.longitude - p1.longitude);
        double lat1 = Math.toRadians(p1.latitude);
        double lat2 = Math.toRadians(p2.latitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
                * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = R * c;
        Log.d(TAG, "getDistanceBetweenTwoPoints: d = "+d);
        return d.intValue();
    }
}
