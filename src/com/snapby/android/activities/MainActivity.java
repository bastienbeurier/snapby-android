package com.snapby.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.widget.Toast;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.snapby.android.custom.CustomViewPager;
import com.snapby.android.fragments.CameraFragment;
import com.snapby.android.fragments.ExploreFragment;
import com.snapby.android.fragments.ProfileFragment;
import com.snapby.android.R;
import com.snapby.android.adapters.MainSlidePagerAdapter;
import com.snapby.android.fragments.SettingsFragment;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.ImageUtils;
import com.snapby.android.utils.LocationUtils;
import com.snapby.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
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

    private SettingsFragment settingsFragment = null;

    public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    public TreeSet<Integer> myLikes = null;

    private boolean shouldRestoreCamera = false;

    private boolean firstOpen = true;

    private boolean preventRedirectToCamera = false;

    private int commingFromPage = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        myLikes = new TreeSet<Integer>();

        mainViewPager = (CustomViewPager) findViewById(R.id.main_view_pager);

        exploreFragment = new ExploreFragment();
        cameraFragment = new CameraFragment();
        profileFragment = new ProfileFragment();
        settingsFragment  = new SettingsFragment();

        mainPagerAdapter = new MainSlidePagerAdapter(this.getSupportFragmentManager(), exploreFragment, cameraFragment, profileFragment, settingsFragment);
        mainViewPager.setAdapter(mainPagerAdapter);
        mainViewPager.setOffscreenPageLimit(3);

        mainViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1 && commingFromPage == 0) {
                    repullExploreSnapbies();
                } else if (i == 1 && commingFromPage == 2) {
                    repullProfileSnapbies();
                }

                commingFromPage = i;
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
     * Location-related methods
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

            if (exploreFragment.waitingForLocation) {
                repullExploreSnapbies();
            }
        }
    }

    private void getMyLikes() {
        ApiUtils.getMyLikes(this, new AjaxCallback<JSONObject>() {
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
                            myLikes.add(Integer.parseInt(((JSONObject) rawLikes.get(i)).getString("snapby_id")));
                        }

                        reloadExploreSnapbies();
                        reloadProfileSnapbies();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void reloadExploreSnapbies() {
        exploreFragment.reloadAdapterIfAlreadyLoaded();
    }

    public void reloadProfileSnapbies() {
        profileFragment.reloadAdapterIfAlreadyLoaded();
    }

    @Override
    public void onBackPressed() {
        if (mainViewPager.getCurrentItem() == 0 || mainViewPager.getCurrentItem() == 2) {
            mainViewPager.setCurrentItem(1);
        } else if (mainViewPager.getCurrentItem() == 3) {
            mainViewPager.setCurrentItem(2);
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

    public void updateLocalSnapbyCount() {
        LatLngBounds bounds = exploreFragment.getExploreMapBounds();

        cameraFragment.updateLocalSnapbyCount(bounds);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (firstOpen) {
            mainViewPager.setCurrentItem(1, false);
            firstOpen = false;
        }

        super.onWindowFocusChanged(hasFocus);
    }

    public void repullSnapbies() {
        repullExploreSnapbies();
        repullProfileSnapbies();
    }

    private void repullExploreSnapbies() {
        if (exploreFragment.mapLoaded && myLocation != null) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.EXPLORE_ZOOM);
            exploreFragment.exploreMap.moveCamera(update);

            exploreFragment.loadContent();
        }
    }

    private void repullProfileSnapbies() {
        if (profileFragment.mapLoaded) {
            profileFragment.getUserSnapbies();
            profileFragment.getUserInfo();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.DISPLAY_SHOUT_REQUEST && data != null && data.hasExtra("delete")) {
            repullSnapbies();
        } else if (requestCode == Constants.CHOOSE_PROFILE_PICTURE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bitmap formattedPicture = null;

                //From camera?
                if (data.hasExtra("data")) {
                    formattedPicture = ImageUtils.makeThumb((Bitmap) data.getExtras().get("data"));
                    //From library
                } else {
                    //New Kitkat way of doing things
                    if (Build.VERSION.SDK_INT < 19) {
                        String libraryPhotoPath = ImageUtils.getPathFromUri(this, data.getData());
                        formattedPicture = ImageUtils.decodeAndMakeThumb(libraryPhotoPath);
                    } else {
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = this.getContentResolver().openFileDescriptor(data.getData(), "r");
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            formattedPicture = ImageUtils.makeThumb(BitmapFactory.decodeFileDescriptor(fileDescriptor));
                            parcelFileDescriptor.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //Convert bitmap to byte array
                Bitmap bitmap = formattedPicture;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                byte[] bmData = stream.toByteArray();
                String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

                ApiUtils.updateUserInfoWithLocation(this, GeneralUtils.getAquery(this), null, encodedImage, null, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        if (status.getError() == null && object != null && status.getCode() != 222) {
                            Toast toast = Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.update_profile_picture_success), Toast.LENGTH_LONG);
                            toast.show();
                            profileFragment.updateUI(true);
                            settingsFragment.updateUI();
                        } else {
                            Toast toast = Toast.makeText(MainActivity.this, getString(R.string.update_profile_picture_failure), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });
            }
        } else if (requestCode == Constants.REFINE_LOCATION_ACTIVITY_REQUEST) {
            if (data != null && data.hasExtra("accurateSnapbyLocation")) {
                cameraFragment.refinedSnapbyLocation = data.getParcelableExtra("accurateSnapbyLocation");
            }
        }

        preventRedirectToCamera = true;
    }

    public void letUserChooseProfilePic() {
        Intent chooserIntent = ImageUtils.getPhotoChooserIntent(this);

        startActivityForResult(chooserIntent, Constants.CHOOSE_PROFILE_PICTURE_REQUEST);
    }

    public void refineSnapbyLocation(Location location) {
        Intent refineIntent = new Intent(this, RefineLocationActivity.class);
        refineIntent.putExtra("snapbyRefinedLocation", location);
        startActivityForResult(refineIntent, Constants.REFINE_LOCATION_ACTIVITY_REQUEST);
    }
}