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
import com.example.yashual.androidnavigationfinalproject.MainActivity;
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
    private Context context;
    private MainActivity mainActivity;

    public ConnectionServer (Context context){
        mQueue = Volley.newRequestQueue(context);
        this.mainActivity = (MainActivity) context;
    }

    public void jsonParse() {
        String url = "https://api.myjson.com/bins/14ps0o";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Double lat = response.getDouble("lat");
                    Double lan = response.getDouble("lan");
                    Log.e(TAG, "onResponse: lat ->"+lat +"  lan ->"+lan);
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

    public void getSafeLocation(){
        String url = "https://api.myjson.com/bins/14ps0o";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("locations");
                    ArrayList <LatLng> rv = new ArrayList<>();
                    for (int i=0; i< jsonArray.length(); i++) {
                        JSONObject latlan = jsonArray.getJSONObject(i);
                        Log.d(TAG, "onResponse: latlan" + latlan.toString());
                        rv.add(new LatLng(latlan.getDouble("lat"), latlan.getDouble("lan")));
                    }
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
