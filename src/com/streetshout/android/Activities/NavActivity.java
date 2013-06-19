package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
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

public class NavActivity extends Activity implements GoogleMap.OnMyLocationChangeListener, ShoutFragment.OnShoutSelectedListener, FeedFragment.OnFeedShoutSelectedListener {

    private static int CREATE_SHOUT_CODE = 11101;

    private static int FEED_FRAGMENT_ID = R.id.feed_fragment;

    private static int MAP_FRAGMENT_ID = R.id.map;

    private static int SHOUT_FRAGMENT_ID = R.id.shout_fragment;

    private static int CREATE_ACTIVITY_ID = 33312;

    private ConnectivityManager connectivityManager = null;

    private LocationManager locationManager = null;

    private AQuery aq = null;

    private GoogleMap mMap = null;

    /** Set of shout ids to keep track of shouts already added to the map */
    private HashMap<Integer, ShoutModel> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    private Location myLocation = null;

    private CameraPosition savedCameraPosition = null;

    private FeedFragment feedFragment = null;

    private ShoutFragment shoutFragment = null;

    private int currentlySelectedShout = -1;

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
            savedCameraPosition = savedInstanceState.getParcelable("cameraPosition");
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

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            if (savedCameraPosition != null) {
                initializeCameraWithCameraPosition(savedCameraPosition);
                savedCameraPosition = null;
            } else if (myLocation != null) {
                initializeCameraWithLocation(myLocation);
            }
        }
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
            settings.setMyLocationButtonEnabled(true);
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
                    pullShouts(cameraPosition);
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

    private void pullShouts(CameraPosition cameraPosition) {
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
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
        markerOptions.anchor((float) 0.2, (float) 0.6);
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
            Intent createShout = new Intent(this, CreateShoutActivity.class);
            createShout.putExtra("myLocation", myLocation);
            startActivityForResult(createShout, CREATE_SHOUT_CODE);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CREATE_SHOUT_CODE) {

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
        animateCameraToShout(shout, true);
    }

    private void animateCameraToShout(ShoutModel shout, boolean zoomToShout) {
        CameraUpdate update = null;

        if (zoomToShout) {
            update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), Constants.CLICK_ON_SHOUT_ZOOM);
        } else {
            update = CameraUpdateFactory.newLatLng(new LatLng(shout.lat, shout.lng));
        }

        mMap.animateCamera(update);
    }

    private void showFragment(int fragmentId) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (fragmentId == FEED_FRAGMENT_ID) {
            ft.hide(shoutFragment);
            ft.show(feedFragment);
        } else if (fragmentId == SHOUT_FRAGMENT_ID) {
            ft.hide(feedFragment);
            ft.show(shoutFragment);
        }

        ft.addToBackStack(null);
        ft.commit();
    }

    private void selectShout(ShoutModel shout, Marker marker, int id) {
        if (currentlySelectedShout != -1) {
            deselectShout(currentlySelectedShout);
        }

        currentlySelectedShout = shout.id;

        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));

        shoutFragment.displayShoutInFragment(shout);
        showFragment(SHOUT_FRAGMENT_ID);

        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        //At the end to avoid creating thread conflicts on the shout hashmaps
        if (id == MAP_FRAGMENT_ID) {
            animateCameraToShout(shout, false);
        } else if (id == CREATE_ACTIVITY_ID) {
            animateCameraToShout(shout, true);
        }
    }

    private void deselectShout(int shoutId) {
        displayedShoutMarkers.get(shoutId).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.shout_marker));
    }

    public void showFeedFragment(View view) {
        showFragment(FEED_FRAGMENT_ID);
        if (currentlySelectedShout != -1) {
            deselectShout(currentlySelectedShout);
            currentlySelectedShout = -1;
        }
    }
}