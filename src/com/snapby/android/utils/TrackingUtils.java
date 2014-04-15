package com.snapby.android.utils;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.snapby.android.models.Shout;
import com.snapby.android.models.User;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 1/29/14.
 */
public class TrackingUtils {

    public static MixpanelAPI getMixpanel(Context ctx) {
        return ((StreetShoutApplication) ctx.getApplicationContext()).getMixpanel();
    }

    public static void identify(Context ctx, User user) {
        MixpanelAPI.People people = getMixpanel(ctx).getPeople();

        people.identify(String.valueOf(user.id));

        people.set("Username", user.username);
        people.set("Email", user.email);
    }

    public static void trackCreateShout(Context ctx) {
        JSONObject properties = new JSONObject();

        getMixpanel(ctx).track("Create shout", properties);

        getMixpanel(ctx).getPeople().increment("Create shout count", 1);
    }

    public static void trackAppOpened(Context ctx) {
        JSONObject properties = new JSONObject();
        try {
            if (SessionUtils.isSignIn(ctx)) {
                properties.put("Signed in", "Yes");
            } else {
                properties.put("Signed in", "No");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getMixpanel(ctx).track("Open app", properties);

        getMixpanel(ctx).getPeople().increment("Open app count", 1);
    }

    public static void trackSignup(Context ctx, String source) {
        JSONObject properties = new JSONObject();
        try {
            properties.put("Source", source);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getMixpanel(ctx).track("Sign up", properties);
    }
}
