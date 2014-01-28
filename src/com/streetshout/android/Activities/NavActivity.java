package com.streetshout.android.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.adapters.MapWindowAdapter;
import com.streetshout.android.fragments.FeedFragment;
import com.streetshout.android.fragments.ShoutFragment;
import com.streetshout.android.models.Shout;
import com.streetshout.android.R;
import com.streetshout.android.utils.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavActivity extends Activity implements GoogleMap.OnMyLocationChangeListener, ShoutFragment.OnZoomOnShoutListener, FeedFragment.OnFeedShoutSelectedListener {

    private ConnectivityManager connectivityManager = null;

    private LocationManager locationManager = null;

    private AQuery aq = null;

    private GoogleMap mMap = null;

    /** Set of shout ids to keep track of shouts already added to the map */
    private HashMap<Integer, Shout> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    private Location myLocation = null;

    private CameraPosition savedInstanceStateCameraPosition = null;

    private FeedFragment feedFragment = null;

    private ShoutFragment shoutFragment = null;

    public int currentlySelectedShout = -1;

    private boolean notificationRedirectionHandled = false;

    private int preventBackButtonPressedOnMapPositionChanged = 0;

    private ImageView settingsButton = null;

    private Button createShoutImageView = null;

    private boolean newMap = false;

    private boolean shoutJustCreated = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav);

        this.aq = new AQuery(this);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();

        feedFragment = (FeedFragment) getFragmentManager().findFragmentById(R.id.feed_fragment);
        shoutFragment = (ShoutFragment) getFragmentManager().findFragmentById(R.id.shout_fragment);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.hide(shoutFragment);
        ft.commit();

        if (savedInstanceState != null) {
            savedInstanceStateCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }

        LocationUtils.checkLocationServicesEnabled(this, locationManager);

        createShoutImageView = (Button) findViewById(R.id.create_shout_button);

        newMap = setUpMapIfNeeded();

        if (Constants.ADMIN) {
            final ImageView shatteShoutSwitch = (ImageView) findViewById(R.id.shatte_shout_switch);
            shatteShoutSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Constants.PRODUCTION = !Constants.PRODUCTION;
                    setShatteShoutSwitchImage(shatteShoutSwitch);
                }
            });

            setShatteShoutSwitchImage(shatteShoutSwitch);
            shatteShoutSwitch.setVisibility(View.VISIBLE);
        }
    }

    private void setShatteShoutSwitchImage(ImageView shatteShoutSwitch) {
        if (Constants.PRODUCTION) {
            shatteShoutSwitch.setImageResource(R.drawable.button_settings_selector);
        } else {
            shatteShoutSwitch.setImageResource(R.drawable.button_my_location_selector);
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    @Override
    protected void onResume() {
        super.onResume();

        myLocation = LocationUtils.getLastLocationWithLocationManager(this, locationManager);

        //Handles case when user clicked a shout notification
        if (!notificationRedirectionHandled && savedInstanceStateCameraPosition == null && getIntent().hasExtra("notificationShout")) {
            //To avoid going through here after before OnActivityResult
            notificationRedirectionHandled = true;

            JSONObject rawShout = null;
            try {
                rawShout = new JSONObject(getIntent().getStringExtra("notificationShout"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (rawShout != null) {
                Shout shout = Shout.rawShoutToInstance(rawShout);
                if (!displayedShoutModels.containsKey(shout.id)) {
                    Marker marker = displayShoutOnMap(shout);
                    displayedShoutMarkers.put(shout.id, marker);
                    displayedShoutModels.put(shout.id, shout);
                }
                onNotificationShoutSelected(shout, displayedShoutMarkers.get(shout.id));
                return;
            }
        }

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        //But activity gets destroyed when user shout with photo (memory issue), so don't initialize in that case!
        if (newMap && !shoutJustCreated) {
            if (savedInstanceStateCameraPosition != null) {
                initializeCameraWithCameraPosition(savedInstanceStateCameraPosition);
                savedInstanceStateCameraPosition = null;
            } else if (myLocation != null) {
                initializeCameraWithLocation(myLocation);
            }
            newMap = false;
        }
        shoutJustCreated = false;
    }

    @Override
    protected void onPause () {
        super.onPause();

        ApiUtils.updateUserInfoWithLocation(this, aq, myLocation);
    }

    private void pullShouts() {
        findViewById(R.id.no_connection_feed).setVisibility(View.GONE);
        findViewById(R.id.feed_wrapper).setVisibility(View.VISIBLE);

        MapRequestHandler mapReqHandler = new MapRequestHandler();

        feedFragment.showFeedProgressBar();

        //Set listener to catch API response from the MapRequestHandler
        mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
            @Override
            public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                if (status.getError() == null) {
                    JSONArray rawResult;
                    try {

                        if (object != null) {
                            rawResult = object.getJSONArray("result");
                        } else {
                            showNoConnectionInFeedMessage();
                            return;
                        }

                        ArrayList<Shout> shouts = Shout.rawShoutsToInstances(rawResult);

                        displayShoutsOnMap(shouts);
                        feedFragment.hideFeedProgressBar();
                        feedFragment.setAdapter(NavActivity.this, shouts);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });

        //Add a request to populate the map with shouts
        mapReqHandler.addMapRequest(aq, mMap.getProjection().getVisibleRegion().latLngBounds);
    }

    private void showNoConnectionInFeedMessage() {
        feedFragment.hideFeedProgressBar();
        findViewById(R.id.feed_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.no_connection_feed).setVisibility(View.VISIBLE);
        findViewById(R.id.feed_wrapper).setVisibility(View.GONE);
    }

    private void displayShoutsOnMap(List<Shout> shouts) {
        displayedShoutModels.clear();
        HashMap<Integer, Marker> newDisplayedShoutMarkers = new HashMap<Integer, Marker>();

        for (Shout shout: shouts) {
            displayedShoutModels.put(shout.id, shout);

            //Check that the shout is not already marked on the map
            if (!displayedShoutMarkers.containsKey(shout.id)) {
                Marker shoutMarker = displayShoutOnMap(shout);
                newDisplayedShoutMarkers.put(shout.id, shoutMarker);
            //If he is, re-use the marker
            } else {
                newDisplayedShoutMarkers.put(shout.id, displayedShoutMarkers.get(shout.id));
                displayedShoutMarkers.remove(shout.id);
            }
        }

        for (HashMap.Entry<Integer, Marker> entry: displayedShoutMarkers.entrySet()) {
            entry.getValue().remove();
        }

        displayedShoutMarkers = newDisplayedShoutMarkers;
    }

    private Marker displayShoutOnMap(Shout shout) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(new LatLng(shout.lat, shout.lng));
        if (shout.id != currentlySelectedShout) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, false)));
        } else {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, true)));
        }
        markerOptions.title(Integer.toString(shout.id));

        return mMap.addMarker(markerOptions);
    }

    public void createShout(View view) {
        createShoutImageView.setEnabled(false);

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
            createShoutImageView.setEnabled(true);
        } else if (myLocation == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_location), Toast.LENGTH_SHORT);
            toast.show();
            createShoutImageView.setEnabled(true);
        } else {
            Intent createShout = new Intent(this, CreateShoutActivity.class);
            createShout.putExtra("myLocation", myLocation);
            startActivityForResult(createShout, Constants.CREATE_SHOUT_REQUEST);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("cameraPosition", mMap.getCameraPosition());
        super.onSaveInstanceState(outState);
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CREATE_SHOUT_REQUEST) {
            createShoutImageView.setEnabled(true);

            if (resultCode == RESULT_OK) {
                Shout shout = data.getParcelableExtra("newShout");

                displayedShoutModels.put(shout.id, shout);
                Marker shoutMarker = displayShoutOnMap(shout);
                displayedShoutMarkers.put(shout.id, shoutMarker);

                onShoutCreationShoutSelected(shout, shoutMarker);

                shoutJustCreated = true;
            }
        } else if (requestCode == Constants.SETTINGS_REQUEST) {
            settingsButton.setEnabled(true);

            //Go back to feed and refresh if user is coming back from settings (in case user changed distance unit)
            if (shoutFragment.isVisible()) {
                deselectShoutIfAnySelected();
                onBackPressed();
            }

            pullShouts();
        }


    }

    @Override
    public void onFeedShoutSelected(Shout shout) {
        Marker marker = displayedShoutMarkers.get(shout.id);

        shoutSelected(shout);

        updateMapOnShoutSelectedFromMapOrFeed(shout, marker);
    }

    private void onMapShoutSelected(Marker marker) {
        Shout shout = displayedShoutModels.get(Integer.parseInt(marker.getTitle()));

        shoutSelected(shout);

        updateMapOnShoutSelectedFromMapOrFeed(shout, marker);
    }

    private void onNotificationShoutSelected(Shout shout, Marker marker) {
        shoutSelected(shout);

        updateMapOnShoutSelectedFromNotification(shout, marker);
    }

    private void onShoutCreationShoutSelected(Shout shout, Marker marker) {
        shoutSelected(shout);

        updateMapOnShoutSelectedFromShoutCreation(shout, marker);
    }

    private void shoutSelected(Shout shout) {
        this.deselectShoutIfAnySelected();
        this.currentlySelectedShout = shout.id;

        showShoutFragment(shout);
    }

    private void showShoutFragment(Shout shout) {
        shoutFragment.displayShoutInFragment(shout, myLocation);

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        //slide_out_left and slide_in_left are not used
//        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);

        ft.show(shoutFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void deselectShoutIfAnySelected() {
        if (currentlySelectedShout != -1 && displayedShoutMarkers.containsKey(currentlySelectedShout)) {
            Shout shout = displayedShoutModels.get(currentlySelectedShout);
            displayedShoutMarkers.get(currentlySelectedShout).setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, false)));
            currentlySelectedShout = -1;
        }
    }

    /**
     *  MAP RELATED METHODS
     */

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (mMap == null) {
                return false;
            }

            //Set map settings
            UiSettings settings = mMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setMyLocationButtonEnabled(false);
            settings.setRotateGesturesEnabled(false);
            settings.setTiltGesturesEnabled(false);
            mMap.setMyLocationEnabled(true);

            //Set location listener
            mMap.setOnMyLocationChangeListener(this);

            //Pull shouts on the map
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (shoutFragment.isVisible() && preventBackButtonPressedOnMapPositionChanged == 0) {
                        deselectShoutIfAnySelected();
                        onBackPressed();
                    }

                    preventBackButtonPressedOnMapPositionChanged = 0;

                    pullShouts();
                }
            });

            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    onMapShoutSelected(marker);
                    return true;
                }
            });

            findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myLocation != null) {
                        CameraUpdate update;
                        if (mMap.getCameraPosition().zoom < Constants.CLICK_ON_MY_LOCATION_BUTTON) {
                            update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.CLICK_ON_MY_LOCATION_BUTTON);
                        } else {
                            update = CameraUpdateFactory.newLatLng(LocationUtils.toLatLng(myLocation));
                        }
                        mMap.animateCamera(update);
                    } else {
                        Toast toast = Toast.makeText(NavActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            });

            findViewById(R.id.zoom_plus_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.animateCamera(CameraUpdateFactory.zoomIn());
                }
            });

            findViewById(R.id.zoom_minus_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.animateCamera(CameraUpdateFactory.zoomOut());
                }
            });

            settingsButton = (ImageView) findViewById(R.id.settings_button);

            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
                        Toast toast = Toast.makeText(NavActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                        toast.show();
                        return;
                    }

                    settingsButton.setEnabled(false);
                    Intent settings = new Intent(NavActivity.this, SettingsActivity.class);
                    startActivityForResult(settings, Constants.SETTINGS_REQUEST);
                }
            });

            return true;
        }

        return false;
    }

    private void initializeCameraWithLocation(Location location) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(location), Constants.INITIAL_ZOOM);
        mMap.moveCamera(update);
    }

    private void initializeCameraWithCameraPosition(CameraPosition cameraPosition) {
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(update);
    }

    private void animateCameraToShout(Shout shout, Integer zoomLevel) {
        CameraUpdate update;

        if (zoomLevel != null) {
            update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), zoomLevel);
        } else {
            update = CameraUpdateFactory.newLatLng(new LatLng(shout.lat, shout.lng));
        }

        mMap.animateCamera(update, new GoogleMap.CancelableCallback() {
            @Override
            public void onCancel() {
                preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged - 1;
            }

            @Override
            public void onFinish() {

            }
        });
    }

    @Override
    public void zoomOnShout(Shout shout) {
        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;
        animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_SHOUT);
    }

    private void updateMapOnShoutSelectedFromMapOrFeed(final Shout shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, true)));
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;

        if (mMap.getCameraPosition().zoom <= Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED - 1) {
            animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED);
        } else {
            animateCameraToShout(shout, null);
        }
    }

    private void updateMapOnShoutSelectedFromNotification(Shout shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, true)));
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), Constants.REDIRECTION_FROM_NOTIFICATION);
        mMap.animateCamera(update, new GoogleMap.CancelableCallback() {
            @Override
            public void onCancel() {
                preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged - 1;
            }

            @Override
            public void onFinish() {

            }
        });
    }

    private void updateMapOnShoutSelectedFromShoutCreation(Shout shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout, true)));
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;

        animateCameraToShout(shout, Constants.REDIRECTION_FROM_CREATE_SHOUT);
    }

    @Override
    public void onBackPressed() {
        deselectShoutIfAnySelected();

        super.onBackPressed();
    }
}