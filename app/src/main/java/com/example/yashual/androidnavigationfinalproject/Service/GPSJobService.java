package com.example.yashual.androidnavigationfinalproject.Service;


import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class GPSJobService extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent service = new Intent(getApplicationContext(), GPSService.class);
        getApplicationContext().startService(service);
        Log.d(TAG, "onStartJob: start GPS Job Service");
        Util.scheduleJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStartJob: stop GPS Job Service");
        return true;
    }

}
