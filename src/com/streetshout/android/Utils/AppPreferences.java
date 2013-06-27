package com.streetshout.android.utils;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String USER_NAME = "ss_saved_user_name";

    private static final String DISTANCE_UNIT = "ss_saved_distance_unit";

    private static final String NOTIFICATION_PREF = "ss_saved_notification_pref";

    private static final String APP_SHARED_PREFS = AppPreferences.class.getSimpleName();

    private SharedPreferences sharedPrefs = null;

    private SharedPreferences.Editor prefsEditor = null;

    public AppPreferences(Context context) {
        this.sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this.prefsEditor = sharedPrefs.edit();
    }

    public String getUserNamePref() {
        return sharedPrefs.getString(USER_NAME, "");
    }

    public void setUserNamePref(String value) {
        prefsEditor.putString(USER_NAME, value);
        prefsEditor.commit();
    }

    public int getDistanceUnitPref() {
        return sharedPrefs.getInt(DISTANCE_UNIT, -1);
    }

    public void setDistanceUnitPref(int value) {
        prefsEditor.putInt(DISTANCE_UNIT, value);
        prefsEditor.commit();
    }

    public int getNotificationPref() {
        return sharedPrefs.getInt(NOTIFICATION_PREF, -1);
    }

    public void setNotificationPref(int value) {
        prefsEditor.putInt(NOTIFICATION_PREF, value);
        prefsEditor.commit();
    }
}
