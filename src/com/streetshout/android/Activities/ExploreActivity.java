package com.streetshout.android.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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
import com.streetshout.android.adapters.ShoutSlidePagerAdapter;
import com.streetshout.android.custom.ZoomOutPageTransformer;
import com.streetshout.android.fragments.ShoutSlidePageFragment;
import com.streetshout.android.models.Shout;
import com.streetshout.android.R;
import com.streetshout.android.utils.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExploreActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener{

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private AQuery aq = null;

    private GoogleMap mMap = null;

    /** Set of shout ids to keep track of shouts already added to the map */
    private HashMap<Integer, Shout> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    public Location myLocation = null;

    private CameraPosition savedInstanceStateCameraPosition = null;

    private boolean notificationRedirectionHandled = false;

    private boolean newMap = false;

    private LocationClient locationClient = null;

    private LocationRequest locationRequest = null;

    public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    private Shout redirectToShout = null;

    private ViewPager shoutViewPager;

    private PagerAdapter shoutPagerAdapter;

    private FrameLayout shoutProgressBar = null;

    private TextView noShoutInFeed = null;

    private TextView noConnectionInFeed = null;

    private Shout shoutSelectedOnMap = null;

    private ArrayList<Shout> shouts = null;

    private MapRequestHandler mapReqHandler = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);

        if (getIntent().hasExtra("myLocation")) {
            myLocation = getIntent().getParcelableExtra("myLocation");
        }

        if (getIntent().hasExtra("newShout")) {
            redirectToShout = getIntent().getParcelableExtra("newShout");
        }

        this.aq = new AQuery(this);

        locationRequest = LocationUtils.createLocationRequest(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_IN_MILLISECONDS);

        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (statusCode == ConnectionResult.SUCCESS) {
            locationClient = new LocationClient(this, this, this);
        } else {
            LocationUtils.googlePlayServicesFailure(this);
        }

        if (savedInstanceState != null) {
            savedInstanceStateCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }

        mapReqHandler = new MapRequestHandler();

        shoutProgressBar = (FrameLayout) findViewById(R.id.explore_shout_progress_bar);
        shoutViewPager = (ViewPager) findViewById(R.id.explore_view_pager);
        shoutViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateSelectedShoutMarker(shouts.get(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        noShoutInFeed = (TextView) findViewById(R.id.explore_shout_no_shout);
        noConnectionInFeed = (TextView) findViewById(R.id.explore_shout_no_connection);

        newMap = setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        //TODO: change implementation
        if (redirectToShout != null) {
            redirectToShout();
        }
        redirectToShout = null;

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        //But activity gets destroyed when user shout with photo (memory issue), so don't initialize in that case!
        if (newMap && redirectToShout == null) {
            if (savedInstanceStateCameraPosition != null) {
                initializeCameraWithCameraPosition(savedInstanceStateCameraPosition);
                savedInstanceStateCameraPosition = null;
            } else if (myLocation != null) {
                initializeCameraWithLocation(myLocation);
            }
            newMap = false;
            pullShouts();
        }
    }

    @Override
    protected void onPause () {
        super.onPause();

        ApiUtils.updateUserInfoWithLocation(this, aq, myLocation);
    }

    private void pullShouts() {
        mMap.clear();
        shoutSelectedOnMap = null;
        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();
        shouts = null;
        shoutViewPager.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        shoutProgressBar.setVisibility(View.VISIBLE);

        //Set listener to catch API response from the MapRequestHandler
        mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
            @Override
            public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                if (status.getError() == null) {
                    JSONArray rawShouts;
                    try {

                        if (object != null) {
                            JSONObject rawResult = object.getJSONObject("result");
                            rawShouts = rawResult.getJSONArray("shouts");
                        } else {
                            showNoConnectionInFeedMessage();
                            return;
                        }

                        shouts = Shout.rawShoutsToInstances(rawShouts);
                        shouts = checkForRemovedShouts(shouts);

                        displayShoutsOnMap(shouts);

                        noConnectionInFeed.setVisibility(View.GONE);
                        shoutProgressBar.setVisibility(View.GONE);

                        if (shouts.size() > 0) {
                            // Instantiate a ViewPager and a PagerAdapter.
                            shoutPagerAdapter = new ShoutSlidePagerAdapter(ExploreActivity.this.getSupportFragmentManager(), shouts);
                            shoutViewPager.setAdapter(shoutPagerAdapter);
                            updateSelectedShoutMarker(shouts.get(0));
                            shoutViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
                            shoutViewPager.setVisibility(View.VISIBLE);
                            noShoutInFeed.setVisibility(View.GONE);
                        } else {
                            shoutViewPager.setAdapter(null);
                            shoutViewPager.setVisibility(View.GONE);
                            noShoutInFeed.setVisibility(View.VISIBLE);
                        }

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

    private ArrayList<Shout> checkForRemovedShouts(ArrayList<Shout> shouts) {
        ArrayList<Shout> newShouts = new ArrayList<Shout>();

        for (Shout shout:shouts) {
            if (!shout.removed) {
                newShouts.add(shout);
            }
        }

        return newShouts;
    }

    private void showNoConnectionInFeedMessage() {
        shoutViewPager.setVisibility(View.GONE);
        shoutProgressBar.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
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

        markerOptions.icon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(this, shout, false)));

        markerOptions.title(Integer.toString(shout.id));

        return mMap.addMarker(markerOptions);
    }

    public void createShout(View view) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);

        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("cameraPosition", mMap.getCameraPosition());
        super.onSaveInstanceState(outState);
    }

   private void updateSelectedShoutMarker(Shout shout) {
       if (shoutSelectedOnMap != null) {
           Marker oldSelectedMarker = displayedShoutMarkers.get(shoutSelectedOnMap.id);
           if (oldSelectedMarker != null) {
               oldSelectedMarker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(this, shoutSelectedOnMap, false)));
           }
       }

       shoutSelectedOnMap = shout;

       Marker marker = displayedShoutMarkers.get(shout.id);

       marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(this, shout, true)));
   }

    private void onMapShoutSelected(Marker marker) {
        Shout selectedShout = displayedShoutModels.get(Integer.parseInt(marker.getTitle()));

        int count = shouts.size();

        for (int i = 0; i < count; i++) {
            if (shouts.get(i).id == selectedShout.id) {
                //Creates a fragment that calls onFeedShoutSelected
                shoutViewPager.setCurrentItem(i);
                break;
            }
        }
    }

    private void onNotificationShoutSelected(Shout shout, Marker marker) {

        //TODO only display the notified shout?

        updateMapOnShoutSelectedFromNotificationOrCreation(shout, marker);
    }

    private void onShoutCreationShoutSelected(Shout shout, Marker marker) {

        //TODO only display the created shout?

        updateMapOnShoutSelectedFromNotificationOrCreation(shout, marker);
    }

    /**
     *  Map-related methods
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
                        Toast toast = Toast.makeText(ExploreActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
                        toast.show();
                    }
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

    private void updateMapOnShoutSelectedFromNotificationOrCreation(Shout shout, Marker marker) {
        //Hack to make to the marker come to front when click (warning! to work, a marker title must be set)
        marker.showInfoWindow();

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), Constants.REDIRECTION_FROM_CREATE_SHOUT);
        mMap.moveCamera(update);
    }

    private void redirectToShout() {
        displayedShoutModels.put(redirectToShout.id, redirectToShout);
        Marker shoutMarker = displayShoutOnMap(redirectToShout);
        displayedShoutMarkers.put(redirectToShout.id, shoutMarker);

        onShoutCreationShoutSelected(redirectToShout, shoutMarker);
    }

    /**
     *  Map-related methods
     */

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        if (locationClient != null) {
            locationClient.connect();
        }
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (locationClient != null) {
            if (locationClient.isConnected()) {
                locationClient.removeLocationUpdates(this);
            }

            locationClient.disconnect();
        }

        super.onStop();
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        Location lastLocation = locationClient.getLastLocation();

        if (lastLocation != null) {
            myLocation = lastLocation;
        }

        // Display the connection status
        locationClient.requestLocationUpdates(locationRequest, this);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            LocationUtils.googlePlayServicesFailure(this);
        }
    }

    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        if (location != null) {
            myLocation = location;
        }
    }
}