package com.streetshout.android.Utils;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String USER_NAME = "saved_user_name";

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
}
