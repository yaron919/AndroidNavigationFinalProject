package com.example.yashual.androidnavigationfinalproject.Server;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.MainActivity;
import com.example.yashual.androidnavigationfinalproject.SafePoint;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;

public class ConnectionServer  {
    private static final String TAG = "Connection Server";
    private static RequestQueue mQueue;
    private MainActivity mainActivity;
    private DatabaseHelper mDatabaseHelper;
    private GPSService gpsService;
    private SafePoint originPosition;
    private static String unique_id;
    private static Geocoder geocoder;


    public ConnectionServer (Context context){
        mQueue = Volley.newRequestQueue(context);
        this.mainActivity = (MainActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
        unique_id  = FirebaseInstanceId.getInstance().getToken();
        geocoder = new Geocoder(context, Locale.ENGLISH);

    }

    public void getSafeLocation(double lat, double lan){
        this.originPosition = new SafePoint(lat,lan);
        String url = "http://3.121.116.91:3000/operative/closestShelters";
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("unique_id",unique_id);
            jsonObj.put("latitude",lat);
            jsonObj.put("longitude",lan);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "getSafeLocation: body:" + jsonObj.toString() );
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "onResponse: getSafeLocation"+response.toString());
                    JSONArray jsonArray = response.getJSONArray("result");
                    ArrayList <LatLng> rv;
                    for (int i=0; i< jsonArray.length(); i++) {
                        JSONObject latlan = jsonArray.getJSONObject(i);
                        mDatabaseHelper.addData(latlan.getDouble("latitude"),latlan.getDouble("longitude")); // adding points to local db
                    }
                    Log.d(TAG, "onResponse: before show safe location");
                    rv = mDatabaseHelper.getPointsNear(originPosition); // get points from db
                    mainActivity.addSafeMarkerOnMap(rv);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static void sendMyLocationToServer(Double lat, Double lan) throws JSONException, IOException {
        Log.d(TAG, "sendMyLocationToServer: start function");
        List<Address> addresses = geocoder.getFromLocation(lat, lan, 1);
        String cityName = addresses.get(0).getLocality().toLowerCase();
        String language = Locale.getDefault().getDisplayLanguage().toLowerCase();
        switch(language) {
            case("עברית"):
                language = "hebrew";
                break;
        }
        String url = "http://3.121.116.91:3000/idle/update";
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("lat",lat);
        jsonObj.put("lang",lan);
        jsonObj.put("city",cityName);
        jsonObj.put("unique_id",unique_id);
        jsonObj.put("language",language);
        jsonObj.put("is_android",1);
        Log.d(TAG, "sendMyLocationToServer: json body:"+jsonObj.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "onResponse: respone"+ response );
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static void registerOnServerMyPhoneId() throws JSONException {
        Log.d(TAG, "registerOnServerMyPhoneId: start function");
        String url = "http://3.121.116.91:3000/idle/register";
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("unique_id",unique_id);
        jsonObj.put("is_android",1);
        Log.d(TAG, "registerOnServerMyPhoneId: body json:"+jsonObj.toString());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e(TAG, "onResponse: from register on server");
                Paper.book().write("unique_id",unique_id);
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static void UpdateLanguageInServer (){
        String url = String.format("http://3.121.116.91:3000/idle/preferred_language");
        Log.e(TAG, "UpdateLanguageInServer: url: "+url);
        String language = Locale.getDefault().getDisplayLanguage().toLowerCase();
        switch(language) {
            case("עברית"):
                language = "hebrew";
                break;
        }
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("unique_id",unique_id);
            jsonObj.put("language",language);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "UpdateLanguageInServer: body:" + jsonObj.toString() );
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                    Log.e(TAG, "onResponse: UpdateLanguageInServer"+response.toString());
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }
}
