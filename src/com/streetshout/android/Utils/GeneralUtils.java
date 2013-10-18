package com.streetshout.android.utils;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.provider.Settings;
import com.streetshout.android.R;
import com.streetshout.android.models.ShoutModel;

public class GeneralUtils {
    public static String getAppVersion(Context ctx) {
        try {
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getDeviceId(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static int getShoutAgeColor(Context ctx, ShoutModel shout) {
        long shoutAge = TimeUtils.getShoutAge(shout.created);

        if (shoutAge < Constants.SHOUT_DURATION / 24) {
            return ctx.getResources().getColor(R.color.shoutRed);
        } else if (shoutAge < 23 * (Constants.SHOUT_DURATION / 24)) {
            return ctx.getResources().getColor(R.color.shoutPink);
        } else {
            return ctx.getResources().getColor(R.color.shoutLightPink);
        }
    }

    public static int getShoutMarkerImageResource(ShoutModel shout, boolean selected) {
        long shoutAge = TimeUtils.getShoutAge(shout.created);

        if (shoutAge < Constants.SHOUT_DURATION / 24) {
            if (selected) {
                return R.drawable.marker_shout_red_selected;
            } else {
                return R.drawable.marker_shout_red;
            }
        } else if (shoutAge < 23 * (Constants.SHOUT_DURATION / 24)) {
            if (selected) {
                return R.drawable.marker_shout_pink_selected;
            } else {
                return R.drawable.marker_shout_pink;
            }
        } else {
            if (selected) {
                return R.drawable.marker_shout_lightpink_selected;
            } else {
                return R.drawable.marker_shout_lightpink;
            }
        }
    }
}
