package com.example.yashual.androidnavigationfinalproject;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
// classes needed to initialize map
import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;

import android.location.Location;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;


import android.support.annotation.NonNull;

import io.paperdb.Paper;

import android.util.Log;
// classes needed to launch navigation UI
import org.json.JSONException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback , NavigationView.OnNavigationItemSelectedListener{
    private GoogleMap mMap;
    // variables for adding location layer
    // variables for calculating and drawing a route
    private LatLng originPosition;
    private LatLng destinationPosition;
    private static final String TAG = "MainActivity";
    private ConnectionServer connectionServer;
    private Button navigateButton;
    private DatabaseHelper databaseHelper;
    private List<LatLng> safeList;
    private ImageButton languageButton;
    private Switch warSwitch;
    private DrawerLayout mDrawerLayout;
    private boolean mSlideState = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase,"en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        languageButton = findViewById(R.id.nav_language);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        warSwitch = navigationView.getMenu().findItem(R.id.nav_war_mode).getActionView().findViewById(R.id.warSwitch);
        navigateButton = findViewById(R.id.navigateButton);
        this.databaseHelper = new DatabaseHelper(this);
        //Set default lang
        Paper.init(this);
        //English is default
        String language = Paper.book().read("language");
        if(language == null)
            Paper.book().write("language","en");
        if(Paper.book().read("sound") == null)
            Paper.book().write("sound","True");

        View bar = findViewById(R.id.include_bar);
        ImageButton imageButton = bar.findViewById(R.id.nav_view_btn);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        imageButton.setOnClickListener(v -> mDrawerLayout.openDrawer(GravityCompat.START));

        updateView(Paper.book().read("language"));

        this.connectionServer = new ConnectionServer(this);
        registerPhoneToServer();
//        if (!GPSService.state) {
//            Intent i = new Intent(getApplicationContext(), GPSService.class);
//            startService(i);
//        }
//        languageButton.setOnClickListener(v -> {
//            changeLocale();
//        });
        navigateButton.setOnClickListener(v -> {
            SafePoint destSafePoint = databaseHelper.getNearestSafeLocation(safeList,new SafePoint(getLastBestLocation()));
            LatLng destLatLng = new LatLng(destSafePoint.getLat(), destSafePoint.getLan());
//            List<LatLng> points = new ArrayList<>();
            Log.d(TAG, "onCreate: lat"+destLatLng.latitude+" lan:"+destLatLng.longitude);
//            points.add(destLatLng);
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra("destLatLng", destLatLng);
            startActivity(intent);
        });
//        GPSService.isWar = false;
        Intent intent = new Intent(getApplicationContext(), GPSService.class);
        startService(intent);
        warSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GPSService.isWar = isChecked;
            Intent i = new Intent(getApplicationContext(), GPSService.class);
            stopService(i);
            startService(i);
        });
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void registerPhoneToServer() {
        String unique_id = Paper.book().read("unique_id");

        Log.e(TAG, "onCreate: results unique_id: "+unique_id );
        if (unique_id == null){
            Log.d(TAG, "onCreate: unique_id: "+unique_id);
            try {
                connectionServer.registerOnServerMyPhoneId();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (!unique_id.equals(FirebaseInstanceId.getInstance().getToken()))
        {
            Log.d(TAG, "registerPhoneToServer: firebase instance change");
            connectionServer.updateFirebaseInstance(unique_id);
        }
    }

    private boolean validateDistanceToClosestPoint(LatLng currentLocation, LatLng destination, int time){
        double distance = databaseHelper.getDistanceBetweenTwoPoints(
                new SafePoint(currentLocation.latitude,currentLocation.longitude),
                new SafePoint(destination.latitude,destination.longitude));
        Log.d(TAG, "validateDistanceToClosestPoint: distance: "+distance);
        Log.d(TAG, "validateDistanceToClosestPoint: distanceTime:" +(time*2.5) + " time:"+time);
        Log.d(TAG, "validateDistanceToClosestPoint: rv: "+((time*2.5)>distance));
        return ((time*2.5)>distance);
    }

    private void showNoSafePointMessage(){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(R.string.instructions_headline);
        alertDialog.setMessage(getResources().getString(R.string.instructions));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
        final MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.to_the_point);
        mp.start();
    }

    private void updateView(String language) {
        LocaleHelper.setLocale(this,language);
    }

    private void changeLocale(){
        String current_lang = Locale.getDefault().getDisplayLanguage();
        Log.d(TAG, "current language:" + current_lang);
        switch(current_lang) {
            case("English"):
                Paper.book().write("language","iw");
                updateView((String)Paper.book().read("language"));
                break;
            case("עברית"):
                Paper.book().write("language","en");
                updateView((String)Paper.book().read("language"));
                break;
        }
        updateView((String)Paper.book().read("language"));
        ConnectionServer.UpdateLanguageInServer();
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void addSafeMarkerOnMap(List<LatLng> list) {
        Log.d(TAG, "addSafeMarkerOnMap: list:"+list.toString());
        this.safeList = list;
        for (LatLng latLng : list) {
            mMap.addMarker(new MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));
        }
    }

    public void checkIntent(){
        Log.d(TAG, "checkIntent: start function");
        if (getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude") &&
                getIntent().hasExtra("redAlertId") && getIntent().hasExtra("max_time_to_arrive_to_shelter")) {
            try{
                Log.d(TAG, "i got an intent");
                int alertId = Integer.parseInt(getIntent().getStringExtra("redAlertId"));
                double lat = Double.parseDouble(getIntent().getStringExtra("latitude"));
                double lan = Double.parseDouble(getIntent().getStringExtra("longitude"));
                int time = Integer.parseInt(getIntent().getStringExtra("max_time_to_arrive_to_shelter"));
                Log.d(TAG, "lat:"+lat+" lan:  "+lan);
                destinationPosition = new LatLng(lat, lan);
                if(validateDistanceToClosestPoint(originPosition, destinationPosition, time)){
                    startNavigation(destinationPosition, alertId,time); // example routing NEED TO ADD DB SEARCH FOR DEST
                }else
                    showNoSafePointMessage();
            }catch(Exception e){
                Log.e(TAG, "checkIntent: error in function");
            }
        }
    }

    private void startNavigation(LatLng destLatLng, int alertId,int timeToDistance) {
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra("destLatLng", destLatLng);
        intent.putExtra("AlertID", alertId);
        intent.putExtra("timeToDistance",timeToDistance);
        startActivity(intent);
//        showNoSafePointMessage();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_locations) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_war_mode) {
            warSwitch.setChecked(!warSwitch.isChecked());
        } else if (id == R.id.nav_language) {
            changeLocale();
        } else if (id == R.id.nav_areas) {
//            Intent intent = new Intent(this, MainActivity.class);
//            startActivity(intent);
        } else if (id == R.id.nav_sound) {
            if (Paper.book().read("sound").equals("True")) {
                Paper.book().write("sound", "False");
                Log.d(TAG, "onNavigationItemSelected: sounds False" );
            }
            else{
                Log.d(TAG, "onNavigationItemSelected: sounds True" );
                Paper.book().write("sound","True");
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
        originPosition = new LatLng(myLocation.getLatitude(),
                myLocation.getLongitude());
        com.google.android.gms.maps.model.CameraPosition myPosition = new CameraPosition.Builder()
                .target(originPosition).zoom(17).bearing(90).tilt(30).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(myPosition));
        checkIntent();
        connectionServer.getSafeLocation(originPosition.latitude, originPosition.longitude);
    }
}
