package com.streetshout.android;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONObject;

public class MainActivity extends Activity {

    /** Required recentness and accuracy for creating a bubble */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;

    private LocationManager locationManager = null;
    private LocationListener listener = null;

    private Location bestLoc = null;

    private AQuery aq = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.aq = new AQuery(this);

        setContentView(R.layout.main);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (LocationUtils.isBetterLocation(location, MainActivity.this.bestLoc)) {
                    MainActivity.this.bestLoc = location;
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
            LocationUtils.enableLocationSettings(this);
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
    public void createShoutBtnPressed(View v) {
        if (this.bestLoc != null && (System.currentTimeMillis() - this.bestLoc.getTime() < REQUIRED_RECENTNESS)) {
            String description = ((EditText) this.findViewById(R.id.shout_description_input)).getText().toString();

            ShoutUtils.createShout(aq, bestLoc.getLatitude(), bestLoc.getLongitude(), description
                                                                                      , new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);
                    Toast toast = Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        } else {
            Toast toast = Toast.makeText(MainActivity.this, "No good location available!", Toast.LENGTH_LONG);
            toast.show();
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
