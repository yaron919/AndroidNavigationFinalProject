package com.example.yashual.androidnavigationfinalproject;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mapbox.geojson.Point;


public class FragmentNavigationActivity extends AppCompatActivity {

  private static final String FAB_VISIBLE_KEY = "restart_fab_visible";
  private static final String TAG = FragmentNavigationActivity.class.getSimpleName();
  private NavigationFragment navigationFragment;
  private Point originPosition;
  private Point destinationPosition;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_fragment);
//    initializeNavigationViewFragment(savedInstanceState);
//    restartNavigationFab = findViewById(R.id.restart_navigation_fab);
//    restartNavigationFab.setOnClickListener(v -> {
//      replaceFragment(new NavigationFragment());
//      restartNavigationFab.hide();
//    });
    Intent intent = getIntent();
    originPosition = Point.fromLngLat(intent.getDoubleExtra("positionLon",0.0),
            intent.getDoubleExtra("positionLat",0.0));
    destinationPosition = Point.fromLngLat(intent.getDoubleExtra("destinationLon",0.0),
            intent.getDoubleExtra("destinationLat",0.0));
    navigationFragment = new NavigationFragment();
    Log.d(TAG, "onCreate: position: "+ originPosition.toString());
    Log.d(TAG, "onCreate: dest: "+ destinationPosition.toString());
    navigationFragment.setOriginAndDestination(originPosition,destinationPosition,this);
  }

//  @Override
//  protected void onSaveInstanceState(Bundle outState) {
//    super.onSaveInstanceState(outState);
//    outState.putBoolean(FAB_VISIBLE_KEY, restartNavigationFab.getVisibility() == View.VISIBLE);
//  }
//
//  @Override
//  protected void onRestoreInstanceState(Bundle savedInstanceState) {
//    super.onRestoreInstanceState(savedInstanceState);
//    boolean isVisible = savedInstanceState.getBoolean(FAB_VISIBLE_KEY);
//    int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
//    restartNavigationFab.setVisibility(visibility);
//  }
//
//  public void showNavigationFab() {
//    restartNavigationFab.show();
//  }
//
//  public void showPlaceholderFragment() {
//    replaceFragment(new PlaceholderFragment());
//  }
//
//  private void initializeNavigationViewFragment(@Nullable Bundle savedInstanceState) {
//    FragmentManager fragmentManager = getSupportFragmentManager();
//    if (savedInstanceState == null) {
//      FragmentTransaction transaction = fragmentManager.beginTransaction();
//      transaction.disallowAddToBackStack();
//      transaction.add(R.id.navigation_fragment_frame, new NavigationFragment()).commit();
//    }
//  }
//
//  private void replaceFragment(Fragment newFragment) {
//    String tag = String.valueOf(newFragment.getId());
//    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//    transaction.disallowAddToBackStack();
//    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//    int fadeInAnimId = android.support.v7.appcompat.R.anim.abc_fade_in;
//    int fadeOutAnimId = android.support.v7.appcompat.R.anim.abc_fade_out;
//    transaction.setCustomAnimations(fadeInAnimId, fadeOutAnimId, fadeInAnimId, fadeOutAnimId);
//    transaction.replace(R.id.navigation_fragment_frame, newFragment, tag);
//    transaction.commit();
//  }
}
