package com.streetshout.android.Activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingMapActivity;
import com.streetshout.android.Adapters.ShoutFeedEndlessAdapter;
import com.streetshout.android.Custom.PermanentToast;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.Utils.*;
import com.streetshout.android.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MainActivity extends SlidingMapActivity {

    private static final boolean ADMIN = true;
    private static final boolean FAMILY_AND_FRIENDS = false;
    public static final int MAX_USER_NAME_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    public static final int CLICK_ON_SHOUT_ZOOM = 16;

    private boolean shout_from_anywhere = false;
    private boolean no_twitter = true;

    /** Required recentness and accuracy of the user position for creating a new shout */
    private static final int REQUIRED_RECENTNESS = 1000 * 60 * 2;

    /** Zoom for the initial camera position when we have the user location */
    private static final int INITIAL_ZOOM = 11;

    /** Minimum radius around the user's location where he can create shout **/
    private static final int MIN_SHOUT_RADIUS = 200;

    /** Location manager that handles the network services */
    private LocationManager locationManager = null;

    /** Location listener to get location from network services */
    private LocationListener locationListener = null;

    /** Best user location that we have right now */
    private Location bestLoc = null;

    /** Aquery instance to handle ajax calls to the API */
    private AQuery aq = null;

    /** Google map instance */
    private GoogleMap mMap;

    /** Set of shout ids to keep track of shouts already added to the map */
    private Set<Integer> markedShouts = null;

    /** Because dialog "done" action is triggered twice */
    private boolean canCreateShout = true;

    private CameraPosition savedCameraPosition = null;

    private CameraPosition.Builder builder = null;

    /** Permanent toast to display intructions to the user while creating a shout */
    PermanentToast permanentToast = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setBehindContentView(R.layout.shout_feed);
        displayMainActionBar();

        SlidingMenu menu = getSlidingMenu();
        menu.setBehindWidth(GeneralUtils.getVerticalWindowWitdh(this) - 100);
        menu.setShadowWidth(15);
        menu.setShadowDrawable(R.drawable.sliding_menu_shadow);

        this.aq = new AQuery(this);

        builder = new CameraPosition.Builder();
        builder.zoom(MainActivity.CLICK_ON_SHOUT_ZOOM);

        markedShouts = new HashSet<Integer>();

        if (savedInstanceState != null) {
            savedCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }

        setSpecialCapabilities();
    }

   private void setGlobalShoutsFeed() {
       ListView feedListView = (ListView) findViewById(R.id.global_shouts_feed);
       ShoutFeedEndlessAdapter adapter = new ShoutFeedEndlessAdapter(this, aq, mMap);
       feedListView.setEmptyView(findViewById(R.id.empty_feed_view));
       feedListView.setAdapter(adapter);
   }

    @Override
    protected void onResume() {
        super.onResume();

        //Set up location service
        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //If location is significantly better, update bestLoc
                if (LocationUtils.isBetterLocation(location, MainActivity.this.bestLoc)) {
                    MainActivity.this.bestLoc = location;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        //Set up network services for location updates
        final boolean networkEnabled = this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (networkEnabled) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10 ,locationListener);
        }

        //This is where the map is instantiated
        boolean newMap = setUpMapIfNeeded();

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            if (bestLoc == null && getIntent().hasExtra("firstLocation")) {
                bestLoc = getIntent().getParcelableExtra("firstLocation");
            }

            if (savedCameraPosition != null) {
                initializeCameraWithCameraPosition(savedCameraPosition);
                savedCameraPosition = null;
            } else if (bestLoc != null) {
                initializeCamera(bestLoc);
            }
        }

        setGlobalShoutsFeed();
    }

    @Override
    protected void onPause() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("cameraPosition", mMap.getCameraPosition());
        super.onSaveInstanceState(outState);
    }

    private void setSpecialCapabilities() {
        ToggleButton adminToggle = (ToggleButton) findViewById(R.id.admin_toggle);
        if (ADMIN)  {
            adminToggle.setVisibility(View.VISIBLE);
            adminToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    shout_from_anywhere = isChecked;
                }
            });
        } else {
            adminToggle.setVisibility(View.GONE);
        }

        ToggleButton ffToggle = (ToggleButton) findViewById(R.id.family_friends_toggle);
        if (FAMILY_AND_FRIENDS)  {
            ffToggle.setVisibility(View.VISIBLE);
            ffToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                no_twitter = isChecked;
                }
            });
        } else {
            ffToggle.setVisibility(View.GONE);
        }
    }

    /** Set initial camera position on the user location */
    private void initializeCamera(Location location) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(location), INITIAL_ZOOM);
        mMap.moveCamera(update);
    }

    private void initializeCameraWithCameraPosition(CameraPosition cameraPosition) {
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(update);
    }

    private void displayMainActionBar() {
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        final View mainActionBarView = inflater.inflate(R.layout.actionbar_feed_and_create_shout, null);

        mainActionBarView.findViewById(R.id.create_shout_item_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shout_from_anywhere) {
                    getNewShoutDescription(bestLoc, null, null);
                } else {

                    if (bestLoc != null && (System.currentTimeMillis() - bestLoc.getTime() < REQUIRED_RECENTNESS)) {
                        startShoutCreationProcess(MainActivity.this.bestLoc);
                    } else {
                        Toast toast = Toast.makeText(MainActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }
        });

        mainActionBarView.findViewById(R.id.feed_item_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        actionBar.setCustomView(mainActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.show();
    }

    /** If no map already present, set up map with settings, event listener, ... Return true if a new map is set */
    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            //Instantiate map
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (mMap == null) {
                return false;
            }

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
                pullShouts(cameraPosition);
                }
            });

            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                // Use default InfoWindow frame
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                // Defines the contents of the InfoWindow
                @Override
                public View getInfoContents(Marker marker) {

                    if (marker.getTitle() == null && marker.getSnippet() == null) {
                        return null;
                    }

                    // Getting view from the layout file info_window_layout
                    View v = getLayoutInflater().inflate(R.layout.map_info_window, null);

                    // Getting reference to the TextView to set title
                    TextView userNameView = (TextView) v.findViewById(R.id.map_info_window_title);
                    TextView descriptionView = (TextView) v.findViewById(R.id.map_info_window_body);
                    TextView timeStampView = (TextView) v.findViewById(R.id.map_info_window_stamp);

                    userNameView.setText(marker.getTitle());

                    String[] descriptionAndStamp = TextUtils.split(marker.getSnippet(), GeneralUtils.STAMP_DIVIDER);

                    descriptionView.setText(descriptionAndStamp[0]);
                    timeStampView.setText(descriptionAndStamp[1]);

                    // Returning the view containing InfoWindow contents
                    return v;
                }

            });

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    CameraUpdate update = CameraUpdateFactory.newCameraPosition(builder.target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)).build());
                    mMap.moveCamera(update);
                }
            });

            return true;
        }
        return false;
    }

    /** Set listener for MapRequestHandler responses and add requests to this handler */
    private void pullShouts(CameraPosition cameraPosition) {
        MapRequestHandler mapReqHandler = new MapRequestHandler();

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
        mapReqHandler.addMapRequest(this, aq, cameraPosition, no_twitter);
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

    private void startShoutCreationProcess(Location newShoutLoc) {
        int shoutRadius = Math.max((int) newShoutLoc.getAccuracy(), MIN_SHOUT_RADIUS);

        Circle newShoutCircle = setShoutPerimeterCircle(shoutRadius, newShoutLoc);

        Marker newShoutMarker = getShoutAccuratePosition(shoutRadius, newShoutLoc);

        displayDoneDiscardActionBar(newShoutLoc, newShoutCircle, newShoutMarker);
    }

    private void endShoutCreationProcess(Circle shoutRadiusCircle, Marker newShoutMarker) {
        //Remove circle and position marker
        if (shoutRadiusCircle != null) {
            shoutRadiusCircle.remove();
        }

        if (newShoutMarker != null) {
            newShoutMarker.remove();
        }

        //Bring initial action bar back
        displayMainActionBar();
    }

    /** Display on the map the zone where the user will be able to drag his shout */
    private Circle setShoutPerimeterCircle(int shoutRadius, Location newShoutLoc) {
        /**Shout radius is the perimeter where the user could be due to location inaccuracy (there is a minimum radius
        if location is very accurate) */

        //User instructions in a toast
        final Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_instructions), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, GeneralUtils.getWindowHeight(this) / 2 - 100);
        permanentToast = new PermanentToast(toast);
        permanentToast.start();

        //Compute bouds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(shoutRadius, newShoutLoc);
        LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        //Update the camera to fit this perimeter
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, shoutRadius/15);
        mMap.moveCamera(update);

        //Draw the circle where the user will be able to drag is shout
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(LocationUtils.toLatLng(newShoutLoc)).radius(shoutRadius).strokeWidth(0).fillColor(Color.parseColor("#66327CCB"));
        Circle shoutRadiusCircle = mMap.addCircle(circleOptions);

        return shoutRadiusCircle;
    }

    /** Let the user indicate the accurate position of his shout by dragging a marker within the shout radius */
    private Marker getShoutAccuratePosition(final int shoutRadius, final Location newShoutLoc) {
        //Display marker the user is going to drag to specify his accurate position
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(newShoutLoc.getLatitude(), newShoutLoc.getLongitude()));
        marker.draggable(true);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_arrow));
        Marker newShoutMarker = mMap.addMarker(marker);

        final double initialLat = newShoutLoc.getLatitude();
        final double initialLng = newShoutLoc.getLongitude();

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
                Location.distanceBetween(initialLat, initialLng, marker.getPosition().latitude, marker.getPosition().longitude, distance);

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

        return newShoutMarker;
    }

    /** Display shout on the map and add shout id to current shouts */
    private void displayShoutOnMap(ShoutModel shout) {
        MarkerOptions marker = new MarkerOptions();

        marker.position(new LatLng(shout.lat, shout.lng));
        if (shout.displayName != null && shout.displayName.length() > 0 && !shout.displayName.equals("null")) {
            marker.title(shout.displayName);
        }
        String shoutBody = shout.description + GeneralUtils.STAMP_DIVIDER + TimeUtils.shoutAgeToString(TimeUtils.getShoutAge(shout.created));
        if (shout.source.equals("twitter")) {
            shoutBody += " " + getString(R.string.powered_by_twitter);
        }

        marker.snippet(shoutBody);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
        markedShouts.add(shout.id);
        mMap.addMarker(marker);
    }

    private void displayDoneDiscardActionBar(final Location newShoutLoc, final Circle newShoutCircle, final Marker newShoutMarker) {
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        // Inflate a "Done/Discard" custom action bar view.
        final View customActionBarView = inflater.inflate(R.layout.actionbar_custom_view_done_discard, null);

        //Done button
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permanentToast.interrupt();
                actionBar.hide();
                getNewShoutDescription(newShoutLoc, newShoutCircle, newShoutMarker);
            }
        });

        //Discard button
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permanentToast.interrupt();
                endShoutCreationProcess(newShoutCircle, newShoutMarker);
            }
        });

        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.show();
    }

    private void getNewShoutDescription(final Location newShoutLoc, final Circle shoutRadiusCircle, final Marker newShoutMarker) {
        LayoutInflater inflater = this.getLayoutInflater();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.create_shout_dialog_title));
        builder.setView(inflater.inflate(R.layout.create_shout, null));

        //OK: Redirect user to edit location settings
        builder.setPositiveButton(R.string.shout, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        //DISMISS: MainActivity without user location
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                endShoutCreationProcess(shoutRadiusCircle, newShoutMarker);
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //User press "send" on the keyboard
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    //For some reason, the event get fired twice. This is a hack to send the shout only once.
                    if (canCreateShout) {
                        canCreateShout = false;
                        validateShoutInfo((AlertDialog) dialog, newShoutLoc, shoutRadiusCircle, newShoutMarker);
                    } else {
                        canCreateShout = true;
                    }
                }
                return false;
            }
        });

        dialog.show();

        final EditText descriptionView = (EditText) dialog.findViewById(R.id.create_shout_descr_dialog_descr);
        final TextView charCountView = (TextView) dialog.findViewById(R.id.create_shout_descr_dialog_count);

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void afterTextChanged(Editable s) {
                descriptionView.setError(null);
                charCountView.setText((MAX_DESCRIPTION_LENGTH - s.length()) + " " + getString(R.string.characters));
            }
        });



        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateShoutInfo(dialog, newShoutLoc, shoutRadiusCircle, newShoutMarker);
            }
        });

    }

    private void validateShoutInfo(AlertDialog dialog, final Location newShoutLoc, final Circle shoutRadiusCircle, final Marker newShoutMarker) {
        boolean errors = false;

        EditText userNameView = (EditText) dialog.findViewById(R.id.create_shout_descr_dialog_name);
        EditText descriptionView = (EditText) dialog.findViewById(R.id.create_shout_descr_dialog_descr);
        userNameView.setError(null);
        descriptionView.setError(null);

        String userName = userNameView.getText().toString();
        String description = descriptionView.getText().toString();

        if (userName.length() == 0) {
            userNameView.setError(getString(R.string.name_not_empty));
            errors = true;
        }

        if (userName.length() > MAX_USER_NAME_LENGTH) {
            userNameView.setError(getString(R.string.name_too_long));
            errors = true;
        }

        if (description.length() == 0) {
            descriptionView.setError(getString(R.string.description_not_empty));
            errors = true;
        }

        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            descriptionView.setError(getString(R.string.description_too_long));
            errors = true;
        }

        if (!errors) {
            dialog.dismiss();
            createNewShoutFromInfo(userName, description, newShoutLoc);
            endShoutCreationProcess(shoutRadiusCircle, newShoutMarker);
        }
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void createNewShoutFromInfo(String userName, String description, final Location newShoutLoc) {
        double lat;
        double lng;

        //If a admin capabilities, create shout in the middle on the map
        if (shout_from_anywhere) {
            lat = mMap.getCameraPosition().target.latitude;
            lng = mMap.getCameraPosition().target.longitude;
        //Else get the location specified by the user
        } else {
            lat = newShoutLoc.getLatitude();
            lng = newShoutLoc.getLongitude();
        }

        //Create shout!
        ShoutModel.createShout(aq, lat, lng, userName, description, new AjaxCallback<JSONObject>() {
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
                    Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_success), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}
