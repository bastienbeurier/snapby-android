package com.streetshout.android.Activities;

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
import com.streetshout.android.Utils.MapRequestHandler;
import com.streetshout.android.R;
import com.streetshout.android.Utils.LocationUtils;
import com.streetshout.android.Utils.ShoutUtils;
import org.json.JSONObject;

public class MainActivity extends Activity {

    /** Required recentness and accuracy of the user position for creating a new shout */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;

    /** Zoom for the initial camera position when we have the user location */
    private static final int INITIAL_ZOOM = 11;

    /** Location manager that handles the GPS and network services */
    private LocationManager locationManager = null;

    /** Location listener to get location from GPS and network services */
    private LocationListener locationListener = null;

    /** Instance of the class that handles request to populate a map zone with shouts */
    private MapRequestHandler mapReqHandler = null;

    /** Best user location that we have right now */
    private Location bestLoc = null;

    /** Aquery instance to handle ajax calls to the API */
    private AQuery aq = null;

    /** Google map instance */
    private GoogleMap mMap;

    /** Status to know if the camera has been initialized to user initial position when he opens the app (not
     * initialized if we don't have the user position) */
    private boolean initCamera = false;

    /** First user location that we get from the previous activity (Welcome activity) to set map position faster, it is
     * used only once (set to null after use), if no user location, user gets zoom 0 map by default */
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

        //Set up location service
        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                //If location is significantly better, update bestLoc
                if (LocationUtils.isBetterLocation(location, MainActivity.this.bestLoc)) {
                    MainActivity.this.bestLoc = location;
                }

                //If camera hasn't been initialized, do it
                if (mMap != null && !initCamera) {
                    initializeCamera();
                    initCamera = true;
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

        //Set up GPD and network services for location updates
        final boolean gpsEnabled = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gpsEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);
        }
        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,locationListener);
        }

        //This is where the map is instantiated
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

    /** Set initial camera position on the user location */
    private void initializeCamera() {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(bestLoc), INITIAL_ZOOM);
        mMap.moveCamera(update);
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

            /** Set camera move listener that sends requests to populate the map to the MapRequestHandler and listen for
             its response */
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    //Set listener to catch API response from the MapRequestHandler
                    mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
                        @Override
                        public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                            Log.d("BAB", "Server response: " + object);
                            Log.d("BAB", "URL: " + url);
                        }
                    });

                    //Add a request to populate the map to the MapRequestHandler
                    mapReqHandler.addMapRequest(aq, cameraPosition);
                }
            });

            return true;
        }
        return false;
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
