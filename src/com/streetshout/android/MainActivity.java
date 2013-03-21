package com.streetshout.android;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    /** Required recentness and accuracy for creating a bubble */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;

    private LocationManager locationManager = null;
    private LocationListener listener = null;

    private Location currentBestLocation = null;
    private Location currentAccurateLocation = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                printLocationLogs(location, "provider");

                if (Utils.isBetterLocation(location, MainActivity.this.currentBestLocation)) {
                    MainActivity.this.currentBestLocation = location;
                }

                if (location.getAccuracy() < REQUIRED_ACCURACY) {
                    MainActivity.this.currentAccurateLocation = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                //Nothing
            }

            @Override
            public void onProviderEnabled(String provider) {
                //Nothing
            }

            @Override
            public void onProviderDisabled(String provider) {
                //Nothing
            }
        };

        final boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);



        if (!gpsEnabled && !networkEnabled) {
            Utils.enableLocationSettings(this);
            this.finish();
        }

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, listener);
        }

        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,listener);
        }
    }

    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(listener);
    }

    /** User clicks on the button to create a new bubble */
    public void createBubbleBtnPressed(View v) {
        printLocationLogs(currentAccurateLocation, "bubble");
        if (this.currentAccurateLocation != null &&
                          (System.currentTimeMillis() - this.currentAccurateLocation.getTime() < REQUIRED_RECENTNESS)) {
            ((TextView) this.findViewById(R.id.display_latlng)).setText("Lat: " + currentAccurateLocation.getLatitude() + ", lng: " + currentAccurateLocation.getLongitude());
        } else {
            ((TextView) this.findViewById(R.id.display_latlng)).setText("No location available");
        }
    }

    private void printLocationLogs(Location location, String locationType) {
        if (locationType.equals("bubble")) {
            Log.d("BAB", "NEW BUBBLE LOCATION");
        } else {
            Log.d("BAB", "NEW LOCATION PROVIDED");
        }
        Log.d("BAB", "lat: " + location.getLatitude());
        Log.d("BAB", "lng: " + location.getLongitude());
        Log.d("BAB", "provider: " + location.getProvider());
        long ago = (System.currentTimeMillis() - location.getTime()) / 1000;
        Log.d("BAB", "time: " + ago);
    }
}
