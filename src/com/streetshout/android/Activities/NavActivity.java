package com.streetshout.android.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.adapters.MapWindowAdapter;
import com.streetshout.android.fragments.FeedFragment;
import com.streetshout.android.fragments.ShoutFragment;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.utils.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

public class NavActivity extends Activity implements GoogleMap.OnMyLocationChangeListener, ShoutFragment.OnZoomOnShoutListener, FeedFragment.OnFeedShoutSelectedListener {

    private ConnectivityManager connectivityManager = null;

    private LocationManager locationManager = null;

    private AQuery aq = null;

    private GoogleMap mMap = null;

    /** Set of shout ids to keep track of shouts already added to the map */
    private HashMap<Integer, ShoutModel> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    private Location myLocation = null;

    private CameraPosition savedInstanceStateCameraPosition = null;

    private FeedFragment feedFragment = null;

    private ShoutFragment shoutFragment = null;

    public int currentlySelectedShout = -1;

    private boolean notificationRedirectionHandled = false;

    private int preventBackButtonPressedOnMapPositionChanged = 0;

    private ImageView settingsButton = null;

    private ImageView createShoutImageView = null;

    private boolean newMap = false;

    private boolean shoutJustCreated = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav);

        this.aq = new AQuery(this);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        displayedShoutModels = new HashMap<Integer, ShoutModel>();
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

        createShoutImageView = (ImageView) findViewById(R.id.create_shout_item_menu);

        newMap = setUpMapIfNeeded();
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForCrashes();
        checkForUpdates();

        myLocation = LocationUtils.getLastLocationWithLocationManager(this, locationManager);
        ApiUtils.sendDeviceInfo(this, aq, myLocation);

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
                ShoutModel shout = ShoutModel.rawShoutToInstance(rawShout);
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

        ApiUtils.sendDeviceInfo(this, aq, myLocation);
    }

    private void pullShouts() {
        MapRequestHandler mapReqHandler = new MapRequestHandler();

        feedFragment.showFeedProgressBar();

        //Set listener to catch API response from the MapRequestHandler
        mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
            @Override
            public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                if (status.getError() == null) {
                    JSONArray rawResult;
                    try {
                        rawResult = object.getJSONArray("result");

                        ArrayList<ShoutModel> shouts = ShoutModel.rawShoutsToInstances(rawResult);

                        displayShoutsOnMap(shouts);
                        feedFragment.hideFeedProgressBar();
                        feedFragment.setAdapter(NavActivity.this, shouts);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //Add a request to populate the map with shouts
        mapReqHandler.addMapRequest(aq, mMap.getProjection().getVisibleRegion().latLngBounds);
    }

    private void displayShoutsOnMap(List<ShoutModel> shouts) {
        displayedShoutModels.clear();
        HashMap<Integer, Marker> newDisplayedShoutMarkers = new HashMap<Integer, Marker>();

        for (ShoutModel shout: shouts) {
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

    private Marker displayShoutOnMap(ShoutModel shout) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(new LatLng(shout.lat, shout.lng));
        if (shout.id != currentlySelectedShout) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
        } else {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
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
            Intent createShout = new Intent(this, NewShoutContentActivity.class);
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
                ShoutModel shout = data.getParcelableExtra("newShout");

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
    public void onFeedShoutSelected(ShoutModel shout) {
        Marker marker = displayedShoutMarkers.get(shout.id);

        shoutSelected(shout);

        updateMapOnShoutSelectedFromMapOrFeed(shout, marker);
    }

    private void onMapShoutSelected(Marker marker) {
        ShoutModel shout = displayedShoutModels.get(Integer.parseInt(marker.getTitle()));

        shoutSelected(shout);

        updateMapOnShoutSelectedFromMapOrFeed(shout, marker);
    }

    private void onNotificationShoutSelected(ShoutModel shout, Marker marker) {
        shoutSelected(shout);

        updateMapOnShoutSelectedFromNotification(shout, marker);
    }

    private void onShoutCreationShoutSelected(ShoutModel shout, Marker marker) {
        shoutSelected(shout);

        updateMapOnShoutSelectedFromShoutCreation(shout, marker);
    }

    private void shoutSelected(ShoutModel shout) {
        this.deselectShoutIfAnySelected();
        this.currentlySelectedShout = shout.id;

        showShoutFragment(shout);
    }

    private void showShoutFragment(ShoutModel shout) {
        shoutFragment.displayShoutInFragment(shout, myLocation);

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.hide(feedFragment);
        ft.show(shoutFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void deselectShoutIfAnySelected() {
        if (currentlySelectedShout != -1 && displayedShoutMarkers.containsKey(currentlySelectedShout)) {
            displayedShoutMarkers.get(currentlySelectedShout).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
            currentlySelectedShout = -1;
        }
    }

    private void checkForCrashes() {
        CrashManager.register(this, "d8088fe6145a4b3dbf56d2d2f2289de9");
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, "d8088fe6145a4b3dbf56d2d2f2289de9");
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
                        if (mMap.getCameraPosition().zoom < Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED) {
                            update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED);
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

            findViewById(R.id.dezoom_to_world_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(0));
                }
            });

            settingsButton = (ImageView) findViewById(R.id.settings_button);

            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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

    private void animateCameraToShout(ShoutModel shout, Integer zoomLevel) {
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
    public void zoomOnShout(ShoutModel shout) {
        animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_SHOUT);
    }

    private void updateMapOnShoutSelectedFromMapOrFeed(final ShoutModel shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;

        if (mMap.getCameraPosition().zoom <= Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED - 1) {
            animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED);
        } else {
            animateCameraToShout(shout, null);
        }
    }

    private void updateMapOnShoutSelectedFromNotification(ShoutModel shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
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

    private void updateMapOnShoutSelectedFromShoutCreation(ShoutModel shout, Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        preventBackButtonPressedOnMapPositionChanged = preventBackButtonPressedOnMapPositionChanged + 1;

        animateCameraToShout(shout, Constants.REDIRECTION_FROM_CREATE_SHOUT);
    }
}