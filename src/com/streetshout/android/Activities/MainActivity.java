package com.streetshout.android.Activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingMapActivity;
import com.streetshout.android.Adapters.MapWindowAdapter;
import com.streetshout.android.Adapters.ShoutFeedEndlessAdapter;
import com.streetshout.android.Custom.PermanentToast;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.Utils.*;
import com.streetshout.android.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends SlidingMapActivity implements GoogleMap.OnMyLocationChangeListener {

    private static final boolean ADMIN = true;
    public static final int MAX_USER_NAME_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    public static final int CLICK_ON_SHOUT_ZOOM = 16;

    public static final int REGULAR_MODE = 0;
    public static final int SHOUT_LOCATION_MODE = 1;
    public static final int SHOUT_CONTENT_MODE = 2;

    private boolean no_twitter = true;

    /** Zoom for the initial camera position when we have the user location */
    private static final int INITIAL_ZOOM = 11;

    /** Minimum radius around the user's location where he can create shout **/
    private static final int SHOUT_RADIUS = 300;

    /** Location manager that handles the network services */
    private LocationManager locationManager = null;

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

    private  AppPreferences appPrefs = null;

    /** Permanent toast to display intructions to the user while creating a shout */
    private PermanentToast permanentToast = null;

    private ListView feedListView = null;

    private int mode = REGULAR_MODE;

    private Location shoutAccurateLocation = null;

    private Location shoutInitialLocation = null;

    private Marker shoutLocationArrow = null;

    private GoogleMap.OnMarkerClickListener storeClickedMarker = null;

    private Marker currentOpenInfoWindow = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        displayMainActionBar();
        setSlidingMenuOptions();
        setGlobalShoutsFeed();

        this.aq = new AQuery(this);

        builder = new CameraPosition.Builder();
        builder.zoom(MainActivity.CLICK_ON_SHOUT_ZOOM);

        markedShouts = new HashSet<Integer>();

        if (savedInstanceState != null) {
            savedCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }
    }

    private void setSlidingMenuOptions() {
        SlidingMenu menu = getSlidingMenu();
        menu.setBehindOffsetRes(R.dimen.action_bar_icon_width);
        setBehindContentView(R.layout.shout_feed);
    }

    private void setGlobalShoutsFeed() {
       feedListView = (ListView) findViewById(R.id.global_shouts_feed);
       feedListView.setEmptyView(findViewById(R.id.empty_feed_view));

       LinearLayout feedHeader = (LinearLayout) findViewById(R.id.feed_shout_header);
       feedHeader.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               refreshShoutFeed();
           }
       });
    }

    private void refreshShoutFeed() {
        feedListView.setAdapter(new ShoutFeedEndlessAdapter(MainActivity.this, aq, mMap));
    }

    @Override
    public void toggle() {
        refreshShoutFeed();
        super.toggle();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //This is where the map is instantiated
        boolean newMap = setUpMapIfNeeded();
        bestLoc = getFirstUserLocation();

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            if (savedCameraPosition != null) {
                initializeCameraWithCameraPosition(savedCameraPosition);
                savedCameraPosition = null;
            } else if (bestLoc != null) {
                initializeCamera(bestLoc);
            }
        }
    }

    private Location getFirstUserLocation() {
        if (locationManager == null) {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        }

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        return locationManager.getLastKnownLocation(provider);
    }

    @Override
    protected void onPause() {
        super.onStop();

        if (permanentToast != null) {
            permanentToast.interrupt();
        }
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
                if (bestLoc != null) {
                    shoutInitialLocation = bestLoc;
                    startShoutCreationProcess();
                } else {
                    Toast toast = Toast.makeText(MainActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
                    toast.show();
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

            //Set user location
            mMap.setMyLocationEnabled(true);

            //Set location listener
            mMap.setOnMyLocationChangeListener(this);

            //SetAdminCapabilities
            if (ADMIN) {
                setAdminCapabilities();
            }

            storeClickedMarker = new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    currentOpenInfoWindow = marker;
                    return false;
                }
            };

            mMap.setOnMarkerClickListener(storeClickedMarker);

            // Gets the my location button
            View myLocationButton = findViewById(2);

            // Checks if we found the my location button
            if (myLocationButton != null){

                // Sets the margin of the button
                ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(myLocationButton.getLayoutParams());
                marginParams.setMargins(0, (int) getResources().getDimension(R.dimen.feed_header_height) + 20, 20, 0);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                myLocationButton.setLayoutParams(layoutParams);
            }

            /** Set camera move listener that sends requests to populate the map to the MapRequestHandler and listen for
             its response */
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                //Set listener and send calls to the MapRequestHandler
                pullShouts(cameraPosition);
                }
            });

            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    CameraUpdate update = CameraUpdateFactory.newCameraPosition(builder.target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude)).build());
                    mMap.animateCamera(update);
                }
            });

            return true;
        }
        return false;
    }

    private void setAdminCapabilities() {
        ToggleButton ffToggle = (ToggleButton) findViewById(R.id.family_friends_toggle);
        if (ADMIN)  {
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

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                shoutAccurateLocation = new Location("");
                shoutAccurateLocation.setLatitude(latLng.latitude);
                shoutAccurateLocation.setLongitude(latLng.longitude);
                getNewShoutDescription();

                if (permanentToast != null) {
                    permanentToast.interrupt();
                    permanentToast = null;
                }
            }
        });
    }

    @Override
    public void onMyLocationChange(Location location) {
        bestLoc = location;
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

    private void startShoutCreationProcess() {
        mode = SHOUT_LOCATION_MODE;

        setShoutPerimeter();

        displayDoneDiscardActionBar();

        GoogleMap.OnMapClickListener updateShoutLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //Convert LatLng to Location
                shoutAccurateLocation = new Location("");
                shoutAccurateLocation.setLatitude(latLng.latitude);
                shoutAccurateLocation.setLongitude(latLng.longitude);
                updateShoutAccuratePosition();
            }
        };

        GoogleMap.OnMarkerDragListener updateShoutLocOnDrag = new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                shoutAccurateLocation = new Location("");
                shoutAccurateLocation.setLatitude(marker.getPosition().latitude);
                shoutAccurateLocation.setLongitude(marker.getPosition().longitude);
                updateShoutAccuratePosition();
            }
        };

        GoogleMap.OnMarkerClickListener disableMarkerClick = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {return true;}
        };

        mMap.setOnMapClickListener(updateShoutLocOnClick);
        mMap.setOnMarkerDragListener(updateShoutLocOnDrag);
        mMap.setOnMarkerClickListener(disableMarkerClick);

        if (currentOpenInfoWindow != null) {
            currentOpenInfoWindow.hideInfoWindow();
            currentOpenInfoWindow = null;
        }
    }

    public void updateShoutAccuratePosition() {
        if (mode == SHOUT_LOCATION_MODE) {
            if (shoutLocationArrow != null) {
                shoutLocationArrow.remove();
                shoutLocationArrow = null;
            }

            //Display marker the user is going to drag to specify his accurate position
            MarkerOptions marker = new MarkerOptions();
            marker.position(new LatLng(shoutAccurateLocation.getLatitude(), shoutAccurateLocation.getLongitude()));
            marker.draggable(true);
            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_arrow));
            shoutLocationArrow = mMap.addMarker(marker);
        }
    }

    private void endShoutCreationProcess() {
        mode = REGULAR_MODE;

        if (shoutLocationArrow != null) {
            shoutLocationArrow.remove();
            shoutLocationArrow = null;
        }

        UiSettings settings = mMap.getUiSettings();
        settings.setZoomGesturesEnabled(true);
        settings.setScrollGesturesEnabled(true);

        mMap.setOnMapClickListener(null);
        mMap.setOnMarkerDragListener(null);
        mMap.setOnMarkerClickListener(storeClickedMarker);

        //Bring initial action bar back
        displayMainActionBar();
    }

    /** Display on the map the zone where the user will be able to drag his shout */
    private void setShoutPerimeter() {
        /**Shout radius is the perimeter where the user could be due to location inaccuracy (there is a minimum radius
        if location is very accurate) */

        //User instructions in a toast
        final Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_instructions), Toast.LENGTH_LONG);
        permanentToast = new PermanentToast(toast);
        permanentToast.start();

        //Compute bouds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(SHOUT_RADIUS, shoutInitialLocation);
        LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        //Update the camera to fit this perimeter
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, SHOUT_RADIUS/15);
        mMap.moveCamera(update);

        UiSettings settings = mMap.getUiSettings();
        settings.setZoomGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
    }

    /** Display shout on the map and add shout id to current shouts */
    private void displayShoutOnMap(ShoutModel shout) {
        MarkerOptions marker = new MarkerOptions();

        marker.position(new LatLng(shout.lat, shout.lng));
        if (shout.displayName != null && shout.displayName.length() > 0 && !shout.displayName.equals("null")) {
            marker.title(shout.displayName);
        }
        String shoutBody = shout.description + GeneralUtils.STAMP_DIVIDER + TimeUtils.shoutAgeToString(this, TimeUtils.getShoutAge(shout.created));
        if (shout.source.equals("twitter")) {
            shoutBody += " " + getString(R.string.powered_by_twitter);
        }

        marker.snippet(shoutBody);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
        markedShouts.add(shout.id);
        mMap.addMarker(marker);
    }

    private void displayDoneDiscardActionBar() {
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
                permanentToast = null;
                actionBar.hide();
                getNewShoutDescription();
            }
        });

        //Discard button
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                permanentToast.interrupt();
                permanentToast = null;
                endShoutCreationProcess();
            }
        });

        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        actionBar.show();
    }

    private void getNewShoutDescription() {
        mode = SHOUT_CONTENT_MODE;

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
                endShoutCreationProcess();
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
                        validateShoutInfo((AlertDialog) dialog);
                    } else {
                        canCreateShout = true;
                    }
                }
                return false;
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                endShoutCreationProcess();
            }
        });

        dialog.show();

        //Set user name if we have it
        EditText userNameView = (EditText) dialog.findViewById(R.id.create_shout_descr_dialog_name);
        final EditText descriptionView = (EditText) dialog.findViewById(R.id.create_shout_descr_dialog_descr);
        final TextView charCountView = (TextView) dialog.findViewById(R.id.create_shout_descr_dialog_count);

        appPrefs = new AppPreferences(getApplicationContext());

        String savedUserName = appPrefs.getUserNamePref();
        if (savedUserName.length() > 0) {
            userNameView.setText(savedUserName);
            userNameView.clearFocus();
            descriptionView.requestFocus();
        }

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
                validateShoutInfo(dialog);
            }
        });

    }

    private void validateShoutInfo(AlertDialog dialog) {
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
            createNewShoutFromInfo(userName, description);
            endShoutCreationProcess();
        }
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void createNewShoutFromInfo(String userName, String description) {
        double lat;
        double lng;

        //Save user name in prefs
        appPrefs.setUserNamePref(userName);

        lat = shoutAccurateLocation == null ? shoutInitialLocation.getLatitude() : shoutAccurateLocation.getLatitude();
        lng = shoutAccurateLocation == null ? shoutInitialLocation.getLongitude() : shoutAccurateLocation.getLongitude();
        shoutInitialLocation = null;
        shoutAccurateLocation = null;

        final Toast processingToast = Toast.makeText(MainActivity.this, getString(R.string.shout_processing), Toast.LENGTH_LONG);
        processingToast.show();

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
                    processingToast.cancel();
                    Toast toast = Toast.makeText(MainActivity.this, getString(R.string.create_shout_success), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}
