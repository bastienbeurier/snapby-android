package com.streetshout.android.Utils;


import android.app.Application;
import com.androidquery.util.AQUtility;

public class StreetShoutApplication extends Application {
    @Override
    public void onCreate() {
        PushNotifications.initialize(this);
        if (!Constants.PRODUCTION) {
            AQUtility.setDebug(true);
        }
    }
}
