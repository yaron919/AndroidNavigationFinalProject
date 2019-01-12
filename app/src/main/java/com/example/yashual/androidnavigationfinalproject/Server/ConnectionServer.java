package com.example.yashual.androidnavigationfinalproject.Server;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionServer extends AppCompatActivity {

    private int port = 3000;
    private String url = "www.google.com";
    private static final String TAG = "Connection Server";
    private RequestQueue mQueue;

    public ConnectionServer (){
        mQueue = Volley.newRequestQueue(this);

    }

    public void jsonParse() {

        String url = "http://myjson.com/k81dk";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonObject = response.getJSONObject("location");
                            double lat = jsonObject.getDouble("lat");
                            double lan = jsonObject.getDouble("lan");
                            Log.d(TAG,"lat is "+lat+" lan is "+lan);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        mQueue.add(request);
    }





}
