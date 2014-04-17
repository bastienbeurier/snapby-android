package com.snapby.android.fragments;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
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
import com.snapby.android.R;
import com.snapby.android.activities.DisplayActivity;
import com.snapby.android.activities.MainActivity;
import com.snapby.android.adapters.MapWindowAdapter;
import com.snapby.android.adapters.SnapbiesPagerAdapter;
import com.snapby.android.custom.SnapbyViewPagerContainer;
import com.snapby.android.models.Snapby;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.LocationUtils;
import com.snapby.android.utils.MapRequestHandler;
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

    private HashMap<Integer, Snapby> displayedSnapbyModels = null;

    private HashMap<Integer, Marker>  displayedSnapbyMarkers = null;

    private ViewPager snapbyViewPager;

    private PagerAdapter snapbyPagerAdapter;

    private SnapbyViewPagerContainer viewPagerContainer = null;

    private TextView noSnapbyInFeed = null;

    private TextView noConnectionInFeed = null;

    private Snapby snapbySelectedOnMap = null;

    private ArrayList<Snapby> snapbies = null;

    private MapRequestHandler mapReqHandler = null;

    private ImageView refreshButton = null;

    private View snapbyProgressBar = null;

    public boolean mapLoaded = false;

    private View noLocationDialog = null;

    public boolean waitingForLocation = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.explore, container, false);

        mapReqHandler = new MapRequestHandler();

        snapbyViewPager = (ViewPager) rootView.findViewById(R.id.explore_view_pager);
        viewPagerContainer = (SnapbyViewPagerContainer) rootView.findViewById(R.id.explore_snapby_view_pager_container);
        refreshButton = (ImageView) rootView.findViewById(R.id.explore_refresh_button);
        snapbyProgressBar = rootView.findViewById(R.id.explore_snapby_progress_bar);
        noLocationDialog = rootView.findViewById(R.id.explore_snapby_no_location);

        exploreMap = ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map)).getMap();

        DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        float offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220 + 10, metrics);

        exploreMap.setPadding(0, 0, 0, (int) offset);

        exploreMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //First time map loads, check if we have a location
                if (!mapLoaded) {
                    mapLoaded = true;

                    Location myLocation = ((MainActivity) getActivity()).myLocation;

                    //If we have a location, move the map to there
                    if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.EXPLORE_ZOOM);
                        exploreMap.moveCamera(update);

                    //Else wait for location
                    } else {
                        waitingForLocation();
                    }
                //Else pull local snapbies
                } else {
                    loadContent();
                }
            }
        });

        snapbyViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateSelectedSnapbyMarker(snapbies.get(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        noSnapbyInFeed = (TextView) rootView.findViewById(R.id.explore_snapby_no_snapby);
        noConnectionInFeed = (TextView) rootView.findViewById(R.id.explore_snapby_no_connection);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location myLocation = ((MainActivity) getActivity()).myLocation;
                if (myLocation != null && (myLocation.getLongitude() == 0 || myLocation.getLatitude() == 0)) {
                    CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(myLocation), Constants.EXPLORE_ZOOM);
                    exploreMap.moveCamera(update);
                }

                loadContent();
            }
        });

        viewPagerContainer.setClipChildren(false);
        viewPagerContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        snapbyViewPager.setOffscreenPageLimit(4);
        snapbyViewPager.setPageMargin(30);

        return rootView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        setUpMap();

        super.onActivityCreated(savedInstanceState);
    }

    private void waitingForLocation() {
        waitingForLocation = true;
        noLocationDialog.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);
        snapbyProgressBar.setVisibility(View.GONE);
    }

    private void dismissWaitingForLocation() {
        waitingForLocation = false;
        noLocationDialog.setVisibility(View.GONE);
        refreshButton.setEnabled(true);
    }

    public void loadContent() {
        Location myLocation = ((MainActivity) getActivity()).myLocation;
        if (myLocation == null || (myLocation.getLongitude() == 0 && myLocation.getLatitude() == 0)) {
            waitingForLocation();
            return;
        }

        if (exploreMap.getCameraPosition().zoom < Constants.EXPLORE_ZOOM - 1) {
            return;
        }

        ((MainActivity) getActivity()).updateLocalSnapbyCount();

        dismissWaitingForLocation();

        //Add a request to populate the map with snapbies
        LatLngBounds mapBounds = exploreMap.getProjection().getVisibleRegion().latLngBounds;

        updateUIForLoadingSnapbies();

        //Set listener to catch API response from the MapRequestHandler
        mapReqHandler.setRequestResponseListener(new MapRequestHandler.RequestResponseListener() {
            @Override
            public void responseReceived(String url, JSONObject object, AjaxStatus status) {
                if (status.getError() == null) {
                    JSONArray rawSnapbies;
                    try {

                        if (object != null) {
                            JSONObject rawResult = object.getJSONObject("result");
                            rawSnapbies = rawResult.getJSONArray("snapbies");
                        } else {
                            showNoConnectionInFeedMessage();
                            return;
                        }

                        snapbies = Snapby.rawSnapbiesToInstances(rawSnapbies);

                        displaySnapbiesOnMap(snapbies);

                        noConnectionInFeed.setVisibility(View.GONE);

                        if (snapbies.size() > 0) {
                            updateUIForDisplaySnapbies();
                        } else {
                            showNoSnapbyInFeedMessage();
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

    private void updateUIForDisplaySnapbies() {
        exploreMap.setMyLocationEnabled(true);
        snapbyProgressBar.setVisibility(View.GONE);
        viewPagerContainer.setVisibility(View.VISIBLE);

        // Instantiate a ViewPager and a PagerAdapter.                   (
        snapbyPagerAdapter = new SnapbiesPagerAdapter(getActivity().getSupportFragmentManager(), snapbies);
        snapbyViewPager.setAdapter(snapbyPagerAdapter);
        updateSelectedSnapbyMarker(snapbies.get(0));

        noSnapbyInFeed.setVisibility(View.GONE);
    }

    private void updateUIForLoadingSnapbies() {
        exploreMap.setMyLocationEnabled(false);
        snapbyProgressBar.setVisibility(View.VISIBLE);

        exploreMap.clear();
        snapbySelectedOnMap = null;
        displayedSnapbyModels = new HashMap<Integer, Snapby>();
        displayedSnapbyMarkers = new HashMap<Integer, Marker>();
        snapbies = null;

        noConnectionInFeed.setVisibility(View.GONE);
        noSnapbyInFeed.setVisibility(View.GONE);
        viewPagerContainer.setVisibility(View.GONE);
    }

    private void showNoConnectionInFeedMessage() {
        exploreMap.setMyLocationEnabled(true);
        snapbyProgressBar.setVisibility(View.GONE);
        noSnapbyInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
        viewPagerContainer.setVisibility(View.GONE);
    }

    private void showNoSnapbyInFeedMessage() {
        exploreMap.setMyLocationEnabled(true);
        snapbyProgressBar.setVisibility(View.GONE);
        snapbyViewPager.setAdapter(null);
        noSnapbyInFeed.setVisibility(View.VISIBLE);
        snapbyViewPager.setVisibility(View.GONE);
    }

    private void displaySnapbiesOnMap(List<Snapby> snapbies) {
        displayedSnapbyModels.clear();
        HashMap<Integer, Marker> newDisplayedSnapbyMarkers = new HashMap<Integer, Marker>();

        for (Snapby snapby: snapbies) {
            displayedSnapbyModels.put(snapby.id, snapby);

            //Check that the snapby is not already marked on the map
            if (!displayedSnapbyMarkers.containsKey(snapby.id)) {
                Marker snapbyMarker = displaySnapbyOnMap(snapby);
                newDisplayedSnapbyMarkers.put(snapby.id, snapbyMarker);
                //If he is, re-use the marker
            } else {
                newDisplayedSnapbyMarkers.put(snapby.id, displayedSnapbyMarkers.get(snapby.id));
                displayedSnapbyMarkers.remove(snapby.id);
            }
        }

        for (HashMap.Entry<Integer, Marker> entry: displayedSnapbyMarkers.entrySet()) {
            entry.getValue().remove();
        }

        displayedSnapbyMarkers = newDisplayedSnapbyMarkers;
    }

    private Marker displaySnapbyOnMap(Snapby snapby) {
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(new LatLng(snapby.lat, snapby.lng));

        markerOptions.icon(BitmapDescriptorFactory.fromResource(GeneralUtils.getSnapbyMarkerImageResource(snapby.anonymous, false)));

        markerOptions.title(Integer.toString(snapby.id));

        markerOptions.anchor(0.35f, 0.9f);

        return exploreMap.addMarker(markerOptions);
    }

    private void updateSelectedSnapbyMarker(Snapby snapby) {
        if (snapbySelectedOnMap != null) {
            Marker oldSelectedMarker = displayedSnapbyMarkers.get(snapbySelectedOnMap.id);
            if (oldSelectedMarker != null) {
                oldSelectedMarker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getSnapbyMarkerImageResource(snapbySelectedOnMap.anonymous, false)));
                oldSelectedMarker.setAnchor(0.35f, 0.9f);
            }
        }

        snapbySelectedOnMap = snapby;

        Marker marker = displayedSnapbyMarkers.get(snapby.id);

        marker.setIcon(BitmapDescriptorFactory.fromResource(GeneralUtils.getSnapbyMarkerImageResource(snapby.anonymous, true)));

        marker.setAnchor(0.5f, 0.8f);

        marker.showInfoWindow();
    }

    private void onMapSnapbySelected(Marker marker) {
        Snapby selectedSnapby = displayedSnapbyModels.get(Integer.parseInt(marker.getTitle()));

        int count = snapbies.size();

        for (int i = 0; i < count; i++) {
            if (snapbies.get(i).id == selectedSnapby.id) {
                //Creates a fragment that calls onFeedSnapbySelected
                snapbyViewPager.setCurrentItem(i);
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
                onMapSnapbySelected(marker);

                marker.showInfoWindow();

                return true;
            }
        });
    }

    public void reloadAdapterIfAlreadyLoaded() {
        if (snapbyPagerAdapter != null) {
            snapbyPagerAdapter = new SnapbiesPagerAdapter(getActivity().getSupportFragmentManager(), snapbies, "profile");
            snapbyViewPager.setAdapter(snapbyPagerAdapter);
        }
    }

}
