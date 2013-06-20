package com.streetshout.android.Utils;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
}
