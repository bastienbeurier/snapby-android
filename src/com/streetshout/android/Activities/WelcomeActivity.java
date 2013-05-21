package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Window;
import com.streetshout.android.R;
import com.streetshout.android.Utils.LocationUtils;

public class WelcomeActivity extends Activity {

    /** Location manager that handles the network services */
    private LocationManager locationManager = null;

    /** Location listener to get location from network services */
    private LocationListener listener = null;

    private Thread thread = null;

    private boolean locationFound = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.welcome);

        //Set up location service: trying to get user location in WelcomeActivity to display map faster in MainActivity
        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        //Create listener for location service responses
        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                locationFound = true;
                if (thread != null) {
                    thread.interrupt();
                }

                //When we have the user location, start MainActivity
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

        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        //If location services disabled, user is invited to edit settings
        if (!networkEnabled) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            //OK: Redirect user to edit location settings
            builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    LocationUtils.enableLocationSettings(WelcomeActivity.this);
                    WelcomeActivity.this.finish();
                }
            });
            //DISMISS: MainActivity without user location
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

        //Request location to network provider
        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,listener);

            thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(5000);

                        //Waiting to long for user location, start activity (retrieve city somehow?)
                        Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
                        startActivity(i);
                        WelcomeActivity.this.finish();

                        interrupt();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Disable location services
        this.locationManager.removeUpdates(listener);

        thread = null;
    }
}