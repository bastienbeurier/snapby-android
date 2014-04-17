package com.snapby.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
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
import com.snapby.android.R;
import com.snapby.android.activities.SettingsActivity;
import com.snapby.android.adapters.MapWindowAdapter;
import com.snapby.android.adapters.SnapbiesPagerAdapter;
import com.snapby.android.custom.SnapbyViewPagerContainer;
import com.snapby.android.models.Snapby;
import com.snapby.android.models.User;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bastien on 4/11/14.
 */
public class ProfileFragment extends Fragment {

    private int userId = 0;
    private User user = null;
    private ImageView profilePicture = null;
    private TextView username = null;
    private FrameLayout profilePictureContainer = null;
    private TextView snapbyCountView = null;
    private boolean imageLoaded = false;

    private HashMap<Integer, Snapby> displayedSnapbyModels = null;

    private HashMap<Integer, Marker>  displayedSnapbyMarkers = null;

    private GoogleMap profileMap = null;

    private ViewPager snapbyViewPager;

    private PagerAdapter snapbyPagerAdapter;

    private SnapbyViewPagerContainer viewPagerContainer = null;

    private TextView noSnapbyInFeed = null;

    private TextView noConnectionInFeed = null;

    private TextView likedSnapbies = null;

    private View userInfoContainer = null;

    private Snapby snapbySelectedOnMap = null;

    private ArrayList<Snapby> snapbies = null;

    public boolean mapLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.profile, container, false);

        snapbyViewPager = (ViewPager) rootView.findViewById(R.id.profile_view_pager);
        viewPagerContainer = (SnapbyViewPagerContainer) rootView.findViewById(R.id.profile_snapby_view_pager_container);
        profilePicture = (ImageView) rootView.findViewById(R.id.profile_user_picture);
        username = (TextView) rootView.findViewById(R.id.profile_username);
        profilePictureContainer = (FrameLayout) rootView.findViewById(R.id.profile_profile_picture_container);
        snapbyCountView = (TextView) rootView.findViewById(R.id.profile_snapby_count);
        userInfoContainer = rootView.findViewById(R.id.profile_user_info_container);
        likedSnapbies = (TextView) rootView.findViewById(R.id.profile_liked_count);

        profileMap = ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.profile_map)).getMap();

        profileMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                //First time map loads, check if we have a location
                if (!mapLoaded) {
                    mapLoaded = true;
                }
            }
        });

        DisplayMetrics metrics = getActivity().getResources().getDisplayMetrics();
        float offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220 + 10, metrics);

        profileMap.setPadding(0, 0, 0, (int) offset);

        snapbyViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateSelectedSnapbyMarker(snapbies.get(i));

                CameraUpdate update = CameraUpdateFactory.newLatLng(new LatLng(snapbies.get(i).lat, snapbies.get(i).lng));
                profileMap.animateCamera(update);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


        noSnapbyInFeed = (TextView) rootView.findViewById(R.id.profile_snapby_no_snapby);
        noConnectionInFeed = (TextView) rootView.findViewById(R.id.profile_snapby_no_connection);

        //Admin capability
        if (Constants.ADMIN) {
            if (Constants.PRODUCTION) {
                snapbyCountView.setTextColor(getResources().getColor(R.color.snapbyPink));
            } else {
                snapbyCountView.setTextColor(getResources().getColor(R.color.snapbyPink));
            }

            snapbyCountView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Constants.PRODUCTION = !Constants.PRODUCTION;

                    if (Constants.PRODUCTION) {
                        snapbyCountView.setTextColor(getResources().getColor(R.color.snapbyPink));
                    } else {
                        snapbyCountView.setTextColor(getResources().getColor(R.color.snapbyPink));
                    }

                    SessionUtils.logOut(getActivity());
                }
            });
        }

        profilePictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(getActivity(), SettingsActivity.class);
                settings.putExtra("chooseProfilePicture", true);
                startActivityForResult(settings, Constants.SETTINGS_REQUEST);
            }
        });

        userId = SessionUtils.getCurrentUser(getActivity()).id;

        getUserInfo();

        viewPagerContainer.setClipChildren(false);
        viewPagerContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        snapbyViewPager.setOffscreenPageLimit(4);
        snapbyViewPager.setPageMargin(30);

        return rootView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        setUpMap();

        getUserSnapbies();

        super.onActivityCreated(savedInstanceState);
    }

    public void getUserInfo() {
        ApiUtils.getUserInfo(getActivity(), userId, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null) {
                    JSONObject result = null;
                    JSONObject rawUser = null;

                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    user = User.rawUserToInstance(rawUser);

                    if (imageLoaded) {
                        updateUI(false);
                    } else {
                        updateUI(true);
                        imageLoaded = true;
                    }
                }
            }
        });
    }

    public void getUserSnapbies() {
        updateUIForLoadingSnapbies();

        ApiUtils.getSnapbies(getActivity(), userId, 1, 100, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawSnapbies = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawSnapbies = result.getJSONArray("snapbies");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    noConnectionInFeed.setVisibility(View.GONE);

                    if (rawSnapbies.length() > 0) {
                        snapbies = Snapby.rawSnapbiesToInstances(rawSnapbies);

                        displaySnapbiesOnMap(snapbies);

                        updateUIForDisplaySnapbies();
                    } else {
                        showNoSnapbyInFeedMessage();
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void updateUI(boolean reloadPicture) {
        snapbyCountView.setText("" + user.snapbyCount);


        if (reloadPicture) {
            GeneralUtils.getAquery(getActivity()).id(profilePicture).image(GeneralUtils.getProfileBigPicturePrefix() + user.id, false, false, 0, 0, null, AQuery.FADE_IN);
        }

        username.setText("@" + user.username);
        likedSnapbies.setText("" + user.likedSnapbies);
    }

    private void updateUIForDisplaySnapbies() {
        viewPagerContainer.setVisibility(View.VISIBLE);

        // Instantiate a ViewPager and a PagerAdapter.
        snapbyPagerAdapter = new SnapbiesPagerAdapter(getActivity().getSupportFragmentManager(), snapbies, "profile");
        snapbyViewPager.setAdapter(snapbyPagerAdapter);
        updateSelectedSnapbyMarker(snapbies.get(0));
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(snapbies.get(0).lat, snapbies.get(0).lng), Constants.INITIAL_PROFILE_ZOOM);
        profileMap.animateCamera(update);

        noSnapbyInFeed.setVisibility(View.GONE);
    }

    private void updateUIForLoadingSnapbies() {
        profileMap.clear();
        snapbySelectedOnMap = null;
        displayedSnapbyModels = new HashMap<Integer, Snapby>();
        displayedSnapbyMarkers = new HashMap<Integer, Marker>();
        snapbies = null;

        noConnectionInFeed.setVisibility(View.GONE);
        noSnapbyInFeed.setVisibility(View.GONE);
        viewPagerContainer.setVisibility(View.GONE);
    }

    private void showNoConnectionInFeedMessage() {
        noSnapbyInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
        viewPagerContainer.setVisibility(View.GONE);
    }

    private void showNoSnapbyInFeedMessage() {
        snapbyViewPager.setAdapter(null);
        noSnapbyInFeed.setVisibility(View.VISIBLE);
        viewPagerContainer.setVisibility(View.GONE);
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

        return profileMap.addMarker(markerOptions);
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
        UiSettings settings = profileMap.getUiSettings();
        settings.setZoomControlsEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        settings.setAllGesturesEnabled(false);
        profileMap.setMyLocationEnabled(true);

        profileMap.setInfoWindowAdapter(new MapWindowAdapter(getActivity()));

        profileMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
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
