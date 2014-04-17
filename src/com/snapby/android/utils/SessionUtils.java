package com.snapby.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;
import com.snapby.android.activities.WelcomeActivity;
import com.snapby.android.models.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by bastien on 1/27/14.
 */
public class SessionUtils {

    public static void synchronizeUserInfo(final Activity activity, Location myLocation) {
        ApiUtils.updateUserInfoWithLocation(activity, GeneralUtils.getAquery(activity), myLocation, null, null, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(activity);
                    return;
                }

                if (status.getError() == null) {
                    JSONObject result = null;
                    JSONObject rawUser = null;


                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    User currentUser = User.rawUserToInstance(rawUser);

                    if (currentUser != null) {
                        SessionUtils.updateCurrentUserInfoInPhone(activity, currentUser);
                    }
                }
            }
        });
    }

    public static void updateCurrentUserInfoInPhone(Context ctx, User user) {
        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserIdPref(user.id);
        appPrefs.setCurrentUserEmailPref(user.email);
        appPrefs.setCurrentUsernamePref(user.username);
        appPrefs.setCurrentUserBlacklistedPref(user.isBlackListed);
    }

    public static void removeCurrentUserInfoInPhone(Context ctx) {
        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserIdPref(0);
        appPrefs.setCurrentUserEmailPref(null);
        appPrefs.setCurrentUsernamePref(null);
        appPrefs.setCurrentUserBlacklistedPref(null);
    }

    public static User getCurrentUser(Context ctx) {
        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

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
        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserTokenPref(authToken);
    }

    public static String getCurrentUserToken(Context ctx) {
        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

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
        TrackingUtils.getMixpanel(ctx).flush();

        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
        }

        AppPreferences appPrefs = ((SnapbyApplication) ctx.getApplicationContext()).getAppPrefs();

        appPrefs.setCurrentUserTokenPref(null);
        removeCurrentUserInfoInPhone(ctx);
    }

    public static void logOut(Activity activity) {
        SessionUtils.wipeOffCredentials(activity);

        Intent welcome = new Intent(activity, WelcomeActivity.class);
        welcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(welcome);
        activity.finish();
    }

}
