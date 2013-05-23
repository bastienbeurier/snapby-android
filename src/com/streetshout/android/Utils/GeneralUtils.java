package com.streetshout.android.Utils;

import android.app.Activity;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;

public class GeneralUtils {
    public static final String STAMP_DIVIDER = "tttimestamppp";

    public static int getVerticalWindowWitdhInDp(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density  = activity.getResources().getDisplayMetrics().density;
        int dpHeight = Math.round(outMetrics.heightPixels / density);
        int dpWidth  = Math.round(outMetrics.widthPixels / density);

        return Math.min(dpHeight, dpWidth);
    }

    public static int getWindowHeight(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }
}
