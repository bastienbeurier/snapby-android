package com.streetshout.android.Utils;

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

    public static String shoutAgeToString(long age) {
        if (age > 0) {
            long hours = age / ONE_HOUR;
            if (hours > 1) {
                return String.format("%d hrs ago", hours);
            } else if (hours == 1) {
                return String.format("1 hr ago");
            } else {
                long minutes = age / ONE_MIN;
                if (minutes > 1) {
                    return String.format("%d mins ago", minutes);
                } else {
                    return String.format("%d min ago", minutes);
                }
            }
        } else {
            return "just now";
        }
    }
}
