package com.example.yashual.androidnavigationfinalproject;

import android.content.Intent;
import android.content.res.Resources;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;

import org.json.JSONException;

import java.util.ArrayList;
import io.paperdb.Paper;

public class AreasActivity extends AppCompatActivity implements  NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private static final String TAG = "AreasActivity";
    private ArrayList<City> cities;
    private ConnectionServer connectionServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas);
        this.connectionServer = new ConnectionServer(this,"");
        Paper.init(this);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View bar = findViewById(R.id.include_bar);
        ImageButton imageButton = bar.findViewById(R.id.nav_view_btn);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        imageButton.setOnClickListener(v -> mDrawerLayout.openDrawer(GravityCompat.START));
        displayListView();

    }
    private void displayListView(){
        ListView listView = (ListView) findViewById(R.id.listView);

        cities = Paper.book().read("cities");
        if (cities == null)
            updateInitCitiesList();

        final CitiesCustomAdapter adapter = new CitiesCustomAdapter(this, cities);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                City city = cities.get(position);

                if(city.isSelected()){
                    //remove from Warning messages API
                    city.setSelected(false);
                    try{
                        connectionServer.deleteNotifyCity(city.getCode());
                    }  catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    // add to warning message API
                    city.setSelected(true);
                    try{
                        connectionServer.addNotifyCity(city.getCode());
                    }  catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Paper.book().write("cities",cities);
                cities.set(position, city);

                //now update adapter
                adapter.updateRecords(cities);

            }
        });
    }

    private void updateInitCitiesList(){
        cities = new ArrayList<City>();
        Resources res = getResources();
        String[] initCities = res.getStringArray(R.array.cities);
        for (String str : initCities) {
            String[] splittedItem = str.split("@");
            cities.add(new City(Integer.parseInt(splittedItem[1]),splittedItem[0],false));
        }

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_locations) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_war_mode) {
            //  warSwitch.setChecked(!warSwitch.isChecked());
        } else if (id == R.id.nav_language) {
            //   changeLocale();
        } else if (id == R.id.nav_areas) {
/*            Intent intent = new Intent(this, AreasActivity.class);
            startActivity(intent);*/
        } else if (id == R.id.nav_sound) {
/*            if (Paper.book().read("sound").equals("True")) {
                Paper.book().write("sound", "False");
                Toast.makeText(this, R.string.sound_off, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onNavigationItemSelected: sounds False" );
            }
            else{
                Log.d(TAG, "onNavigationItemSelected: sounds True" );
                Toast.makeText(this,R.string.sound_on, Toast.LENGTH_SHORT).show();
                Paper.book().write("sound","True");
            }*/
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
