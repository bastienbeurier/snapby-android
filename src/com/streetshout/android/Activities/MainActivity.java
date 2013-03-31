package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.Utils.MapRequestHandler;
import com.streetshout.android.R;
import com.streetshout.android.Utils.LocationUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** Set of shout ids to keep track of shouts already added to the map */
    private Set<Integer> markedShouts = null;

    private boolean canCreateShout = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);


        this.aq = new AQuery(this);
        mapReqHandler = new MapRequestHandler();

        markedShouts = new HashSet<Integer>();

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
                //Set listener and send calls to the MapRequestHandler
                pullShoutsOnMap(cameraPosition);
                }
            });

            return true;
        }
        return false;
    }

    /** Set listener for MapRequestHandler responses and add requests to this handler */
    private void pullShoutsOnMap(CameraPosition cameraPosition) {
        //Set listener to catch API response from the MapRequestHandler
        mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
            @Override
            public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                if (status.getError() == null) {

                    JSONArray rawResult = null;
                    try {
                        rawResult = object.getJSONArray("result");

                        //Get ShoutModel instances for a raw JSONArray
                        List<ShoutModel> shouts = ShoutModel.rawShoutsToInstances(rawResult);

                        //Add received shours on the map
                        addShoutsOnMap(shouts);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //Add a request to populate the map to the MapRequestHandler
        mapReqHandler.addMapRequest(aq, cameraPosition);
    }

    /** Add a list of shouts on the map */
    private void addShoutsOnMap(List<ShoutModel> shouts) {

        for (ShoutModel shout: shouts) {
            //Check that the shout is not already marked on the map
            if (!markedShouts.contains(shout.id)) {
                MarkerOptions marker = new MarkerOptions();
                marker.position(new LatLng(shout.lat, shout.lng));
                marker.title(shout.description).snippet(shout.created);
                marker.draggable(true);
                mMap.addMarker(marker);

                //Keep track that we added the shout on the map
                markedShouts.add(shout.id);
            }
        }
    }

    /** User clicks on the button to create a new bubble */
    public void createShoutBtnPressed(View v) {
        if (this.bestLoc != null && (System.currentTimeMillis() - this.bestLoc.getTime() < REQUIRED_RECENTNESS)) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();

            builder.setTitle(getString(R.string.create_shout_dialog_title));

            builder.setView(inflater.inflate(R.layout.create_shout, null));

            //OK: Redirect user to edit location settings
            builder.setPositiveButton(R.string.shout, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String description = ((EditText) ((AlertDialog) dialog).findViewById(R.id.create_shout_descr_dialog)).getText().toString();
                    createShoutConfirmed(description);
                }
            });
            //DISMISS: MainActivity without user location
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                   //Dismiss
                }
            });

            Dialog dialog = builder.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (canCreateShout) {
                            canCreateShout = false;
                            String description = ((EditText) ((AlertDialog) dialog).findViewById(R.id.create_shout_descr_dialog)).getText().toString();
                            dialog.dismiss();
                            createShoutConfirmed(description);
                        } else {
                            canCreateShout = true;
                        }
                    }
                    return false;
                }
            });
            dialog.show();
        } else {
            Toast toast = Toast.makeText(MainActivity.this, "No good location available!", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void createShoutConfirmed(String description) {
        ShoutModel.createShout(aq, bestLoc.getLatitude(), bestLoc.getLongitude(), description
                , new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);
                Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_success), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }
}
