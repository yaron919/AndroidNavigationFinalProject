package com.example.yashual.androidnavigationfinalproject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.Toast;

import com.example.yashual.androidnavigationfinalproject.Server.ConnectionServer;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.example.yashual.androidnavigationfinalproject.Service.LocaleHelper;
import com.example.yashual.androidnavigationfinalproject.Service.Util;
import com.example.yashual.androidnavigationfinalproject.Service.WarService;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import io.paperdb.Paper;

public class AreasActivity extends AppCompatActivity implements  NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private static final String TAG = "AreasActivity";
    private ArrayList<City> cities;
    private ArrayList<City> tempCities;
    private EditText search;
    private ConnectionServer connectionServer;
    private CitiesCustomAdapter adapter;
    private ListView listView;
    private Switch warSwitch;
    private int currentAmountOfSubs;
    private int MAX_SUBS = 10;
    private boolean jobSchedulerOn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_areas);
        this.connectionServer = new ConnectionServer(this,"");
        Paper.init(this);
        currentAmountOfSubs = 0;
        search = (EditText) findViewById(R.id.searchView);
        listView = (ListView) findViewById(R.id.listView);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View bar = findViewById(R.id.include_bar);
        ImageButton imageButton = bar.findViewById(R.id.nav_view_btn);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        imageButton.setOnClickListener(v -> mDrawerLayout.openDrawer(GravityCompat.START));
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("")){
                    displayListView();
                }else{
                    searchList(s.toString());
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        warSwitch = navigationView.getMenu().findItem(R.id.nav_war_mode).getActionView().findViewById(R.id.warSwitch);
        warSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            GPSService.isWar = isChecked;
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
        String language = Paper.book().read("language");
        if(language == null)
            Paper.book().write("language","en");
        if(Paper.book().read("sound") == null)
            Paper.book().write("sound","True");
        if(Paper.book().read("war") == null)
            Paper.book().write("war",false);
        else
            warSwitch.setChecked(Paper.book().read("war")); // set switch status
        updateView(Paper.book().read("language"));
        displayListView();


    }
    public void updateListFromServer(ArrayList<Integer> areaCodes){
        currentAmountOfSubs = areaCodes.size(); // set number of subs
        for(int i =0 ; i< cities.size(); i++){
            if(areaCodes.contains(cities.get(i).getCode())){
                cities.get(i).setSelected(true);
            }
        }
        Paper.book().write("cities",cities);


    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase,"en"));
    }

    private void updateView(String language) {
        LocaleHelper.setLocale(this,language);
    }
    private void displayListView(){
        cities = Paper.book().read("cities");
        if (cities == null)
            updateInitCitiesList();
        connectionServer.getAllSubAreas();
        updateListLanguage();
        adapter = new CitiesCustomAdapter(this, cities);
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
                        currentAmountOfSubs--;
                        Toast.makeText( getApplicationContext(), R.string.alarm_off, Toast.LENGTH_SHORT).show();

                    }  catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    if(currentAmountOfSubs < MAX_SUBS){
                        try{
                            city.setSelected(true);
                            connectionServer.addNotifyCity(city.getCode());
                            currentAmountOfSubs++;
                             Toast.makeText( getApplicationContext(), R.string.alarm_on, Toast.LENGTH_SHORT).show();
                        }  catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else{
                        Toast.makeText( getApplicationContext(),R.string.maximum_subs, Toast.LENGTH_SHORT).show();
                    }
                }
                Paper.book().write("cities",cities);
                cities.set(position, city);
                //now update adapter
                if(search.getText().length() == 0)
                    adapter.updateRecords(cities);
                else
                    adapter.updateRecords(tempCities);

            }
        });
    }

    private void searchList(String textToSearch){
        tempCities = new ArrayList<>();
        for (City city : new ArrayList<>(cities)) {
            if (city.getName().toLowerCase().startsWith(textToSearch.toLowerCase())) {
                tempCities.add(city);
            }
        }
        adapter.updateRecords(tempCities);
    }

    private void updateInitCitiesList(){
        cities = new ArrayList<City>();
        tempCities = Paper.book().read("cities");
        Resources res = getResources();
        String[] initCities = res.getStringArray(R.array.cities);
        for (String str : initCities) {
            String[] splittedItem = str.split("@");
            if(tempCities == null){
                cities.add(new City(Integer.parseInt(splittedItem[1]),splittedItem[0],false));
            }else{
                cities.add(new City(Integer.parseInt(splittedItem[1]),splittedItem[0],false)); // should be changed
            }
        }

    }

    private void updateListLanguage(){
        Resources res = getResources();
        String[] initCities = res.getStringArray(R.array.cities);
        for(int i = 0 ; i < initCities.length; i++){
            cities.get(i).setName(initCities[i].split("@")[0]);
        }
        Paper.book().write("cities",cities);
    }
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
/*            Intent intent = new Intent(this, AreasActivity.class);
            startActivity(intent);*/
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
    private  void changeLocale(){
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
        ConnectionServer.UpdateLanguageInServer();
        Intent refresh = new Intent(this, AreasActivity.class);
        startActivity(refresh);
        finish();
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

}
