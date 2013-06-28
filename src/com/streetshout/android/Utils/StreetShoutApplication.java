package com.streetshout.android.utils;


import android.app.Application;
import com.amazonaws.services.s3.AmazonS3Client;
import com.androidquery.util.AQUtility;
import com.streetshout.android.s3.AmazonClientManager;

public class StreetShoutApplication extends Application {
    private AmazonClientManager amazonClientManager = null;

    private AppPreferences appPrefs = null;

    @Override
    public void onCreate() {
        appPrefs = new AppPreferences(getApplicationContext());

        PushNotifications.initialize(this);
        if (!Constants.PRODUCTION) {
            AQUtility.setDebug(true);
        }

        amazonClientManager = new AmazonClientManager(appPrefs.getSharedPrefs());
    }

    public AppPreferences getAppPrefs() {
        return appPrefs;
    }

    public AmazonS3Client getS3Client() {
        return amazonClientManager.s3(getApplicationContext());
    }
}
