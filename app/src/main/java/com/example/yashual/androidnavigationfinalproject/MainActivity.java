package com.example.yashual.androidnavigationfinalproject;


import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
// classes needed to initialize map
import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import android.location.Location;
import android.util.DisplayMetrics;
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
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
// classes needed to add a marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.util.Log;
// classes needed to launch navigation UI

import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {
    private MapView mapView;
    // variables for adding location layer
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private Location originLocation;
    // variables for calculating and drawing a route
    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private ConnectionServer connectionServer;
    private Button navigateButton;
    private DatabaseHelper databaseHelper;
    private List<LatLng> safeList;
    private ImageButton languageButton;
    private Switch warSwitch;

    //offline maps
    private boolean isEndNotified;
    private ProgressBar progressBar;
    private OfflineManager offlineManager;

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase,"en"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        languageButton = findViewById(R.id.languageButton);
        mapView = findViewById(R.id.mapView);
        warSwitch = (Switch) findViewById(R.id.warSwitch);
        navigateButton = findViewById(R.id.navigateButton);
        this.databaseHelper = new DatabaseHelper(this);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        warSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GPSService.isWar = isChecked;
            Intent i = new Intent(getApplicationContext(), GPSService.class);
            stopService(i);
            startService(i);
        });
        languageButton.setOnClickListener(v -> changeLocale());
        navigateButton.setOnClickListener(v -> {
            SafePoint destSafePoint = databaseHelper.getNearestSafeLocation(safeList,new SafePoint(originLocation));
            originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());
            destinationPosition = Point.fromLngLat(destSafePoint.getLan(), destSafePoint.getLat());
            getRoute(originPosition, destinationPosition); // example routing NEED TO ADD DB SEARCH FOR DEST
        });

        //Set default lang
        Paper.init(this);

        //English is default
        String language = Paper.book().read("language");
        if(language == null)
            Paper.book().write("language","en");

        updateView((String)Paper.book().read("language"));

        if (!GPSService.state) {
            Intent i = new Intent(getApplicationContext(), GPSService.class);
            startService(i);
        }

        this.connectionServer = new ConnectionServer(this);
        downloadOfflineMap();
    }

    private void downloadOfflineMap(){

        //Set up offline manager
        offlineManager = OfflineManager.getInstance(MainActivity.this);
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(new LatLng(35.225, 33.248)) // Northeast
                .include(new LatLng(34.445, 30.942)) // Southwest
                .build();
        // Define the offline region
        Log.e(TAG, "Inside offline");
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                "mapbox://styles/yaron919/cjr64dzlf36g02slfo0s3zzkm",
                latLngBounds,
                6,
                13,
                MainActivity.this.getResources().getDisplayMetrics().density);
        // Set the metadata
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, "Metola to bear sheva");
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to encode metadata: " + exception.getMessage());
            metadata = null;
        }
        // Create the region asynchronously
        offlineManager.createOfflineRegion(
                definition,
                metadata,
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
                        // Display the download progress bar
                        //       progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                        //       startProgress();

                        // Monitor the download progress using setObserver
                        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                            @Override
                            public void onStatusChanged(OfflineRegionStatus status) {
                                Log.e(TAG, "Download started");
                                // Calculate the download percentage and update the progress bar
                                double percentage = status.getRequiredResourceCount() >= 0
                                        ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                                        0.0;

                                if (status.isComplete()) {
                                    Log.e(TAG, "Download complete");
                                    // Download complete
                                   //      endProgress("done");
                                } else if (status.isRequiredResourceCountPrecise()) {
                                    // Switch to determinate state
                                    //      setPercentage((int) Math.round(percentage));
                                }
                            }

                            @Override
                            public void onError(OfflineRegionError error) {
                                // If an error occurs, print to logcat
                                Log.e(TAG, "onError reason: " + error.getReason());
                                Log.e(TAG, "onError message: " + error.getMessage());
                            }

                            @Override
                            public void mapboxTileCountLimitExceeded(long limit) {
                                // Notify if offline region exceeds maximum tile count
                                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error: help " + error);
                    }
                });

    }

    private boolean validateDistanceToClosestPoint(Point currentLocation, Point destination){
        double distance = databaseHelper.getDistanceBetweenTwoPoints(new SafePoint(currentLocation.latitude()
                ,currentLocation.altitude()
        ),new SafePoint(destination.latitude(),destination.altitude()));
        return (distance < 300);
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
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    private void navigationLauncherStart(){
        Log.e(TAG, "navigationLauncherStart: start" );
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .directionsProfile(DirectionsCriteria.PROFILE_WALKING)
                .shouldSimulateRoute(false)
                .waynameChipEnabled(true)
                .build();
        Log.e(TAG, "navigationLauncherStart: before luncher" );
        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(this, options);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        enableLocationComponent();
        checkIntent();
        connectionServer.getSafeLocation(this.originLocation.getLatitude(), this.originLocation.getLongitude());
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
        if (getIntent().hasExtra("lat") && getIntent().hasExtra("lan")) {
            try{
                Log.d(TAG, "i got an intent");
                double lat = Double.parseDouble(getIntent().getStringExtra("lat"));
                double lan = Double.parseDouble(getIntent().getStringExtra("lan"));
                Log.d(TAG, " "+lat+"  "+lan);
                destinationPosition = Point.fromLngLat(lat,lan);
                originPosition = Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());
                if(validateDistanceToClosestPoint(originPosition,destinationPosition)){
                    getRoute(originPosition, destinationPosition); // example routing NEED TO ADD DB SEARCH FOR DEST
                }else
                    showNoSafePointMessage();
            }catch(Exception e){
                Log.e(TAG, "checkIntent: error in function");
            }
        }
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                        navigationLauncherStart();
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
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
            locationComponent.setRenderMode(RenderMode.COMPASS);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

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
        if (offlineManager != null) {
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                @Override
                public void onList(OfflineRegion[] offlineRegions) {
                    if (offlineRegions.length > 0) {
                        // delete the last item in the offlineRegions list which will be yosemite offline map
                        offlineRegions[(offlineRegions.length - 1)].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                            @Override
                            public void onDelete() {
                                Toast.makeText(
                                        MainActivity.this,
                                        getString(R.string.project_id),
                                        Toast.LENGTH_LONG
                                ).show();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "On Delete error: " + error);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "onListError: " + error);
                }
            });
        }
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
                .zoom(19) // Sets the zoom
                .bearing(180) // Rotate the camera
                .tilt(30) // Set the camera tilt
                .build(); // Creates a CameraPosition from the builder

        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 7000);
    }


    // Progress bar methods
    private void startProgress() {

        // Start and show the progress bar
        isEndNotified = false;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setPercentage(final int percentage) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);
    }

    private void endProgress(final String message) {
        // Don't notify more than once
        if (isEndNotified) {
            return;
       }

        // Stop and hide the progress bar
        isEndNotified = true;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

        // Show a toast
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }
}

