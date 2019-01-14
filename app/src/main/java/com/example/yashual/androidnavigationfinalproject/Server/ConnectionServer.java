package com.example.yashual.androidnavigationfinalproject.Server;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.yashual.androidnavigationfinalproject.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.MainActivity;
import com.example.yashual.androidnavigationfinalproject.Point;
import com.google.gson.JsonObject;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ConnectionServer  {
    private static final String TAG = "Connection Server";
    private RequestQueue mQueue;
    private MainActivity mainActivity;
    private DatabaseHelper mDatabaseHelper;

    public ConnectionServer (Context context){
        mQueue = Volley.newRequestQueue(context);
        this.mainActivity = (MainActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
    }

    public void getSafeLocation(){
        String url = "https://api.myjson.com/bins/ng7h4";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("locations");
                    ArrayList <LatLng> rv = new ArrayList<>();
                    for (int i=0; i< jsonArray.length(); i++) {
                        JSONObject latlan = jsonArray.getJSONObject(i);
                        mDatabaseHelper.addData(latlan.getDouble("lat"),latlan.getDouble("lan")); // adding points to local db
                        rv.add(new LatLng(latlan.getDouble("lat"), latlan.getDouble("lan"))); // this list will be changed
                    }
                   // Point p = new Point(rv.get(0).getLatitude(),rv.get(0).getLongitude());
                  //  mDatabaseHelper.getPointsNear(p); // we are going to get the points to show to the user here
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





}
