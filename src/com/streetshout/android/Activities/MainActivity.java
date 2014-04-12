package com.streetshout.android.activities;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.streetshout.android.Fragments.CameraFragment;
import com.streetshout.android.Fragments.ExploreFragment;
import com.streetshout.android.Fragments.ProfileFragment;
import com.streetshout.android.R;
import com.streetshout.android.adapters.MainSlidePagerAdapter;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.SessionUtils;

/**
 * Created by bastien on 4/11/14.
 */
public class MainActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private PagerAdapter mainPagerAdapter;

    public ViewPager mainViewPager;

    private LocationClient mLocationClient = null;

    public Location myLocation = null;

    private LocationRequest mLocationRequest = null;

    private LocationManager locationManager = null;

    private ExploreFragment exploreFragment = null;

    private CameraFragment cameraFragment = null;

    private ProfileFragment profileFragment = null;

    public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mainViewPager = (ViewPager) findViewById(R.id.main_view_pager);

        exploreFragment = new ExploreFragment();
        cameraFragment = new CameraFragment();
        profileFragment = new ProfileFragment();

        mainPagerAdapter = new MainSlidePagerAdapter(this.getSupportFragmentManager(), exploreFragment, cameraFragment, profileFragment);
        mainViewPager.setAdapter(mainPagerAdapter);
        mainViewPager.setCurrentItem(1);
        mainViewPager.setOffscreenPageLimit(2);

        mainViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 0) {
                    exploreFragment.loadContent(myLocation);
                } else if (i ==  2) {
                    profileFragment.loadContent();
                }
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

        //TODO: notification redirection
        //TODO: release camera
        //TODO: onActivityResult for refine
        //TODO: notif implementation do not redirect to Camera
        //TODO: implement back button on fragments
        //TODO: redirect create shout!
        //TODO: check that createView is not called everytime
        //TODO: no location --> update
        //TODO: increase swipe in explore
        //TODO: paginate shouts on explore and profile
        //TODO: settings fragment
        //TODO: like
        //TODO: move map on select shout for profile
        //TODO: faster scrolling
        //TODO Don't save image more than once
        //TODO: Cleaner loading dialog
        //TODO: Make selected snap bigger
        //TODO: Trending mark
    }

    @Override
    protected void onResume() {
        super.onResume();

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
        if (location != null) {
            myLocation = location;
        }
    }


}