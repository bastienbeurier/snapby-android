package com.streetshout.android.Utils;

import android.app.Activity;
import com.streetshout.android.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Tools relative to date and time.
 */
public class TimeUtils {
    public static final long ONE_MIN = 60 * 1000;
    public static final long ONE_HOUR = 60 * 60 * 1000;

    public static long getShoutAge(String dateCreated) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = format.parse(dateCreated);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (new Date()).getTime() - date.getTime();
    }

    public static String shoutAgeToString(Activity activity, long age) {
        if (age > 0) {
            long hours = age / ONE_HOUR;
            if (hours > 1) {
                return String.valueOf(hours) + " " + activity.getString(R.string.hours_ago);
            } else if (hours == 1) {
                return String.valueOf(hours) + " " + activity.getString(R.string.hour_ago);
            } else {
                long minutes = age / ONE_MIN;
                if (minutes > 1) {
                    return String.valueOf(minutes) + " " + activity.getString(R.string.minutes_ago);
                } else if (minutes == 1) {
                    return String.valueOf(minutes) + " " + activity.getString(R.string.minute_ago);
                } else {
                    return activity.getString(R.string.just_now);
                }
            }
        } else {
            return activity.getString(R.string.just_now);
        }
    }
}
