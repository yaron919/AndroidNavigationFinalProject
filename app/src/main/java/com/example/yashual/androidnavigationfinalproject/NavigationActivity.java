package com.example.yashual.androidnavigationfinalproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.widget.TextView;

import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Response;

public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,RouteListener, NavigationListener,ProgressChangeListener {

    private static final String TAG = NavigationActivity.class.getSimpleName();
    private static final double INITIAL_ZOOM = 19;
    private NavigationView navigationView;
    private DirectionsRoute directionsRoute;
    private Point originPosition;
    private Point destinationPosition;
    private int redAlertID;
    private boolean fromNotification = false;
    private TextView timerText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigation_view);
        timerText = findViewById(R.id.timerText);
        navigationView.onCreate(savedInstanceState);
        updateNightMode();
        Intent intent = getIntent();
        originPosition = Point.fromLngLat(intent.getDoubleExtra("positionLon",0.0),
                intent.getDoubleExtra("positionLat",0.0));
        destinationPosition = Point.fromLngLat(intent.getDoubleExtra("destinationLon",0.0),
                intent.getDoubleExtra("destinationLat",0.0));
        redAlertID = intent.getIntExtra("AlertID",-1);
        if (redAlertID != -1)
            fromNotification = true;
        Log.d(TAG, "onCreate: position: "+ originPosition.toString());
        Log.d(TAG, "onCreate: dest: "+ destinationPosition.toString());
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(originPosition.latitude(), originPosition.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.initialize(this, initialPosition);
        fetchRoute(originPosition,destinationPosition);
        updateView((String)Paper.book().read("language"));
        startTimer(45);
    }

    private void updateView(String language) {
        LocaleHelper.setLocale(this,language);
    }

    private void startTimer(int time){
        new CountDownTimer(time*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                timerText.setText(getString(R.string.seconds_remain)+millisUntilFinished / 1000);
            }
            public void onFinish() {
                timerText.setText("done!");
            }
        }.start();

    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
// If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
//        NavigationViewOptions options = NavigationViewOptions.builder()
//                .directionsRoute(directionsRoute)
//                .shouldSimulateRoute(true)
//                .build();
//
//        navigationView.startNavigation(options);
    }

    @Override
    public void onCancelNavigation() {
        navigationView.stopNavigation();
        stopNavigation();
    }

    @Override
    public void onNavigationFinished() {
    }

    @Override
    public void onNavigationRunning() {

    }
    private void fetchRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .destination(destination)
                .build()
                .getRoute(new SimplifiedCallback() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        directionsRoute = response.body().routes().get(0);
                        startNavigation(directionsRoute);
                    }
                });
    }
    public void startNavigation(DirectionsRoute directions) {
        if (directions == null) {
            return;
        }
        NavigationViewOptions options = NavigationViewOptions.builder()
                .directionsRoute(directions)
                .shouldSimulateRoute(true)
                .navigationListener(this)
                .routeListener(this)
                .directionsProfile(DirectionsCriteria.PROFILE_WALKING)
                .progressChangeListener(this)
                .build();
        navigationView.startNavigation(options);
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        boolean isInTunnel = routeProgress.inTunnel();
        boolean wasInTunnel = wasInTunnel();
        if (isInTunnel) {
            if (!wasInTunnel) {
                updateWasInTunnel(true);
                updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } else {
            if (wasInTunnel) {
                updateWasInTunnel(false);
                updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            }
        }
    }
    private void updateNightMode() {
        if (wasNavigationStopped()) {
            updateWasNavigationStopped(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
//            getActivity().recreate();
            finish();
        }
    }
    public void updateWasNavigationStopped(boolean wasNavigationStopped) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(getString(R.string.was_navigation_stopped), wasNavigationStopped);
        editor.apply();
    }
    private boolean wasNavigationStopped() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(getString(R.string.was_navigation_stopped), false);
    }
    private boolean wasInTunnel() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(this.getString(R.string.was_in_tunnel), false);
    }

    private void updateWasInTunnel(boolean wasInTunnel) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(this.getString(R.string.was_in_tunnel), wasInTunnel);
        editor.apply();
    }
    private void stopNavigation() {
        updateWasNavigationStopped(true);
        updateWasInTunnel(false);
    }
    private void updateCurrentNightMode(int nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode);
        finish();
    }

    @Override
    public boolean allowRerouteFrom(Point offRoutePoint) {
        return false;
    }

    @Override
    public void onOffRoute(Point offRoutePoint) {

    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {

    }

    @Override
    public void onFailedReroute(String errorMessage) {

    }

    @Override
    public void onArrival() {
        if (fromNotification)
            ConnectionServer.Arrive(redAlertID);
    }
}
