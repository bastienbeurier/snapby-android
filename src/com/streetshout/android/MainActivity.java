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
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import org.json.JSONObject;

public class MainActivity extends Activity {

    /** Required recentness and accuracy for creating a bubble */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;
    private static final int INITIAL_ZOOM = 11;

    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private MapRequestHandler mapReqHandler = null;
    private GoogleMap.OnCameraChangeListener cameraListener = null;
    private Location bestLoc = null;
    private AQuery aq = null;
    private GoogleMap mMap;
    private boolean initCamera = false;
    private Location firstLoc = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.aq = new AQuery(this);
        mapReqHandler = new MapRequestHandler();

        setContentView(R.layout.main);

        //Check if we have the user position from the Welcome Activity
        if (getIntent().hasExtra("firstLocation")) {
            firstLoc = (Location) getIntent().getParcelableExtra("firstLocation");
            bestLoc = firstLoc;
        }

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        locationListener = new LocationListener() {

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
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
        }

        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,locationListener);
        }

        boolean newMap = setUpMapIfNeeded();

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            initCamera = false;
            if (firstLoc != null) {
                bestLoc = (Location) getIntent().getParcelableExtra("firstLocation");
                initializeCamera();

                //Now camera has been initialized
                initCamera = true;

                //Only use the first location once
                firstLoc = null;
            }
        }
    }

    /** If no map already present, set up map with settings, event listener, ... Return true if a new map is set */
    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            //Instantiate map
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            //Set map settings
            UiSettings settings = mMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setCompassEnabled(true);
            settings.setMyLocationButtonEnabled(true);
            settings.setRotateGesturesEnabled(false);
            settings.setTiltGesturesEnabled(false);

            //Set camera move listener
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
                        @Override
                        public void responseReceived(JSONObject object, AjaxStatus status) {
                            Log.d("BAB", "Server response: " + object);
                        }
                    });
                    mapReqHandler.addMapRequest(aq, cameraPosition);
                }
            });

            return true;
        }
        return false;
    }

    /** Set initial camera position on the user location */
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
