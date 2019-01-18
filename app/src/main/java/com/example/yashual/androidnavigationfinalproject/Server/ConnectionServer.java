package com.example.yashual.androidnavigationfinalproject.Server;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.MainActivity;
import com.example.yashual.androidnavigationfinalproject.SafePoint;
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
    private SafePoint originPosition;


    public ConnectionServer (Context context){
        mQueue = Volley.newRequestQueue(context);
        this.mainActivity = (MainActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);

    }

    public void getSafeLocation(double lat, double lan){
        this.originPosition = new SafePoint(lat,lan);
        String url = "https://api.myjson.com/bins/dqf1c";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("locations");
                    ArrayList <LatLng> rv;
                    for (int i=0; i< jsonArray.length(); i++) {
                        JSONObject latlan = jsonArray.getJSONObject(i);
                        mDatabaseHelper.addData(latlan.getDouble("lat"),latlan.getDouble("lan")); // adding points to local db
                    }
                    Log.e(TAG, "onResponse: before show safe location");
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





}
