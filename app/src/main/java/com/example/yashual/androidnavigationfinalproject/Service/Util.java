package com.example.yashual.androidnavigationfinalproject.Service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class Util {

    // schedule the start of the service every 10 - 30 seconds
    public static boolean scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, GPSService.class);
        JobInfo.Builder builder = new JobInfo.Builder(999, serviceComponent);
        builder.setPersisted(true);
        builder.setPeriodic(15 * 60 * 1000);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
        Log.d(TAG, "scheduleJob: Start");
        return true;
    }
    public static boolean scheduleJobCancel(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancel(999);
        Log.d(TAG, "scheduleJob: canceled");
        return false;
    }

}
