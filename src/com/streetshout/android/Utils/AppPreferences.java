package com.streetshout.android.utils;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String USER_NAME = "ss_saved_user_name";

    private static final String DISTANCE_UNIT = "ss_saved_distance_unit";

    private static final String NOTIFICATION_PREF = "ss_saved_notification_pref";

    private static final String CURRENT_USER_ID_PREF = "ss_current_user_id_pref";

    private static final String CURRENT_USER_EMAIL_PREF = "ss_current_user_email_pref";

    private static final String CURRENT_USERNAME_PREF = "ss_current_username_pref";

    private static final String CURRENT_USER_BLACKLISTED_PREF = "ss_current_user_blacklisted_pref";

    private static final String CURRENT_USER_TOKEN_PREF = "ss_current_user_token_pref";

    private static final String APP_SHARED_PREFS = AppPreferences.class.getSimpleName();

    private SharedPreferences sharedPrefs = null;

    private SharedPreferences.Editor prefsEditor = null;

    public AppPreferences(Context context) {
        this.sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this.prefsEditor = sharedPrefs.edit();
    }

    public SharedPreferences getSharedPrefs() {
        return sharedPrefs;
    }

    public String getUserNamePref() {
        return sharedPrefs.getString(USER_NAME, "");
    }

    public void setUserNamePref(String value) {
        if (value == null) {
            prefsEditor.remove(USER_NAME);
        } else {
            prefsEditor.putString(USER_NAME, value);
        }
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

    public int getCurrentUserIdPref() {
        return sharedPrefs.getInt(CURRENT_USER_ID_PREF, 0);
    }

    public void setCurrentUserIdPref(int value) {
        prefsEditor.putInt(CURRENT_USER_ID_PREF, value);
        prefsEditor.commit();
    }

    public String getCurrentUserEmailPref() {
        return sharedPrefs.getString(CURRENT_USER_EMAIL_PREF, "");
    }

    public void setCurrentUserEmailPref(String value) {
        if (value == null) {
            prefsEditor.remove(CURRENT_USER_EMAIL_PREF);
        } else {
            prefsEditor.putString(CURRENT_USER_EMAIL_PREF, value);
        }
        prefsEditor.commit();
    }

    public String getCurrentUsernamePref() {
        return sharedPrefs.getString(CURRENT_USERNAME_PREF, "");
    }

    public void setCurrentUsernamePref(String value) {
        if (value == null) {
            prefsEditor.remove(CURRENT_USERNAME_PREF);
        } else {
            prefsEditor.putString(CURRENT_USERNAME_PREF, value);
        }

        prefsEditor.commit();
    }

    public Boolean getCurrentUserBlacklistedPref() {
        return sharedPrefs.getBoolean(CURRENT_USER_BLACKLISTED_PREF, false);
    }

    public void setCurrentUserBlacklistedPref(Boolean value) {
        if (value == null) {
           prefsEditor.remove(CURRENT_USER_BLACKLISTED_PREF);
        } else {
            prefsEditor.putBoolean(CURRENT_USER_BLACKLISTED_PREF, value);
        }

        prefsEditor.commit();
    }

    public String getCurrentUserToken() {
        return sharedPrefs.getString(CURRENT_USER_TOKEN_PREF, "");
    }

    public void setCurrentUserTokenPref(String value) {
        if (value == null) {
            prefsEditor.remove(CURRENT_USER_TOKEN_PREF);
        } else {
            prefsEditor.putString(CURRENT_USER_TOKEN_PREF, value);
        }
        prefsEditor.commit();
    }
}
