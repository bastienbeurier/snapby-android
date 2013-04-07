package com.streetshout.android.Activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.*;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.Utils.MapRequestHandler;
import com.streetshout.android.R;
import com.streetshout.android.Utils.LocationUtils;
import com.streetshout.android.Utils.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MainActivity extends Activity {

    private static final boolean ADMIN = false;
    private boolean admin_super_powers = false;

    /** Required recentness and accuracy of the user position for creating a new shout */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;
    private static final int REQUIRED_ACCURACY = 50;

    /** Zoom for the initial camera position when we have the user location */
    private static final int INITIAL_ZOOM = 11;

    private static final boolean MARKERS_DRAGGABLE = false;

    private static final int MIN_SHOUT_RADIUS = 200;

    private static final int FEED_CREATE_BAR = 1;
    private static final int DONE_DISCARD_BAR = 2;


    /** Location manager that handles the network services */
    private LocationManager locationManager = null;

    /** Location listener to get location from network services */
    private LocationListener locationListener = null;

    /** Instance of the class that handles request to populate a map zone with shouts */
    private MapRequestHandler mapReqHandler = null;

    /** Best user location that we have right now */
    private Location bestLoc = null;

    /** Location a newly created shout */
    private Location newShoutLoc = null;

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

    /** Because dialog "done" action is triggered twice */
    /** Because dialog "done" action is triggered twice */
    private boolean canCreateShout = true;

    UiSettings settings = null;

    Circle shoutRadiusCircle = null;
    Marker newShoutPosition = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);


        this.aq = new AQuery(this);
        mapReqHandler = new MapRequestHandler();

        markedShouts = new HashSet<Integer>();

        setContentView(R.layout.main);

        displayCustomActionBar(FEED_CREATE_BAR);

        ToggleButton adminToggle = (ToggleButton) findViewById(R.id.admin_toggle);

        if (ADMIN)  {
            adminToggle.setVisibility(View.VISIBLE);
            adminToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    admin_super_powers = isChecked;
                }
            });
        } else {
            adminToggle.findViewById(R.id.admin_toggle).setVisibility(View.GONE);
        }

        //Check if we have the user position from the Welcome Activity
        if (getIntent().hasExtra("firstLocation")) {
            firstLoc = getIntent().getParcelableExtra("firstLocation");
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

        //Set up network services for location updates
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

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

    private void displayCustomActionBar(int barCode) {
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        if (barCode == FEED_CREATE_BAR) {
            final View mainActionBarView = inflater.inflate(R.layout.actionbar_feed_and_create_shout, null);

            mainActionBarView.findViewById(R.id.create_shout_item_menu).setOnClickListener(new View.OnClickListener() {
                @Override
            public void onClick(View v) {
                    if (admin_super_powers) {
                        displayCreateShoutDialog();
                    } else {

                        if (bestLoc != null && (System.currentTimeMillis() - bestLoc.getTime() < REQUIRED_RECENTNESS)) {

                            newShoutLoc = MainActivity.this.bestLoc;

                            startShoutLocationMode();

                        } else {
                            Toast toast = Toast.makeText(MainActivity.this, "No good location available!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                }
            });

            actionBar.setCustomView(mainActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        } else if (barCode == DONE_DISCARD_BAR) {
            // Inflate a "Done/Discard" custom action bar view.
            final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_done_discard, null);

            //Done button
            customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayCreateShoutDialog();
                }
            });

            //Discard button
            customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    endShoutLocationMode();
                }
            });

            actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
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
            settings = mMap.getUiSettings();
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
                displayShoutOnMap(shout);
            }
        }
    }

    /** Display shout on the map and add shout id to current shouts */
    private void displayShoutOnMap(ShoutModel shout) {
        MarkerOptions marker = new MarkerOptions();

        marker.position(new LatLng(shout.lat, shout.lng));
        marker.draggable(MARKERS_DRAGGABLE);
        marker.title(shout.description).snippet(TimeUtils.shoutAgeToString(TimeUtils.getShoutAge(shout.created)));
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
        markedShouts.add(shout.id);
        mMap.addMarker(marker);
    }

    private void startShoutLocationMode() {
        int shoutRadius = Math.max((int) newShoutLoc.getAccuracy(), MIN_SHOUT_RADIUS);

        displayShoutPerimeterOnMap(shoutRadius);

        chooseAccurateShoutPosition(shoutRadius);

        displayCustomActionBar(DONE_DISCARD_BAR);
    }

    private void endShoutLocationMode() {
        //Remove circle and position marker
        if (shoutRadiusCircle != null && newShoutPosition != null) {
            shoutRadiusCircle.remove();
            newShoutPosition.remove();
        }

        //Enable moving and zooming camera
        settings.setScrollGesturesEnabled(true);
        settings.setZoomGesturesEnabled(true);

        //Bring initial action bar back
        displayCustomActionBar(FEED_CREATE_BAR);
    }

    /** Display on the map the zone where the user will be able to drag his shout */
    private int displayShoutPerimeterOnMap(int shoutRadius) {
        /**Shout radius is the perimeter where the user could be due to location inaccuracy (there is a minimum radius
        if location is very accurate) */

        //Compute bouds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(shoutRadius, newShoutLoc);
        LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        //Update the camera to fit this perimeter
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, shoutRadius/15);
        mMap.moveCamera(update);

        //Draw the circle where the user will be able to drag is shout
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(LocationUtils.toLatLng(newShoutLoc)).radius(shoutRadius).strokeWidth(0).fillColor(Color.parseColor("#66327CCB"));
        shoutRadiusCircle = mMap.addCircle(circleOptions);

        //Disable moving or zooming the camera
        settings.setScrollGesturesEnabled(false);
        settings.setZoomGesturesEnabled(false);

        return shoutRadius;
    }

    /** Let the user indicate the accurate position of his shout by dragging a marker within the shout radius */
    private void chooseAccurateShoutPosition(final int shoutRadius) {
        //Display marker the user is going to drag to specify his accurate position
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(newShoutLoc.getLatitude(), newShoutLoc.getLongitude()));
        marker.draggable(true);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_arrow));
        newShoutPosition = mMap.addMarker(marker);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                //Nothing
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                //Nothing
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                float[] distance = new float[] {0};
                Location.distanceBetween(newShoutLoc.getLatitude(), newShoutLoc.getLongitude(), marker.getPosition().latitude, marker.getPosition().longitude, distance);

                //If user drags marker outside of the shoutRadius, bring back shout marker to initial position
                if (distance[0] > shoutRadius) {
                    marker.setPosition(LocationUtils.toLatLng(newShoutLoc));
                //Else update shout position
                } else {
                    newShoutLoc.setLatitude(marker.getPosition().latitude);
                    newShoutLoc.setLongitude(marker.getPosition().longitude);
                }
            }
        });
    }

    private void displayCreateShoutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.create_shout, null));

        //OK: Redirect user to edit location settings
        builder.setPositiveButton(R.string.shout, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String description = ((EditText) ((AlertDialog) dialog).findViewById(R.id.create_shout_descr_dialog)).getText().toString();
                sendShoutConfirmed(description);
                endShoutLocationMode();
            }
        });
        //DISMISS: MainActivity without user location
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                endShoutLocationMode();
            }
        });

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //User press "send" on the keyboard
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    //For some reason, the event get fired twice. This is a hack to send the shout only once.
                    if (canCreateShout) {
                        canCreateShout = false;
                        String description = ((EditText) ((AlertDialog) dialog).findViewById(R.id.create_shout_descr_dialog)).getText().toString();
                        dialog.dismiss();
                        sendShoutConfirmed(description);
                        endShoutLocationMode();
                    } else {
                        canCreateShout = true;
                    }
                }
                return false;
            }
        });

        dialog.show();
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void sendShoutConfirmed(String description) {
        double lat;
        double lng;

        //If a admin capabilities, create shout in the middle on the map
        if (admin_super_powers) {
            lat = mMap.getCameraPosition().target.latitude;
            lng = mMap.getCameraPosition().target.longitude;
        //Else get the location specified by the user
        } else {
            lat = newShoutLoc.getLatitude();
            lng = newShoutLoc.getLongitude();
        }

        //Create shout!
        ShoutModel.createShout(aq, lat, lng, description, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);
                if (status.getError() == null) {
                    JSONObject rawShout = null;

                    try {
                        rawShout = object.getJSONObject("result");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    displayShoutOnMap(ShoutModel.rawShoutToInstance(rawShout));
                    newShoutLoc = null;
                    Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_success), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}
