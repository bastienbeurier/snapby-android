package com.snapby.android.utils;

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
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
    public static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;


    //Returns long in milliseconds
    public static long getSnapbyAge(String dateCreated) {
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

    public static String[] snapbyAgeToShortStrings(long age) {
        String[] result = new String[2];

        if (age > 0) {
            long weeks = age / ONE_WEEK;
            long days = age / ONE_DAY;
            long hours = age / ONE_HOUR;
            if (weeks >= 1) {
                result[0] = String.valueOf(weeks);
                result[1] = "w";
            } else if (days >= 1) {
                result[0] = String.valueOf(days);
                result[1] = "d";
            } else if (hours >= 1) {
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

    public static boolean snapbyExpired(String dateCreated) {
        return getSnapbyAge(dateCreated) > Constants.SHOUT_DURATION;
    }
}
