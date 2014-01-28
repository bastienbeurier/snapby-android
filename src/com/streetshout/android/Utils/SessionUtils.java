package com.streetshout.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.facebook.Session;
import com.streetshout.android.models.User;

/**
 * Created by bastien on 1/27/14.
 */
public class SessionUtils {

    public static void updateCurrentUserInfoInPhone(Context ctx, User user) {
        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserIdPref(user.id);
        appPrefs.setCurrentUserEmailPref(user.email);
        appPrefs.setCurrentUsernamePref(user.username);
        appPrefs.setCurrentUserBlacklistedPref(user.isBlackListed);
    }

    public static void removeCurrentUserInfoInPhone(Context ctx) {
        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserIdPref(0);
        appPrefs.setCurrentUserEmailPref(null);
        appPrefs.setCurrentUsernamePref(null);
        appPrefs.setCurrentUserBlacklistedPref(null);
    }

    public static User getCurrentUser(Context ctx) {
        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        User currentUser = new User();

        currentUser.id = appPrefs.getCurrentUserIdPref();
        currentUser.email = appPrefs.getCurrentUserEmailPref();
        currentUser.username = appPrefs.getCurrentUsernamePref();
        currentUser.isBlackListed = appPrefs.getCurrentUserBlacklistedPref();

        if (currentUser.id != 0 && !currentUser.email.isEmpty() &&
                                                !currentUser.username.isEmpty() && currentUser.isBlackListed != null) {
            return currentUser;
        } else {
            return null;
        }
    }

    public static void saveCurrentUserToken(Context ctx, String authToken) {
        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserTokenPref(authToken);
    }

    public static String getCurrentUserToken(Context ctx) {
        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        String token = appPrefs.getCurrentUserToken();

        if (!token.isEmpty()) {
            return token;
        } else {
            return null;
        }
    }

    public static boolean isSignIn(Context ctx) {
        return getCurrentUser(ctx) != null && getCurrentUserToken(ctx) != null;
    }

    public static void wipeOffCredentials(Context ctx) {
        //Wipe off mixpanel

        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
        }

        AppPreferences appPrefs = ((StreetShoutApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserTokenPref(null);
        removeCurrentUserInfoInPhone(ctx);
    }
}
