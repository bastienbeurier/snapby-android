package com.streetshout.android.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

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

    public static String formatedDistance(Context ctx, Location loc1, Location loc2) {
        int distance = (int) (loc1.distanceTo(loc2));

        AppPreferences appPrefs = new AppPreferences(ctx.getApplicationContext());

        if (appPrefs.getDistanceUnitPref() == 1) {
            return formatedDistanceInMiles(distance);
        } else {
            return formatedDistanceInMeters(distance);
        }
    }

    public static String formatedDistanceInMeters(int distance) {

        if (distance < 100) {
            return "nearby";
        } else if (distance < 1000) {
            return (Math.round(distance / 100.0) * 100) + " meters away";
        } else if (distance < 10000) {
            return (Math.round(distance / 1000.0)) + "km away";
        } else if (distance < 100000 ) {
            return (Math.round(distance / 10000.0) * 10) + "km away";
        } else {
            return "far away";
        }
    }

    public static String formatedDistanceInMiles(int distance) {
        long distanceYd = Math.round(distance * 1.09361);
        long distanceMiles = Math.round(distance * 0.000621371);

        if (distanceYd < 100) {
            return "nearby";
        } else if (distanceMiles < 1) {
            return (Math.round(distanceYd / 100.0) * 100) + "yd away";
        } else if (distanceMiles < 10) {
            return distanceMiles + "mi away";
        } else if (distanceMiles < 100 ) {
            return (Math.round(distanceMiles / 10.0) * 10) + "mi away";
        } else {
            return "far away";
        }
    }

    public static Address geocodeAddress(Geocoder geocoder, String address, LatLngBounds latLngBounds) {
        Address result = null;

        try {
            List<Address> addressList = null;
            if (latLngBounds != null) {
                addressList = geocoder.getFromLocationName(address, 1, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude);
            } else {
                addressList = geocoder.getFromLocationName(address, 1);
            }

            if (addressList != null && addressList.size() > 0 && addressList.get(0) != null) {
                result = addressList.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
