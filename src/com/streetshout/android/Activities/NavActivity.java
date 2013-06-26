package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.Adapters.MapWindowAdapter;
import com.streetshout.android.Fragments.AddressSearchFragment;
import com.streetshout.android.Fragments.FeedFragment;
import com.streetshout.android.Fragments.ShoutFragment;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.Utils.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavActivity extends Activity implements GoogleMap.OnMyLocationChangeListener, ShoutFragment.OnShoutSelectedListener, FeedFragment.OnFeedShoutSelectedListener, AddressSearchFragment.OnAddressValidateListener {

    private static int FEED_FRAGMENT_ID = R.id.feed_fragment;

    private static int MAP_FRAGMENT_ID = R.id.map;

    private static int SHOUT_FRAGMENT_ID = R.id.shout_fragment;

    private static int ADDRESS_SEARCH_FRAGMENT_ID = R.id.address_search_fragment;

    private static int CREATE_ACTIVITY_ID = 33312;

    private static int NOTIFICATION_REDIRECTION_ID = 33313;

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

    private AddressSearchFragment addressSearchFragment = null;

    public int currentlySelectedShout = -1;

    private boolean notificationRedirectionHandled = false;

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
        addressSearchFragment = (AddressSearchFragment) getFragmentManager().findFragmentById(R.id.address_search_fragment);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.hide(shoutFragment);
        ft.hide(addressSearchFragment);
        ft.commit();

        if (savedInstanceState != null) {
            savedInstanceStateCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }

        checkLocationServicesEnabled();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("cameraPosition", mMap.getCameraPosition());
        super.onSaveInstanceState(outState);
    }

    private void checkLocationServicesEnabled() {
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {}

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getText(R.string.no_location_dialog_title));
            dialog.setMessage(getText(R.string.no_location_dialog_message));
            dialog.setPositiveButton(getText(R.string.settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent settings = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    NavActivity.this.startActivity(settings);
                }
            });
            dialog.setNegativeButton(getText(R.string.skip), null);
            dialog.show();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    @Override
    protected void onResume() {
        super.onResume();


        boolean newMap = setUpMapIfNeeded();
        myLocation = getMyInitialLocation();
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
                selectShout(shout, displayedShoutMarkers.get(shout.id), NOTIFICATION_REDIRECTION_ID);
                return;
            }
        }

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            if (savedInstanceStateCameraPosition != null) {
                initializeCameraWithCameraPosition(savedInstanceStateCameraPosition);
                savedInstanceStateCameraPosition = null;
            } else if (myLocation != null) {
                initializeCameraWithLocation(myLocation);
            }
        }
    }

    @Override
    protected void onPause () {
        super.onPause();

        ApiUtils.sendDeviceInfo(this, aq, myLocation);
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (mMap == null) {
                return false;
            }

            //Set map settings
            UiSettings settings = mMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setCompassEnabled(true);
            settings.setMyLocationButtonEnabled(false);
            settings.setRotateGesturesEnabled(false);
            settings.setTiltGesturesEnabled(false);

            //Set user location
            mMap.setMyLocationEnabled(true);

            //Set location listener
            mMap.setOnMyLocationChangeListener(this);

            //Pull shouts on the map
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    pullShouts();
                }
            });

            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    ShoutModel shout = displayedShoutModels.get(Integer.parseInt(marker.getTitle()));

                    selectShout(shout, marker, MAP_FRAGMENT_ID);

                    return true;
                }
            });

            findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myLocation != null) {
                        CameraUpdate update = null;
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

            findViewById(R.id.address_search_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSearchAddressFragment();
                }
            });

            findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent settings = new Intent(NavActivity.this, SettingsActivity.class);
                    startActivityForResult(settings, Constants.SETTINGS_REQUEST);
                }
            });

            if (Constants.PRODUCTION) {
                findViewById(R.id.start_demo_button).setVisibility(View.GONE);
            } else {
                findViewById(R.id.start_demo_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ApiUtils.startDemo(aq);
                        v.setVisibility(View.GONE);
                    }
                });
            }

            return true;
        }

        return false;
    }

    private Location getMyInitialLocation() {
        if (locationManager == null) {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        }

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        return locationManager.getLastKnownLocation(provider);
    }

    private void initializeCameraWithLocation(Location location) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(location), Constants.INITIAL_ZOOM);
        mMap.moveCamera(update);
    }

    private void initializeCameraWithCameraPosition(CameraPosition cameraPosition) {
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(update);
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
                        addShoutsOnMap(shouts);
                        feedFragment.hideFeedProgressBar();
                        feedFragment.setAdapter(NavActivity.this, shouts, myLocation);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //Add a request to populate the map with shouts
        mapReqHandler.addMapRequest(aq, mMap.getProjection().getVisibleRegion().latLngBounds);
    }

    private void addShoutsOnMap(List<ShoutModel> shouts) {
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
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (myLocation == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_location), Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Intent createShout = new Intent(this, NewShoutContentActivity.class);
            createShout.putExtra("myLocation", myLocation);
            startActivityForResult(createShout, Constants.CREATE_SHOUT_REQUEST);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.CREATE_SHOUT_REQUEST) {

            if(resultCode == RESULT_OK){
                Toast toast = Toast.makeText(this, getString(R.string.create_shout_success), Toast.LENGTH_LONG);
                toast.show();

                ShoutModel shout = data.getParcelableExtra("newShout");

                displayedShoutModels.put(shout.id, shout);
                Marker shoutMarker = displayShoutOnMap(shout);
                displayedShoutMarkers.put(shout.id, shoutMarker);

                selectShout(shout, shoutMarker, CREATE_ACTIVITY_ID);
            }
        }
    }

    @Override
    public void onFeedShoutSelected(ShoutModel shout) {
        Marker shoutMarker = displayedShoutMarkers.get(shout.id);

        selectShout(shout, shoutMarker, FEED_FRAGMENT_ID);
    }

    //TODO: change name
    @Override
    public void onShoutSelected(ShoutModel shout) {
        animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_SHOUT);
    }

    @Override
    public void onAddressValidate(double lat, double lng) {
        addressSearchFragment.removeFocusFromSearchAddressView();
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), Constants.SEARCH_ADDRESS_IN_NAV);
        mMap.animateCamera(update, new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                showFragment(FEED_FRAGMENT_ID);
            }

            @Override
            public void onCancel() {
                showFragment(FEED_FRAGMENT_ID);
            }
        });
    }

    private void animateCameraToShout(ShoutModel shout, Integer zoomLevel) {
        CameraUpdate update = null;

        if (zoomLevel != null) {
            update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), zoomLevel);
        } else {
            update = CameraUpdateFactory.newLatLng(new LatLng(shout.lat, shout.lng));
        }

        mMap.animateCamera(update);
    }

    private void showShoutFragment(int shoutId, Marker shoutMarker) {
        this.deselectShoutIfAnySelected();
        this.currentlySelectedShout = shoutId;

        shoutMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));

        showFragment(SHOUT_FRAGMENT_ID);
    }

    private void showSearchAddressFragment() {
        this.deselectShoutIfAnySelected();
        addressSearchFragment.setFocusOnSearchAddressView();

        showFragment(ADDRESS_SEARCH_FRAGMENT_ID);
    }

    private void showFragment(int fragmentId) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        ft.hide(visibleFrament());

        if (fragmentId == FEED_FRAGMENT_ID) {
            ft.show(feedFragment);
        } if (fragmentId == SHOUT_FRAGMENT_ID) {
            ft.show(shoutFragment);
        } else if (fragmentId == ADDRESS_SEARCH_FRAGMENT_ID) {
            ft.show(addressSearchFragment);
        }

        ft.commit();
    }

    private Fragment visibleFrament() {
        if (shoutFragment.isVisible()) {
            return shoutFragment;
        } else if (addressSearchFragment.isVisible()) {
            return addressSearchFragment;
        //Warning: when activity launched by notification, no fragment is visible and this method is called, in this
        //case the feedFragment should be returned.
        } else {
            return feedFragment;
        }
    }

    private void selectShout(ShoutModel shout, Marker marker, int selectionSource) {
        shoutFragment.displayShoutInFragment(shout, myLocation);
        showShoutFragment(shout.id, marker);

        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        //How the camera reacts: at the end to avoid creating thread conflicts on the shout hashmaps
        if (selectionSource == MAP_FRAGMENT_ID || selectionSource == FEED_FRAGMENT_ID) {
            if (mMap.getCameraPosition().zoom <= Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED - 1) {
                animateCameraToShout(shout, Constants.CLICK_ON_SHOUT_IN_MAP_OR_FEED);
            } else {
                animateCameraToShout(shout, null);
            }
        } else if (selectionSource == CREATE_ACTIVITY_ID) {
            animateCameraToShout(shout, Constants.REDIRECTION_FROM_CREATE_SHOUT);
        } else if (selectionSource == NOTIFICATION_REDIRECTION_ID) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), Constants.REDIRECTION_FROM_NOTIFICATION);
            mMap.animateCamera(update);
        }
    }

    public void deselectShoutIfAnySelected() {
        if (currentlySelectedShout != -1 && displayedShoutMarkers.containsKey(currentlySelectedShout)) {
            displayedShoutMarkers.get(currentlySelectedShout).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
            currentlySelectedShout = -1;
        }
    }

    public void shareShoutFromShoutFragment(View v) {
        //TODO: remove
        Toast toast = Toast.makeText(this, "NOT YET IMPLEMENTED!!!", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onBackPressed(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (visibleFrament() == addressSearchFragment) {
            showFragment(FEED_FRAGMENT_ID);
            addressSearchFragment.removeFocusFromSearchAddressView();
            return;
        }

        if (visibleFrament() == shoutFragment) {
            showFragment(FEED_FRAGMENT_ID);
            deselectShoutIfAnySelected();
            return;
        }

        super.onBackPressed();
    }
}