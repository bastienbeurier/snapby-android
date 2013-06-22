package com.streetshout.android.Utils;

import android.location.Location;
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

    public static String formatedDistanceInMeters(Location loc1, Location loc2) {
        int distance = (int) (loc1.distanceTo(loc2));

        if (distance < 100) {
            return "< 100 meters away";
        } else if (distance < 1000) {
            return (Math.round(distance / 100.0) * 100) + " meters away";
        } else if (distance < 10000) {
            return (Math.round(distance / 1000.0)) + "km away";
        } else if (distance < 100000 ) {
            return (Math.round(distance / 10000.0) * 10) + "kms away";
        } else {
            return "far away";
        }
    }
}
