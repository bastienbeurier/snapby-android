package com.streetshout.android.utils;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.streetshout.android.models.User;

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
}
