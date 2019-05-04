package com.example.yashual.androidnavigationfinalproject;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
// classes needed to initialize map
import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.example.yashual.androidnavigationfinalproject.Service.Util;
import com.example.yashual.androidnavigationfinalproject.Service.WarService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import android.location.Location;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.Toast;


import android.support.annotation.NonNull;

import io.paperdb.Paper;

import android.util.Log;
// classes needed to launch navigation UI
import org.json.JSONException;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback , NavigationView.OnNavigationItemSelectedListener ,NumberPicker.OnValueChangeListener{
    private GoogleMap mMap;
    // variables for adding location layer
    // variables for calculating and drawing a route
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private LatLng mDefaultLocation = new LatLng(32.113819, 34.817794);
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int DEFAULT_ZOOM = 17;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
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
    private boolean jobSchedulerOn = false;

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
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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
        if(Paper.book().read("war") == null)
            Paper.book().write("war",false);
        else
            warSwitch.setChecked(Paper.book().read("war"));
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
            SafePoint destSafePoint = databaseHelper.getNearestSafeLocation(safeList,new SafePoint(mLastKnownLocation));
            LatLng destLatLng = new LatLng(destSafePoint.getLat(), destSafePoint.getLan());
//            List<LatLng> points = new ArrayList<>();
            Log.d(TAG, "onCreate: lat"+destLatLng.latitude+" lan:"+destLatLng.longitude);
//            points.add(destLatLng);
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra("destLng", destLatLng.longitude);
            intent.putExtra("destLat", destLatLng.latitude);
            startActivity(intent);
        });
        Util.scheduleJob(this);
        warSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Paper.book().write("war", isChecked);
            if (isChecked){
                Intent i = new Intent(getApplicationContext(), WarService.class);
                startService(i);
                if (jobSchedulerOn){
                    jobSchedulerOn = Util.scheduleJobCancel(this);
                }
            }else{
                if (isMyServiceRunning(WarService.class)){
                    Intent i = new Intent(getApplicationContext(), WarService.class);
                    stopService(i);
                }
                if (!jobSchedulerOn){
                    jobSchedulerOn = Util.scheduleJob(this);
                }
            }
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
        }else if (!unique_id.equals(FirebaseInstanceId.getInstance().getToken()))
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
                Location location = getLastBestLocation();
                originPosition = new LatLng(location.getLatitude(), location.getLongitude());
                if(validateDistanceToClosestPoint(originPosition, destinationPosition, time)){
                    startNavigation(destinationPosition, alertId,time); // example routing NEED TO ADD DB SEARCH FOR DEST
                }else
                    showNoSafePointMessage();
            }catch(Exception e){
                Log.e(TAG, "checkIntent: error in function");
                e.printStackTrace();
            }
        }else if (getIntent().hasExtra("redAlertId")){
            try {
                Log.d(TAG, "checkIntent: only redAlertId");
                Log.d(TAG, "checkIntent: extra :"+getIntent().getExtras());
                Log.d(TAG, "checkIntent: data :"+getIntent().getDataString());
                connectionServer.closestSheltersAfterNotification(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(),
                        Integer.parseInt(getIntent().getStringExtra("redAlertId")));
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
    }

    public void startNavigation(LatLng destLatLng, int alertId, int timeToDistance) {
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra("destLng", destLatLng.longitude);
        intent.putExtra("destLat", destLatLng.latitude);
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
            show();
//            changeLocale();
        } else if (id == R.id.nav_areas) {
            Intent intent = new Intent(this, AreasActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_sound) {
            if (Paper.book().read("sound").equals("True")) {
                Paper.book().write("sound", "False");
                Toast.makeText(this, R.string.sound_off, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onNavigationItemSelected: sounds False" );
            }
            else{
                Log.d(TAG, "onNavigationItemSelected: sounds True" );
                Toast.makeText(this,R.string.sound_on, Toast.LENGTH_SHORT).show();
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

        // prompt the user for permission
        getLocationPermission();
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
//        Location myLocation = getLastBestLocation();
        checkIntent();

    }
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            try {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                connectionServer.getSafeLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                            } catch (NullPointerException e) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            }
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    public void show()
    {
        final Dialog d = new Dialog(MainActivity.this);
        d.setTitle("NumberPicker");
        d.setContentView(R.layout.dialog);
        Button b1 = (Button) d.findViewById(R.id.cancel_dialog_btn);
        Button b2 = (Button) d.findViewById(R.id.ok_dialog_btn);
        final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker1);
        String[] arrayString= new String[]{"Eng","Heb","Rus"};
        np.setMinValue(0);
        np.setMaxValue(arrayString.length-1);
        np.setDisplayedValues(arrayString);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(this);
//        b1.setOnClickListener(new OnClickListener()
//        {
//            @Override
//            public void onClick(View v) {
//                tv.setText(String.valueOf(np.getValue()));
//                d.dismiss();
//            }
//        });
//        b2.setOnClickListener(new OnClickListener()
//        {
//            @Override
//            public void onClick(View v) {
//                d.dismiss();
//            }
//        });
        d.show();


    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        Log.i("value is",""+newVal);
    }
}
