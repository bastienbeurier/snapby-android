package com.snapby.android.activities;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.snapby.android.custom.CustomViewPager;
import com.snapby.android.fragments.CameraFragment;
import com.snapby.android.fragments.ExploreFragment;
import com.snapby.android.fragments.ProfileFragment;
import com.snapby.android.R;
import com.snapby.android.adapters.MainSlidePagerAdapter;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.LocationUtils;
import com.snapby.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TreeSet;

/**
 * Created by bastien on 4/11/14.
 */
public class MainActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private PagerAdapter mainPagerAdapter;

    public CustomViewPager mainViewPager;

    private LocationClient mLocationClient = null;

    public Location myLocation = null;

    private LocationRequest mLocationRequest = null;

    private LocationManager locationManager = null;

    private ExploreFragment exploreFragment = null;

    private CameraFragment cameraFragment = null;

    private ProfileFragment profileFragment = null;

    public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    public TreeSet<Integer> myLikes = null;

    private boolean shouldRestoreCamera = false;

    private boolean firstOpen = true;

    private boolean preventRedirectToCamera = false;

    private int pagePreviouslySelected = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        myLikes = new TreeSet<Integer>();

        mainViewPager = (CustomViewPager) findViewById(R.id.main_view_pager);

        exploreFragment = new ExploreFragment();
        cameraFragment = new CameraFragment();
        profileFragment = new ProfileFragment();

        mainPagerAdapter = new MainSlidePagerAdapter(this.getSupportFragmentManager(), exploreFragment, cameraFragment, profileFragment);
        mainViewPager.setAdapter(mainPagerAdapter);
        mainViewPager.setOffscreenPageLimit(2);

        mainViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1) {
                    if (pagePreviouslySelected == 0) {
                        reloadExploreSnapbys();
                    } else if (pagePreviouslySelected == 2) {
                        reloadProfileSnapbys();
                    }
                }

                pagePreviouslySelected = i;
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (statusCode == ConnectionResult.SUCCESS) {
            mLocationClient = new LocationClient(this, this, this);
        } else {
            LocationUtils.googlePlayServicesFailure(this);
        }

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        myLocation = LocationUtils.getLastLocationWithLocationManager(this, locationManager);

        mLocationRequest = LocationUtils.createLocationRequest(LocationRequest.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_IN_MILLISECONDS);

        getMyLikes();

        //TODO: notification redirection

        //TODO: onActivityResult for refine (1h)
        //TODO: settings fragment (1h)
        //TODO: Don't save image more than once (1h)
        //TODO: Show anonymous shouts in profile (1h)
        //TODO: notifications for snapby: comments, likes, snaps in area (Baptiste)
        //TODO: implement liked (3 heures)
        //TODO: paginate shouts (2h)
        //TODO: like-comment-share icons on display (2h)
        //TODO: update comment count on comment (2h)
        //TODO: shout age in explore (1h)

        //TODO: Display profile
        //TODO: handle no location

        //TODO: design
        //Markers


        //TODO: erase unused stuff
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!firstOpen && !preventRedirectToCamera) {
            preventRedirectToCamera = false;
            mainViewPager.setCurrentItem(1, false);
        }

        if (shouldRestoreCamera) {
            cameraFragment.setUpCamera();
            shouldRestoreCamera = false;
        }

        SessionUtils.synchronizeUserInfo(this, myLocation);
        LocationUtils.checkLocationServicesEnabled(this, locationManager);
    }

    /**
     *  Location-related methods
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        if (mLocationClient != null) {
            mLocationClient.connect();
        }
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (mLocationClient != null) {
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(this);
            }

            mLocationClient.disconnect();
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

        Location lastLocation = mLocationClient.getLastLocation();

        if (lastLocation != null) {
            myLocation = lastLocation;
        }

        // Display the connection status
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
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
        LocationUtils.googlePlayServicesFailure(this);
    }

    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        if (location != null && location.getLatitude() != 0 && location.getLongitude() != 0) {
            myLocation = location;

            setExploreMapPerimeterIfNeeded(location);
            myLocation = location;
        }
    }

    public void setExploreMapPerimeterIfNeeded(Location location) {
        //Change explore map perimeter if never set before or (if there is a significant perimeter change and the explore map is not shown)
        if (myLocation == null || (mainViewPager.getCurrentItem() != 0 && (Math.abs(location.getLatitude() - myLocation.getLatitude()) + Math.abs(location.getLongitude() - myLocation.getLongitude()) > 0.0005))) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), Constants.EXPLORE_ZOOM);
            exploreFragment.exploreMap.moveCamera(update);
        }
    }

    private void getMyLikes() {
        ApiUtils.getMyLikesAndFollowedUsers(this, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(MainActivity.this);
                    return;
                }

                if (status.getError() == null) {
                    JSONObject result = null;

                    try {
                        result = object.getJSONObject("result");

                        JSONArray rawLikes = result.getJSONArray("likes");

                        int likeCount = rawLikes.length();

                        for (int i = 0; i < likeCount; i++) {
                            myLikes.add(Integer.parseInt(((JSONObject) rawLikes.get(i)).getString("shout_id")));
                        }

                        reloadExploreShouts();
                        reloadProfileShouts();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void reloadExploreShouts() {
        exploreFragment.reloadAdapterIfAlreadyLoaded();
    }

    public void reloadProfileShouts() {
        profileFragment.reloadAdapterIfAlreadyLoaded();
    }

    @Override
    public void onBackPressed() {
        if (mainViewPager.getCurrentItem() == 0 || mainViewPager.getCurrentItem() == 2) {
            mainViewPager.setCurrentItem(1);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        cameraFragment.releaseCamera();
        shouldRestoreCamera = true;

        super.onPause();
    }

    public void updateLocalShoutCount() {
        LatLngBounds bounds = exploreFragment.getExploreMapBounds();

        cameraFragment.updateLocalShoutCount(bounds);
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        if (firstOpen) {
            mainViewPager.setCurrentItem(1, false);
            firstOpen = false;
        }

        super.onWindowFocusChanged(hasFocus);
    }

    public void reloadSnapbys() {
        reloadExploreSnapbys();
        reloadProfileSnapbys();
    }

    private void reloadExploreSnapbys() {
        if (exploreFragment.mapLoaded) {
            exploreFragment.loadContent();
        }
    }

    private void reloadProfileSnapbys() {
        if (profileFragment.mapLoaded) {
            profileFragment.getUserShouts();
            profileFragment.getUserInfo();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("BAB", "PREVENT REDIRECT TO CAMERA");
        preventRedirectToCamera = true;
    }
}