package com.streetshout.android.utils;

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

    //Returns long in milliseconds
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

    public static String[] shoutAgeToStrings(Activity activity, long age) {
        String[] result = new String[2];

        if (age > 0) {
            long hours = age / ONE_HOUR;
            if (hours > 1) {
                if (age > Constants.SHOUT_DURATION) {
                    result[0] = activity.getString(R.string.expired);
                    result[1] = "";
                } else {
                    result[0] = String.valueOf(hours);
                    result[1] = activity.getString(R.string.hours);
                }
            } else if (hours == 1) {
                result[0] = String.valueOf(hours);
                result[1] = activity.getString(R.string.hour);
            } else {
                long minutes = age / ONE_MIN;
                if (minutes > 1) {
                    result[0] = String.valueOf(minutes);
                    result[1] = activity.getString(R.string.minutes);
                } else if (minutes == 1) {
                    result[0] = String.valueOf(minutes);
                    result[1] = activity.getString(R.string.minute);
                } else {
                    result[0] = activity.getString(R.string.now);
                    result[1] = "";
                }
            }
        } else {
            result[0] = activity.getString(R.string.now);
            result[1] = "";
        }


        return result;
    }

    public static String[] shoutAgeToShortStrings(long age) {
        String[] result = new String[2];

        if (age > 0) {
            long hours = age / ONE_HOUR;
            if (hours >= 1) {
                result[0] = String.valueOf(hours);
                result[1] = "h";
            } else {
                long minutes = age / ONE_MIN;
                if (minutes >= 1) {
                    result[0] = String.valueOf(minutes);
                    result[1] = "min";
                } else {
                    result[0] = "0";
                    result[1] = "min";
                }
            }
        } else {
            result[0] = "0";
            result[1] = "min";
        }

        return result;
    }
}
