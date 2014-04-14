package com.streetshout.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.streetshout.android.R;
import com.streetshout.android.activities.DisplayActivity;
import com.streetshout.android.activities.MainActivity;
import com.streetshout.android.activities.ProfileActivity;
import com.streetshout.android.adapters.MapWindowAdapter;
import com.streetshout.android.adapters.ShoutsPagerAdapter;
import com.streetshout.android.custom.ShoutViewPagerContainer;
import com.streetshout.android.custom.VelocityViewPager;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.MapRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bastien on 4/11/14.
 */
public class ExploreFragment extends Fragment {
    public GoogleMap exploreMap = null;

    private HashMap<Integer, Shout> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    private ViewPager shoutViewPager;

    private PagerAdapter shoutPagerAdapter;

    private ShoutViewPagerContainer viewPagerContainer = null;

    private TextView noShoutInFeed = null;

    private TextView noConnectionInFeed = null;

    private Shout shoutSelectedOnMap = null;

    private ArrayList<Shout> shouts = null;

    private MapRequestHandler mapReqHandler = null;

    private ProgressDialog progressDialog = null;

    private ImageView refreshButton = null;

    private View shoutProgressBar = null;

    private boolean mapLoaded = false;

    private boolean mapPaddingNotSet = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.explore, container, false);

        mapReqHandler = new MapRequestHandler();

        shoutViewPager = (ViewPager) rootView.findViewById(R.id.explore_view_pager);
        viewPagerContainer = (ShoutViewPagerContainer) rootView.findViewById(R.id.explore_shout_view_pager_container);
        refreshButton = (ImageView) rootView.findViewById(R.id.explore_refresh_button);
        shoutProgressBar = rootView.findViewById(R.id.explore_shout_progress_bar);

        exploreMap = ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map)).getMap();

        exploreMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //First time map loads, check if we have a location
                if (!mapLoaded) {
                    mapLoaded = true;

                    if (mapPaddingNotSet) {
                        mapPaddingNotSet = false;

                        exploreMap.setPadding(0, 0, 0, viewPagerContainer.getHeight());
                    }

                    Location myLocation = ((MainActivity) getActivity()).myLocation;

                    //If we have a location, move the map to there
                    if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.EXPLORE_ZOOM);
                        exploreMap.moveCamera(update);
                    //Else wait for location
                    } else {
                        waitingForLocation();
                    }
                //Else pull local shouts
                } else {
                    loadContent();
                }
            }
        });

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

        noShoutInFeed = (TextView) rootView.findViewById(R.id.explore_shout_no_shout);
        noConnectionInFeed = (TextView) rootView.findViewById(R.id.explore_shout_no_connection);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadContent();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        setUpMap();

        super.onActivityCreated(savedInstanceState);
    }

    private void waitingForLocation() {
        progressDialog = ProgressDialog.show(getActivity(), "", getString(R.string.waiting_for_location), false);
    }

    private void loadContent() {
        ((MainActivity) getActivity()).updateLocalShoutCount();

        if (progressDialog != null) {
            progressDialog.cancel();
        }

        //Add a request to populate the map with shouts
        LatLngBounds mapBounds = exploreMap.getProjection().getVisibleRegion().latLngBounds;

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

                        displayShoutsOnMap(shouts);

                        noConnectionInFeed.setVisibility(View.GONE);

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

        mapReqHandler.addMapRequest(GeneralUtils.getAquery(getActivity()), mapBounds);
    }

    public LatLngBounds getExploreMapBounds() {
        return exploreMap.getProjection().getVisibleRegion().latLngBounds;
    }

    public void displayProfile(int userId) {
        Intent profile = new Intent(getActivity(), ProfileActivity.class);
        profile.putExtra("userId", userId);
        startActivityForResult(profile, Constants.PROFILE_REQUEST);
    }

    private void updateUIForDisplayShouts() {
        exploreMap.setMyLocationEnabled(true);
        shoutProgressBar.setVisibility(View.GONE);

        // Instantiate a ViewPager and a PagerAdapter.                   (
        shoutPagerAdapter = new ShoutsPagerAdapter(getActivity().getSupportFragmentManager(), shouts);
        shoutViewPager.setAdapter(shoutPagerAdapter);
        updateSelectedShoutMarker(shouts.get(0));
        shoutViewPager.setOffscreenPageLimit(4);
        shoutViewPager.setPageMargin(30);
        shoutViewPager.setClipChildren(false);
        shoutViewPager.setVisibility(View.VISIBLE);

        noShoutInFeed.setVisibility(View.GONE);

        viewPagerContainer.setClipChildren(false);
        viewPagerContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void updateUIForLoadingShouts() {
        exploreMap.setMyLocationEnabled(false);
        shoutProgressBar.setVisibility(View.VISIBLE);

        exploreMap.clear();
        shoutSelectedOnMap = null;
        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();
        shouts = null;

        shoutViewPager.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
    }

    private void showNoConnectionInFeedMessage() {
        exploreMap.setMyLocationEnabled(true);
        shoutProgressBar.setVisibility(View.GONE);
        shoutViewPager.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
    }

    private void showNoShoutInFeedMessage() {
        exploreMap.setMyLocationEnabled(true);
        shoutProgressBar.setVisibility(View.GONE);
        shoutViewPager.setAdapter(null);
        shoutViewPager.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.VISIBLE);
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

        return exploreMap.addMarker(markerOptions);
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

    /**
     *  Map-related methods
     */

    private void setUpMap() {
        //Set map settings
        UiSettings settings = exploreMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setAllGesturesEnabled(false);
        exploreMap.setMyLocationEnabled(true);

        exploreMap.setInfoWindowAdapter(new MapWindowAdapter(getActivity()));

        exploreMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                onMapShoutSelected(marker);

                marker.showInfoWindow();

                return true;
            }
        });
    }

    public void startDisplayActivity(Shout shout) {
        Intent displayShout = new Intent(getActivity(), DisplayActivity.class);
        displayShout.putExtra("shout", shout);
        startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
    }

    public void reloadAdapterIfAlreadyLoaded() {
        if (shoutPagerAdapter != null) {
            shoutPagerAdapter = new ShoutsPagerAdapter(getActivity().getSupportFragmentManager(), shouts, "profile");
            shoutViewPager.setAdapter(shoutPagerAdapter);
        }
    }

}
