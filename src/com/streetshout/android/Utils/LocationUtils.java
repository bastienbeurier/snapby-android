package com.streetshout.android.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.streetshout.android.R;

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

    public static String[] formattedDistanceStrings(Context ctx, Location loc1, Location loc2) {
        int distance = (int) (loc1.distanceTo(loc2));

        AppPreferences appPrefs = new AppPreferences(ctx.getApplicationContext());

        if (appPrefs.getDistanceUnitPref() == 1) {
            return formatedDistanceInMiles(ctx, distance);
        } else {
            return formatedDistanceInMeters(ctx, distance);
        }
    }

    public static String[] formatedDistanceInMeters(Context ctx, int distance) {
        String[] result = new String[2];

        if (distance < 100) {
            result[0] = String.format("%d", distance);
            result[1] = "m";
        } else if (distance < 1000) {
            result[0] = String.format("%d", Math.round(distance / 100.0) * 100);
            result[1] = "m";
        } else if (distance < 10000) {
            result[0] = String.format("%d", Math.round(distance / 1000.0));
            result[1] = "km";
        } else if (distance < 100000 ) {
            result[0] = String.format("%d", Math.round(distance / 10000.0) * 10);
            result[1] = "km";
        } else {
            result[0] = "+100";
            result[1] = "km";
        }

        return result;
    }

    public static String[] formatedDistanceInMiles(Context ctx, int distance) {
        String[] result = new String[2];

        long distanceYd = Math.round(distance * 1.09361);
        long distanceMiles = Math.round(distance * 0.000621371);

        if (distanceYd < 100) {
            result[0] = String.format("%d", distance);
            result[1] = "yd";
        } else if (distanceMiles < 1) {
            result[0] = String.format("%d", Math.round(distanceYd / 100.0) * 100);
            result[1] = "yd";
        } else if (distanceMiles < 10) {
            result[0] = String.format("%d", distanceMiles);
            result[1] = "mi";
        } else if (distanceMiles < 100 ) {
            result[0] = String.format("%d", Math.round(distanceMiles / 10.0) * 10);
            result[1] = "mi";
        } else {
            result[0] = "+100";
            result[1] = "mi";
        }

        return result;
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

    public static void checkLocationServicesEnabled(final Context ctx, LocationManager locationManager) {
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {}

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
            dialog.setTitle(ctx.getText(R.string.no_location_dialog_title));
            dialog.setMessage(ctx.getText(R.string.no_location_dialog_message));
            dialog.setPositiveButton(ctx.getText(R.string.settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent settings = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    ctx.startActivity(settings);
                }
            });
            dialog.setNegativeButton(ctx.getText(R.string.skip), null);
            dialog.show();
        }
    }

    public static Location getLastLocationWithLocationManager(Context ctx, LocationManager locationManager) {
        if (locationManager == null) {
            locationManager = (LocationManager) ctx.getSystemService(ctx.LOCATION_SERVICE);
        }

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        return locationManager.getLastKnownLocation(provider);
    }

    public static LocationRequest createLocationRequest(int priority, int interval) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(priority);
        locationRequest.setInterval(interval);

        return locationRequest;
    }

    public static void googlePlayServicesFailure(final Context ctx) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle(ctx.getText(R.string.no_google_play_services_title));
        dialog.setMessage(ctx.getText(R.string.no_google_play_services_message));
        dialog.setPositiveButton("ok", null);
        dialog.show();
    }
}
