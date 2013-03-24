package com.streetshout.android;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import org.json.JSONObject;

public class MainActivity extends Activity {

    /** Required recentness and accuracy for creating a bubble */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;
    private static final int INITIAL_ZOOM = 11;

    private LocationManager locationManager = null;
    private LocationListener listener = null;
    private Location bestLoc = null;
    private AQuery aq = null;
    private GoogleMap mMap;
    private boolean initCamera = false;
    private Location firstLoc = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.aq = new AQuery(this);

        setContentView(R.layout.main);

        if (getIntent().hasExtra("firstLocation")) {
            firstLoc = (Location) getIntent().getParcelableExtra("firstLocation");
            bestLoc = firstLoc;
        }

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        listener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (LocationUtils.isBetterLocation(location, MainActivity.this.bestLoc)) {
                    MainActivity.this.bestLoc = location;
                    if (mMap != null && !initCamera) {
                        initializeCamera();
                        initCamera = true;
                    }
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, listener);
        }

        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,listener);
        }

        boolean newMap = setUpMapIfNeeded();
        if (newMap) {
            initCamera = false;
            if (firstLoc != null) {
                bestLoc = (Location) getIntent().getParcelableExtra("firstLocation");
                initializeCamera();
                initCamera = true;

                //Only use the first location from Welcome Activity once
                firstLoc = null;
            }
        }
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            return true;
        }
        return false;
    }

    private void initializeCamera() {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(bestLoc), INITIAL_ZOOM);
        mMap.moveCamera(update);
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
}
