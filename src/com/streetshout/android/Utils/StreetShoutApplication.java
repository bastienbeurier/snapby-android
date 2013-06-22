package com.streetshout.android.Utils;


import android.app.Application;

public class StreetShoutApplication extends Application {
    @Override
    public void onCreate() {
        PushNotifications.initialize(this);
    }
}
