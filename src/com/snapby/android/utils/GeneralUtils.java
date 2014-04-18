package com.snapby.android.utils;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import com.androidquery.AQuery;
import com.snapby.android.R;
import com.snapby.android.models.Snapby;

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

    public static int getSnapbyMarkerImageResource(boolean anonymous, boolean selected) {
        if (anonymous) {
            if (selected) {
                return R.drawable.anonymous_marker_selected;
            } else {
                return R.drawable.anonymous_marker;
            }
        } else {
            if (selected) {
                return R.drawable.marker_selected;
            } else {
                return R.drawable.marker;
            }
        }
    }

    public static int getSnapbyAgeCode(Context ctx, Snapby snapby) {
        long snapbyAge = TimeUtils.getAge(snapby.created);

        if (snapbyAge < Constants.SHOUT_DURATION / 4) {
            return 1;
        } else if (snapbyAge < 3 * (Constants.SHOUT_DURATION / 4)) {
            return 2;
        } else {
            return 3;
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
        return ((SnapbyApplication) ctx.getApplicationContext()).getAQuery();
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

    public static void shareSnapby(Activity activity) {
        String url = ApiUtils.getUserSiteUrl();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_snapby_text, url));
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_snapby_subject));
        sendIntent.setType("text/plain");

        activity.startActivity(sendIntent);
    }

    public static String getProfileThumbPicturePrefix() {
        return Constants.PRODUCTION ? Constants.THUMB_PROFILE_PICS_URL_PREFIX_PROD : Constants.THUMB_PROFILE_PICS_URL_PREFIX_DEV;
    }

    public static String getProfileBigPicturePrefix() {
        return Constants.PRODUCTION ? Constants.BIG_PROFILE_PICS_URL_PREFIX_PROD : Constants.BIG_PROFILE_PICS_URL_PREFIX_DEV;
    }

    public static String getSnapbySmallPicturePrefix() {
        return Constants.PRODUCTION ? Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD : Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV;
    }

    public static String getSnapbyBigPicturePrefix() {
        return Constants.PRODUCTION ? Constants.BIG_SHOUT_IMAGE_URL_PREFIX_PROD : Constants.BIG_SHOUT_IMAGE_URL_PREFIX_DEV;
    }
}
