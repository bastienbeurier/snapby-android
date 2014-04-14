package com.streetshout.android.activities;

import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.adapters.MapWindowAdapter;
import com.streetshout.android.adapters.ShoutsPagerAdapter;
import com.streetshout.android.models.Shout;
import com.streetshout.android.R;
import com.streetshout.android.utils.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class ExploreActivity extends FragmentActivity {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap = null;

    private HashMap<Integer, Shout> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    public Location myLocation = null;

    private CameraPosition savedInstanceStateCameraPosition = null;

    private boolean notificationRedirectionHandled = false;

    private boolean newMap = false;

    private Shout shoutToRedirectToFromCreateOrNotif = null;

    private Shout shoutToRedirectToFromZoom = null;

    private ViewPager shoutViewPager;

    private PagerAdapter shoutPagerAdapter;

    private FrameLayout shoutProgressBar = null;

    private TextView noShoutInFeed = null;

    private TextView noConnectionInFeed = null;

    private Shout shoutSelectedOnMap = null;

    private ArrayList<Shout> shouts = null;

    private MapRequestHandler mapReqHandler = null;

    private Point[] shoutAreaPoints = null;

    public TreeSet<Integer> myLikes = null;

    public TreeSet<Integer> followedByMe = null;

    private int screenWidth = 0;

    private int screenHeight = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore);

//        if (shoutAreaPoints == null) {
//            setPullShoutArea();
//        }
//
//        myLikes = new TreeSet<Integer>();
//        followedByMe = new TreeSet<Integer>();
//
//        myLocation = getIntent().getParcelableExtra("myLocation");
//
//        if (getIntent().hasExtra("newShout")) {
//            shoutToRedirectToFromCreateOrNotif = getIntent().getParcelableExtra("newShout");
//        }
//
//        if (savedInstanceState != null) {
//        if (savedInstanceState != null) {
//            savedInstanceStateCameraPosition = savedInstanceState.getParcelable("cameraPosition");
//        }
//
//        mapReqHandler = new MapRequestHandler();
//
//        shoutProgressBar = (FrameLayout) findViewById(R.id.explore_shout_progress_bar);
//        shoutViewPager = (ViewPager) findViewById(R.id.explore_view_pager);
//        shoutViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int i, float v, int i2) {
//
//            }
//
//            @Override
//            public void onPageSelected(int i) {
//                updateSelectedShoutMarker(shouts.get(i));
//                updateShoutCountViews(i);
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int i) {
//
//            }
//        });
//
//        noShoutInFeed = (TextView) findViewById(R.id.explore_shout_no_shout);
//        noConnectionInFeed = (TextView) findViewById(R.id.explore_shout_no_connection);
//
//        getMyLikesAndFollowedUsers();
//
//        newMap = setUpMapIfNeeded();
    }

    private void getMyLikesAndFollowedUsers() {
        ApiUtils.getMyLikesAndFollowedUsers(this, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(ExploreActivity.this);
                    return;
                }

                if (status.getError() == null) {
                    JSONObject result = null;

                    try {
                        result = object.getJSONObject("result");

                        JSONArray rawFollowedByMe = result.getJSONArray("current_user_followed_user_ids");
                        JSONArray rawLikes = result.getJSONArray("likes");

                        int likeCount = rawLikes.length();

                        for (int i = 0 ; i < likeCount ; i++) {
                            myLikes.add(Integer.parseInt(((JSONObject) rawLikes.get(i)).getString("shout_id")));
                        }

                        int count = rawFollowedByMe.length();

                        for (int i = 0 ; i < count ; i++) {
                            followedByMe.add(Integer.parseInt(rawFollowedByMe.getString(i)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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

                shoutToRedirectToFromCreateOrNotif = shout;
            }
        }

        //Case where user just created a shout
        if (shoutToRedirectToFromCreateOrNotif != null) {
            redirectToShout(shoutToRedirectToFromCreateOrNotif);
        }

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        //But activity gets destroyed when user shout with photo (memory issue), so don't initialize in that case!
        if (newMap && shoutToRedirectToFromCreateOrNotif == null) {
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

    private void pullShouts() {
        updateUIForLoadingShouts();

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
                            updateUIForDisplayShouts();
                        } else {
                            showNoShoutInFeedMessage();
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
        Projection projection = mMap.getProjection();
        LatLngBounds bounds = new LatLngBounds(projection.fromScreenLocation(shoutAreaPoints[0]),
                                               projection.fromScreenLocation(shoutAreaPoints[1]));

//        mapReqHandler.addMapRequest(aq, bounds);
    }

    public void displayProfile(int userId) {
        Intent profile = new Intent(this, ProfileActivity.class);
        profile.putExtra("userId", userId);
        startActivityForResult(profile, Constants.PROFILE_REQUEST);
    }

    private void setPullShoutArea() {
        int SHOUT_AREA_MARGIN_IN_DPI = 10;
        int FEED_HEIGHT_IN_DPI = 220;

        if (screenHeight == 0 && screenWidth == 0) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            screenWidth = size.x;
            screenHeight = size.y;
        }


        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SHOUT_AREA_MARGIN_IN_DPI, getResources().getDisplayMetrics());
        int feedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, FEED_HEIGHT_IN_DPI, getResources().getDisplayMetrics());

        int height = screenHeight - feedHeight - 2 * margin;

        Point bottomLeft = new Point(margin, height);
        Point TopRight = new Point(screenWidth - margin, margin);

        shoutAreaPoints = new Point[] {bottomLeft, TopRight};
    }

    private void updateUIForDisplayShouts() {
        // Instantiate a ViewPager and a PagerAdapter.                   (
        shoutPagerAdapter = new ShoutsPagerAdapter(ExploreActivity.this.getSupportFragmentManager(), shouts);
        shoutViewPager.setAdapter(shoutPagerAdapter);
        updateSelectedShoutMarker(shouts.get(0));
//        shoutViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        shoutViewPager.setVisibility(View.VISIBLE);
        noShoutInFeed.setVisibility(View.GONE);

        updateShoutCountViews(0);
    }

    private void updateUIForLoadingShouts() {
        mMap.clear();
        shoutSelectedOnMap = null;
        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();
        shouts = null;
        shoutViewPager = null;

        updateShoutCountViews(0);

        shoutViewPager.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        shoutProgressBar.setVisibility(View.VISIBLE);
    }

    private void showNoConnectionInFeedMessage() {
        shoutViewPager.setVisibility(View.GONE);
        shoutProgressBar.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
    }

    private void showNoShoutInFeedMessage() {
        shoutViewPager.setAdapter(null);
        shoutViewPager.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.VISIBLE);
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

        markerOptions.icon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout.anonymous, false)));

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
               oldSelectedMarker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shoutSelectedOnMap.anonymous, false)));
           }
       }

       shoutSelectedOnMap = shout;

       Marker marker = displayedShoutMarkers.get(shout.id);

       marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getShoutMarkerImageResource(shout.anonymous, true)));

       marker.showInfoWindow();
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

    private void updateShoutCountViews(int selected) {
        int MAX_COUNT_VIEWS = 20;

        int count = 0;

        if (shoutPagerAdapter != null) {
            count = shoutPagerAdapter.getCount();
        }

//        LinearLayout shoutCountContainer = (LinearLayout) findViewById(R.id.explore_shout_count_container);

//        shoutCountContainer.removeAllViews();
//
//        for (int i = 0 ; i < Math.min(MAX_COUNT_VIEWS, count) ; i++) {
//
//            View shoutCountView = new View(this);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(15, 15);
//
//            if (i != 0) {
//                params.setMargins(15, 0, 0, 0);
//            }
//
//            shoutCountView.setLayoutParams(params);
//
//            if (i == selected) {
//                shoutCountView.setBackgroundColor(getResources().getColor(R.color.shoutBlue));
//            } else {
//                shoutCountView.setBackgroundColor(getResources().getColor(R.color.semiTransparentBlack));
//            }
//
//            shoutCountContainer.addView(shoutCountView);
//        }
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
            settings.setAllGesturesEnabled(false);
            mMap.setMyLocationEnabled(true);

            //Pull shouts on the map
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (shoutToRedirectToFromCreateOrNotif == null && shoutToRedirectToFromZoom == null) {
                        pullShouts();
                    } else {
                        shoutToRedirectToFromCreateOrNotif = null;
                        shoutToRedirectToFromZoom = null;
                    }
                }
            });

            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    onMapShoutSelected(marker);

                    marker.showInfoWindow();

                    return true;
                }
            });

//            findViewById(R.id.explore_my_location_button).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (myLocation != null) {
//                        CameraUpdate update;
//                        if (mMap.getCameraPosition().zoom < Constants.CLICK_ON_MY_LOCATION_BUTTON) {
//                            update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.CLICK_ON_MY_LOCATION_BUTTON);
//                        } else {
//                            update = CameraUpdateFactory.newLatLng(LocationUtils.toLatLng(myLocation));
//                        }
//                        mMap.animateCamera(update);
//                    } else {
//                        Toast toast = Toast.makeText(ExploreActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
//                        toast.show();
//                    }
//                }
//            });

            return true;
        }

        return false;
    }

    private void initializeCameraWithLocation(Location location) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(location), Constants.EXPLORE_ZOOM);
        mMap.moveCamera(update);
    }

    private void initializeCameraWithCameraPosition(CameraPosition cameraPosition) {
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(update);
    }

    public void redirectToShout(Shout shoutToRedirectTo) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shoutToRedirectTo.lat, shoutToRedirectTo.lng), Constants.REDIRECTION_FROM_CREATE_SHOUT);
        mMap.moveCamera(update);

        shouts = new ArrayList<Shout>();
        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();
        mMap.clear();

        displayedShoutModels.put(shoutToRedirectTo.id, shoutToRedirectTo);
        Marker shoutMarker = displayShoutOnMap(shoutToRedirectTo);
        displayedShoutMarkers.put(shoutToRedirectTo.id, shoutMarker);
        shouts.add(shoutToRedirectTo);

        updateUIForDisplayShouts();
    }

    public void startDisplayActivity(Shout shout) {
        Intent displayShout = new Intent(this, DisplayActivity.class);
        displayShout.putExtra("shout", shout);

        if (this.myLocation != null && this.myLocation.getLatitude() != 0 && this.myLocation.getLongitude() != 0)  {
            displayShout.putExtra("myLocation", this.myLocation);
        }
        startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.DISPLAY_SHOUT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra("delete")) {
                    pullShouts();
                }
            }
        }

        if (requestCode == Constants.PROFILE_REQUEST) {
            getMyLikesAndFollowedUsers();
        }
    }
}