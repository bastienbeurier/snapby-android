package com.streetshout.android.Utils;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

public class GeneralUtils {
    public static final String STAMP_DIVIDER = "tttimestamppp";

    public static int getVerticalWindowWitdh(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return Math.min(size.x, size.y);
    }

    public static int getWindowHeight(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }
}
