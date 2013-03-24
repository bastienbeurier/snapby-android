package com.streetshout.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class WelcomeActivity extends Activity {

    private LocationManager locationManager = null;
    private LocationListener listener = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
                i.putExtra("firstLocation", location);
                startActivity(i);
                WelcomeActivity.this.finish();
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        final boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    LocationUtils.enableLocationSettings(WelcomeActivity.this);
                    WelcomeActivity.this.finish();
                }
            });
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(i);
                    WelcomeActivity.this.finish();
                }
            });

            builder.setTitle(getString(R.string.no_location_dialog_title));
            builder.setMessage(R.string.no_location_dialog_message);

            builder.create().show();

        }

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, listener);
        }

        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,listener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.locationManager.removeUpdates(listener);
    }
}