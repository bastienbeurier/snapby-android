package com.streetshout.android.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import com.google.android.gms.maps.model.LatLng;

/**
 * Tools relative to location.
 */
public class LocationUtils {

    private static final double MIN_LAT = -90;
    private static final double MAX_LAT = 90;
    private static final double MIN_LNG = -180;
    private static final double MAX_LNG = 180;
    private static final double EARTH_RADIUS = 6370997;
    private static final double EARTH_CIRCUMFERENCE = EARTH_RADIUS * 2 * Math.PI;


    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Significant accuracy to determine if a newer and less precised shout should be favored against an older one */
    private static final int SIGNIFICANT_ACCURACY = 200;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > SIGNIFICANT_ACCURACY;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /** Redirects the users to his settings to enable location services */
    public static void enableLocationSettings(Context ctx) {
        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        ctx.startActivity(settingsIntent);
    }

    /** Given a Location object, returns a LatLng*/
    public static LatLng toLatLng(Location loc) {
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }


    public static LatLng[] getLatLngBounds(double circleRadius, Location currentLoc) {

        if (EARTH_RADIUS < 0d || circleRadius < 0d)
            throw new IllegalArgumentException();

        // angular distance in radians on a great circle
        double radDist = circleRadius / EARTH_RADIUS;

        double radLat = Math.toRadians(currentLoc.getLatitude());
        double radLng = Math.toRadians(currentLoc.getLongitude());

        double minLat = radLat - radDist;
        double maxLat = radLat + radDist;

        double minLng, maxLng;
        if (minLat > MIN_LAT && maxLat < MAX_LAT) {
            double deltaLng = Math.asin(Math.sin(radDist) /Math.cos(radLat));
            minLng = radLng - deltaLng;

            if (minLng < MIN_LNG)
                minLng += 2d * Math.PI;

            maxLng = radLng + deltaLng;

            if (maxLng > MAX_LNG)
                maxLng -= 2d * Math.PI;
        } else {
            // a pole is within the distance
            minLat = Math.max(minLat, MIN_LAT);
            maxLat = Math.min(maxLat, MAX_LAT);
            minLng = MIN_LNG;
            maxLng = MAX_LNG;
        }

        minLat = Math.toDegrees(minLat);
        minLng = Math.toDegrees(minLng);
        maxLat = Math.toDegrees(maxLat);
        maxLng = Math.toDegrees(maxLng);

        LatLng southWest = new LatLng(minLat, minLng);
        LatLng northEast = new LatLng(maxLat, maxLng);

        return new LatLng[] {southWest, northEast};
    }

    /** For a given Google Map zoom level and a screen display, returns the max height/width distance in kilometers*/
    public static int zoomToKm(float zoom, Display display) {
        Point size = new Point();
        display.getSize(size);
        int maxPixels = Math.max(size.x, size.y);

        return (int) ((EARTH_CIRCUMFERENCE / 1000) / Math.pow(2, Math.round(zoom) - 1)) * (maxPixels / (256 * 3));
    }
}
