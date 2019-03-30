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
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
// classes needed to initialize map
import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.example.yashual.androidnavigationfinalproject.Service.LocationService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import android.location.Location;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
// classes needed to add a marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import io.paperdb.Paper;

import android.util.Log;
// classes needed to launch navigation UI

import org.json.JSONObject;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener, NavigationView.OnNavigationItemSelectedListener {
    private MapView mapView;
    // variables for adding location layer
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private Location originLocation;
    // variables for calculating and drawing a route
    private Point originPosition;
    private Point destinationPosition;
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
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
//        languageButton = findViewById(R.id.nav_language);
        mapView = findViewById(R.id.mapView);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        warSwitch = (Switch) navigationView.getMenu().findItem(R.id.nav_war_mode).getActionView().findViewById(R.id.warSwitch);
        navigateButton = findViewById(R.id.navigateButton);
        this.databaseHelper = new DatabaseHelper(this);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
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
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        updateView((String)Paper.book().read("language"));

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
            SafePoint destSafePoint = databaseHelper.getNearestSafeLocation(safeList,new SafePoint(originLocation));
            originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());
            LatLng originLatLng = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
            LatLng destLatLng = new LatLng(destSafePoint.getLat(), destSafePoint.getLan());
//            destinationPosition = Point.fromLngLat(destSafePoint.getLan(),destSafePoint.getLat());
            List<LatLng> points = new ArrayList<>();
            Log.d(TAG, "onCreate: lat"+destLatLng.getLatitude()+" lan:"+destLatLng.getLongitude());
            points.add(originLatLng);
            points.add(destLatLng);
            polyLineDraw(points);
//            startNavigation(originPosition, destinationPosition, -1,99);
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

    private boolean validateDistanceToClosestPoint(Point currentLocation, Point destination, int time){
        double distance = databaseHelper.getDistanceBetweenTwoPoints(
                new SafePoint(currentLocation.latitude(),currentLocation.longitude()),
                new SafePoint(destination.latitude(),destination.longitude()));
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
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        enableLocationComponent();
        checkIntent();
        connectionServer.getSafeLocation(this.originLocation.getLatitude(), this.originLocation.getLongitude());

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
            mapboxMap.addMarker(new MarkerOptions()
                                 .position(latLng));
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
                destinationPosition = Point.fromLngLat(lan,lat);
                originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());
                if(validateDistanceToClosestPoint(originPosition,destinationPosition,time)){
                    startNavigation(originPosition, destinationPosition, alertId,time); // example routing NEED TO ADD DB SEARCH FOR DEST
                }else
                    showNoSafePointMessage();
            }catch(Exception e){
                Log.e(TAG, "checkIntent: error in function");
            }
        }
    }

    private void startNavigation(Point originPosition, Point destinationPosition, int alertId,int timeToDistance) {
        Intent intent = new Intent(this,NavigationActivity.class);
        intent.putExtra("positionLon",originPosition.longitude());
        intent.putExtra("positionLat",originPosition.latitude());
        intent.putExtra("destinationLon",destinationPosition.longitude());
        intent.putExtra("destinationLat",destinationPosition.latitude());
        intent.putExtra("AlertID", alertId);
        intent.putExtra("timeToDistance",timeToDistance);
        startActivity(intent);
//        showNoSafePointMessage();
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setRenderMode(RenderMode.GPS);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS);
            /*locationComponent.addOnCameraTrackingChangedListener(new OnCameraTrackingChangedListener() {
                @Override
                public void onCameraTrackingDismissed() {
                    
                }

                @Override
                public void onCameraTrackingChanged(int currentMode) {
                    polyLineDraw(points)
                }
            });*/

            this.originLocation = getLastBestLocation();
            if (this.originLocation == null)
                this.originLocation = locationComponent.getLastKnownLocation();
            Log.d(TAG, "enableLocationComponent: originLocation: "+originLocation.getLongitude()+" "+originLocation.getLatitude());
            changeCameraLocation(this.originLocation);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
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

    private void changeCameraLocation(Location location){
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude())) // Sets the new camera position
                .zoom(12) // Sets the zoom
                .bearing(180) // Rotate the camera
                .tilt(30) // Set the camera tilt
                .build(); // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 7000);
    }
    private void polyLineDraw(List<LatLng> points){
        this.mapboxMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#3bb2d0"))
                .width(2));
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
}
