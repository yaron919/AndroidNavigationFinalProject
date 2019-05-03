package com.example.yashual.androidnavigationfinalproject.Server;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.yashual.androidnavigationfinalproject.AreasActivity;
import com.example.yashual.androidnavigationfinalproject.Service.DatabaseHelper;
import com.example.yashual.androidnavigationfinalproject.MainActivity;
import com.example.yashual.androidnavigationfinalproject.SafePoint;
import com.example.yashual.androidnavigationfinalproject.Service.GPSService;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.iid.FirebaseInstanceId;

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
    private AreasActivity areasActivity;
    private DatabaseHelper mDatabaseHelper;
    private GPSService gpsService;
    private SafePoint originPosition;
    private static String unique_id;
    private static Geocoder geocoder;
    private static final String URL_BASE = "http://109.226.11.202:3000";


    public ConnectionServer (Context context){
        mQueue = Volley.newRequestQueue(context);
        this.mainActivity = (MainActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
        unique_id  = FirebaseInstanceId.getInstance().getToken();
        geocoder = new Geocoder(context, Locale.ENGLISH);
    }
    public ConnectionServer (Context context, String def ){
        mQueue = Volley.newRequestQueue(context);
        this.areasActivity = (AreasActivity) context;
        mDatabaseHelper = new DatabaseHelper(context);
        unique_id  = FirebaseInstanceId.getInstance().getToken();
        geocoder = new Geocoder(context, Locale.ENGLISH);

    }

    public void getSafeLocation(double lat, double lan){
        this.originPosition = new SafePoint(lat,lan);
        String url = URL_BASE + "/operative/closestShelters";
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
                ArrayList <LatLng> rv = mDatabaseHelper.getPointsNear(originPosition); // get points from db
                mainActivity.addSafeMarkerOnMap(rv);
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }



    // Fetch method
    public void getAllSubAreas() {
        String url = URL_BASE+"/management/areas/preferred?unique_id="+unique_id;
        JsonArrayRequest request = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        ArrayList<Integer> subbedAreas = new ArrayList<>();
                        for(int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                Log.d(TAG, "areaCode is"+jsonObject.getInt("area_code"));
                                subbedAreas.add(jsonObject.getInt("area_code"));
                            }
                            catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        areasActivity.updateListFromServer(subbedAreas);

                    }
                },
                new Response.ErrorListener() {
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
        cityName = cityName.replaceAll("\\s+","");
//        cityName = "hadera";
        String language = Locale.getDefault().getDisplayLanguage().toLowerCase();
        switch(language) {
            case("עברית"):
                language = "hebrew";
                break;
        }
        String url = URL_BASE+"/idle/update";
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
        String url = URL_BASE+"/idle/register";
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

    public static void addNotifyCity(int areaCode) throws JSONException{
        String url = URL_BASE+"/management/areas/preferred";
        Log.d(TAG, "onResponse: area_code: "+areaCode);
        Log.d(TAG, "notify areas by id: start function");
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("unique_id",unique_id);
        jsonObj.put("area_code",areaCode+"");
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "onResponse: addNotifyCity"+response.toString());
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static void deleteNotifyCity(int areaCode) throws JSONException{
        String url = URL_BASE+"/management/areas/OnePreferred?area_code="+areaCode+"&unique_id="+unique_id;
        JSONObject jsonObj = new JSONObject();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url,jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e(TAG, "onResponse: from delete notify"+response);
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
        String url = String.format(URL_BASE+"/idle/preferred_language");
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

    public static void Arrive(int redAlertID){
        String url = URL_BASE+"/operative/arrive";
        Log.d(TAG, "Arrive: ");
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("unique_id",unique_id);
            jsonObj.put("red_alert_id",redAlertID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Arrive: body:" + jsonObj.toString() );
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObj, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                    Log.d(TAG, "onResponse: getSafeLocation"+response.toString());
                    Log.d(TAG, "onResponse: before show safe location");
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    public static void updateFirebaseInstance(String prev_id)  {
        Log.d(TAG, "updateFirebaseInstance: start function");
        String url = URL_BASE+"/idle/register";
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("unique_id",unique_id);
            jsonObj.put("prev_id",prev_id);
            jsonObj.put("is_android",1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "updateFirebaseInstance: body json:"+jsonObj.toString());
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
}
