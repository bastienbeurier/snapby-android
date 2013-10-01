package com.streetshout.android.utils;


import android.app.Application;
import com.androidquery.util.AQUtility;

public class StreetShoutApplication extends Application {

    private AppPreferences appPrefs = null;

    @Override
    public void onCreate() {
        appPrefs = new AppPreferences(getApplicationContext());

        PushNotifications.initialize(this);

        if (Constants.PRODUCTION) {
            AQUtility.setDebug(false);
        } else {
            AQUtility.setDebug(true);
        }
    }

    public AppPreferences getAppPrefs() {
        return appPrefs;
    }
}
