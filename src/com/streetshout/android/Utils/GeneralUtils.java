package com.streetshout.android.utils;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import com.amazonaws.services.simpleemail.model.Content;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.models.Shout;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static int getShoutAgeColor(Context ctx, Shout shout) {
        long shoutAge = TimeUtils.getShoutAge(shout.created);

        if (shoutAge < Constants.SHOUT_DURATION / Constants.SHOUT_DURATION_HOURS) {
            return ctx.getResources().getColor(R.color.shoutRed);
        } else if (shoutAge < 3 * (Constants.SHOUT_DURATION / Constants.SHOUT_DURATION_HOURS)) {
            return ctx.getResources().getColor(R.color.shoutPink);
        } else {
            return ctx.getResources().getColor(R.color.shoutLightPink);
        }
    }

    public static int getShoutMarkerImageResourceAccordingToAge(Shout shout, boolean selected) {
        long shoutAge = TimeUtils.getShoutAge(shout.created);

        if (shoutAge < Constants.SHOUT_DURATION / Constants.SHOUT_DURATION_HOURS) {
            if (selected) {
//                return R.drawable.marker_shout_red_selected;
            } else {
//                return R.drawable.marker_shout_red;
            }
        } else if (shoutAge < 2 * (Constants.SHOUT_DURATION / Constants.SHOUT_DURATION_HOURS)) {
            if (selected) {
//                return R.drawable.marker_shout_pink_selected;
            } else {
//                return R.drawable.marker_shout_pink;
            }
        } else {
            if (selected) {
//                return R.drawable.marker_shout_lightpink_selected;
            } else {
//                return R.drawable.marker_shout_lightpink;
            }
        }

        return 0;
    }

    public static int getShoutMarkerImageResource(Context ctx, Shout shout, boolean selected) {
        if (selected) {
            return R.drawable.selected_shout_marker;
        }

        if (SessionUtils.getCurrentUser(ctx).id == shout.userId) {
            if (shout.trending) {
                return R.drawable.my_shout_marker_trending;
            } else {
                return R.drawable.my_shout_marker;
            }
        }

        //TODO Implement following

        if (shout.anonymous) {
            if (shout.trending) {
                return R.drawable.anonymous_shout_marker_trending;
            } else {
                return R.drawable.anonymous_shout_marker;
            }
        }


        if (shout.trending) {
            return R.drawable.public_shout_marker_trending;
        } else {
            return R.drawable.public_shout_marker;
        }
    }

    public static boolean isValidEmail(String email) {
        boolean isValid = false;

        String expression = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if (matcher.matches()) {
            isValid = true;
        }

        return isValid;
    }

    public static boolean isValidUsername(String username) {
        boolean isValid = false;

        String expression = "[A-Z0-9a-z._+-]{1,20}";
        CharSequence inputStr = username;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if (matcher.matches()) {
            isValid = true;
        }

        return isValid;
    }

    public static AQuery getAquery(Context ctx) {
        return ((StreetShoutApplication) ctx.getApplicationContext()).getAQuery();
    }

    public static Map<String, Object> enrichParamsWithWithGeneralUserAndDeviceInfo(Context ctx, Map<String, Object> parameters) {
        parameters.put("push_token", PushNotifications.getPushToken());
        parameters.put("device_model", Build.BRAND + " " + Build.PRODUCT);
        parameters.put("os_version", Build.VERSION.RELEASE);
        parameters.put("os_type", "android");
        parameters.put("app_version", GeneralUtils.getAppVersion(ctx));
        parameters.put("api_version", Constants.API);

        return parameters;
    }

    public static void shareShout(Activity activity, Shout shout) {
        String url = ApiUtils.getUserSiteUrl() + "/shouts/" + shout.id;
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_shout_text, url));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_shout_subject));
        sendIntent.setType("text/plain");

        activity.startActivity(sendIntent);
    }
}
