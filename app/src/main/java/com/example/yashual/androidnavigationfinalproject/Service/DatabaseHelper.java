package com.example.yashual.androidnavigationfinalproject.Service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.yashual.androidnavigationfinalproject.SafePoint;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "safe_locations";
    private static final String COL0 = "ID";
    private static final String COL1= "lat";
    private static final String COL2 = "lan";

    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //String dropTable = "DROP IF TABLE EXISTS "+TABLE_NAME;
        //db.execSQL(dropTable);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating DB "+TABLE_NAME);
        String createTable = "CREATE TABLE "+TABLE_NAME+" (ID INTEGER PRIMARY KEY AUTOINCREMENT, "+COL1+" REAL, "+COL2+" REAL)";
        db.execSQL(createTable);
    }


    public boolean addData(double lat,double lan){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT id FROM "+ TABLE_NAME+ " WHERE lat="+lat+" AND lan="+lan;
        Cursor does_exist = db.rawQuery(query,null);
        if(!does_exist.moveToNext()){ // add data only if the location is not in the db
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL1,lat);
            contentValues.put(COL2,lan);
            Log.e(TAG, "add lat and lan: Adding: "+lat+", "+lan+"+ to "+TABLE_NAME);
            long result = db.insert(TABLE_NAME,null,contentValues);
            if(result == -1)
                return false;
            else
                return true;
        }else{
            Log.e(TAG, "data exist in table id is: "+does_exist.getInt(0));
            return false;
        }
    }

    /**
     * Calculates the end-safePoint from a given source at a given range (meters)
     * and bearing (degrees). This methods uses simple geometry equations to
     * calculate the end-safePoint.
     *
     * @param safePoint
     *           SafePoint of origin
     * @param range
     *           Range in meters
     * @param bearing
     *           Bearing in degrees
     * @return End-safePoint from the source given the desired range and bearing.
     */
    public static SafePoint calculateDerivedPosition(SafePoint safePoint,
                                                     double range, double bearing)
    {
        double EarthRadius = 6371000; // m
        double latA;
        latA = Math.toRadians(safePoint.getLat());
        double lonA = Math.toRadians(safePoint.getLan());
        double angularDistance = range / EarthRadius;
        double trueCourse = Math.toRadians(bearing);

        double lat = Math.asin(
                Math.sin(latA) * Math.cos(angularDistance) +
                        Math.cos(latA) * Math.sin(angularDistance)
                                * Math.cos(trueCourse));

        double dlon = Math.atan2(
                Math.sin(trueCourse) * Math.sin(angularDistance)
                        * Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

        double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        SafePoint newSafePoint = new SafePoint((double) lat, (double) lon);
        return newSafePoint;
    }

    public ArrayList<LatLng>  getPointsNear(SafePoint center){
        // get all points with a 300 m radius from the user
        ArrayList <LatLng> rv = new ArrayList<>();
        final double mult = 1; // mult = 1.1; is more reliable
        final double radius =400; // distance from point
        SafePoint p1 = calculateDerivedPosition(center, mult * radius, 0);
        SafePoint p2 = calculateDerivedPosition(center, mult * radius, 90);
        SafePoint p3 = calculateDerivedPosition(center, mult * radius, 180);
        SafePoint p4 = calculateDerivedPosition(center, mult * radius, 270);
        SQLiteDatabase db = this.getWritableDatabase();
        String strWhere =  " WHERE "
                + "lat > " + String.valueOf(p3.getLat()) + " AND "
                + "lat < " + String.valueOf(p1.getLat()) + " AND "
                + "lan < " + String.valueOf(p2.getLan()) + " AND "
                + "lan > " + String.valueOf(p4.getLan());

        Log.e(TAG, "where : "+strWhere);
        String query = "SELECT * FROM "+TABLE_NAME+strWhere;
        Cursor safePlacesNear = db.rawQuery(query,null);
        Log.e(TAG, "getPointsNear: counts "+safePlacesNear.getCount() );
        while(safePlacesNear.moveToNext()){
            rv.add(new LatLng(safePlacesNear.getDouble(1), safePlacesNear.getDouble(2)));
            Log.e(TAG, "points near are : "+safePlacesNear.getInt(0));
        }
        return rv;
    }
    public static double getDistanceBetweenTwoPoints(SafePoint p1, SafePoint p2) {
        double R = 6371000; // m
        double dLat = Math.toRadians(p2.getLat() - p1.getLat());
        double dLon = Math.toRadians(p2.getLan() - p1.getLan());
        double lat1 = Math.toRadians(p1.getLat());
        double lat2 = Math.toRadians(p2.getLat());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
                * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;

        return d;
    }
    public SafePoint getNearestSafeLocation(List<LatLng> list,SafePoint currentLocation){
        double shortestDistance;
        double distance;
        SafePoint nearest_location = new SafePoint(list.get(0).getLatitude(),list.get(0).getLongitude());
        shortestDistance = getDistanceBetweenTwoPoints(currentLocation,nearest_location);
        for(int i = 0 ; i < list.size() ; i++)
        {
            SafePoint safePointInList = new SafePoint(list.get(i).getLatitude(),list.get(i).getLongitude());
            distance = getDistanceBetweenTwoPoints(currentLocation,safePointInList);
            if(distance < shortestDistance){
                shortestDistance = distance;
                nearest_location =safePointInList;
            }
        }
        return nearest_location;
    }
/*
    public Cursor getTop10(){
     //   SQLiteDatabase db = this.getWritableDatabase();
      //  String query = "SELECT * FROM "+TABLE_NAME+" ORDER BY "+COL3 +" DESC LIMIT 10";
     //   Cursor data = db.rawQuery(query,null);
    //    return data;
    }*/
}
