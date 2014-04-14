package com.streetshout.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.R;
import com.streetshout.android.activities.SettingsActivity;
import com.streetshout.android.adapters.MapWindowAdapter;
import com.streetshout.android.adapters.ShoutsPagerAdapter;
import com.streetshout.android.custom.ShoutViewPagerContainer;
import com.streetshout.android.models.Shout;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
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

    private User user = null;
    private ImageView profilePicture = null;
    private TextView username = null;
    private FrameLayout profilePictureContainer = null;
    private TextView shoutCountView = null;
    private boolean imageLoaded = false;

    private HashMap<Integer, Shout> displayedShoutModels = null;

    private HashMap<Integer, Marker>  displayedShoutMarkers = null;

    private GoogleMap profileMap = null;

    private ViewPager shoutViewPager;

    private PagerAdapter shoutPagerAdapter;

    private ShoutViewPagerContainer viewPagerContainer = null;

    private TextView noShoutInFeed = null;

    private TextView noConnectionInFeed = null;

    private View userInfoContainer = null;

    private Shout shoutSelectedOnMap = null;

    private ArrayList<Shout> shouts = null;

    private boolean mapPaddingNotSet = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.profile, container, false);

        shoutViewPager = (ViewPager) rootView.findViewById(R.id.profile_view_pager);
        viewPagerContainer = (ShoutViewPagerContainer) rootView.findViewById(R.id.profile_shout_view_pager_container);
        profilePicture = (ImageView) rootView.findViewById(R.id.profile_user_picture);
        username = (TextView) rootView.findViewById(R.id.profile_username);
        profilePictureContainer = (FrameLayout) rootView.findViewById(R.id.profile_profile_picture_container);
        shoutCountView = (TextView) rootView.findViewById(R.id.profile_shout_count);
        userInfoContainer = rootView.findViewById(R.id.profile_user_info_container);

        profileMap = ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.profile_map)).getMap();

        shoutViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                updateSelectedShoutMarker(shouts.get(i));

                if (mapPaddingNotSet) {
                    profileMap.setPadding(0, userInfoContainer.getHeight(), 0, viewPagerContainer.getHeight());
                    mapPaddingNotSet = false;
                }

                CameraUpdate update = CameraUpdateFactory.newLatLng(new LatLng(shouts.get(i).lat, shouts.get(i).lng));
                profileMap.animateCamera(update);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


        noShoutInFeed = (TextView) rootView.findViewById(R.id.profile_shout_no_shout);
        noConnectionInFeed = (TextView) rootView.findViewById(R.id.profile_shout_no_connection);

        //Admin capability
        if (Constants.ADMIN) {
            if (Constants.PRODUCTION) {
                shoutCountView.setTextColor(getResources().getColor(R.color.shoutBlue));
            } else {
                shoutCountView.setTextColor(getResources().getColor(R.color.shoutPink));
            }

            shoutCountView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Constants.PRODUCTION = !Constants.PRODUCTION;

                    if (Constants.PRODUCTION) {
                        shoutCountView.setTextColor(getResources().getColor(R.color.shoutBlue));
                    } else {
                        shoutCountView.setTextColor(getResources().getColor(R.color.shoutPink));
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

        getMyInfo();

        return rootView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        setUpMap();

        getMyShouts();

        super.onActivityCreated(savedInstanceState);
    }

    public void loadContent() {
        getMyInfo();
    }

    private void getMyInfo() {

        ApiUtils.getUserInfo(getActivity(), SessionUtils.getCurrentUser(getActivity()).id, new AjaxCallback<JSONObject>() {
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

    private void getMyShouts() {
        updateUIForLoadingShouts();

        ApiUtils.getShouts(getActivity(), SessionUtils.getCurrentUser(getActivity()).id, 1, 100, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawShouts = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawShouts = result.getJSONArray("shouts");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    noConnectionInFeed.setVisibility(View.GONE);

                    if (rawShouts.length() > 0) {
                        shouts = Shout.rawShoutsToInstances(rawShouts);

                        displayShoutsOnMap(shouts);

                        updateUIForDisplayShouts();
                    } else {
                        showNoShoutInFeedMessage();
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void updateUI(boolean reloadPicture) {
        shoutCountView.setText("" + user.shoutCount);


        if (reloadPicture) {
            GeneralUtils.getAquery(getActivity()).id(profilePicture).image(GeneralUtils.getProfileBigPicturePrefix() + user.id, false, false, 0, 0, null, AQuery.FADE_IN);
        }

        username.setText("@" + user.username);
    }

    private void updateUIForDisplayShouts() {
        // Instantiate a ViewPager and a PagerAdapter.
        shoutPagerAdapter = new ShoutsPagerAdapter(getActivity().getSupportFragmentManager(), shouts, "profile");
        shoutViewPager.setAdapter(shoutPagerAdapter);
        updateSelectedShoutMarker(shouts.get(0));
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shouts.get(0).lat, shouts.get(0).lng), Constants.INITIAL_PROFILE_ZOOM);
        profileMap.animateCamera(update);
        shoutViewPager.setOffscreenPageLimit(4);
        shoutViewPager.setPageMargin(30);
        shoutViewPager.setClipChildren(false);
        shoutViewPager.setVisibility(View.VISIBLE);

        noShoutInFeed.setVisibility(View.GONE);

        viewPagerContainer.setClipChildren(false);
        viewPagerContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void updateUIForLoadingShouts() {
        profileMap.clear();
        shoutSelectedOnMap = null;
        displayedShoutModels = new HashMap<Integer, Shout>();
        displayedShoutMarkers = new HashMap<Integer, Marker>();
        shouts = null;

        shoutViewPager.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
    }

    private void showNoConnectionInFeedMessage() {
        shoutViewPager.setVisibility(View.GONE);
        noShoutInFeed.setVisibility(View.GONE);
        noConnectionInFeed.setVisibility(View.VISIBLE);
    }

    private void showNoShoutInFeedMessage() {
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

        return profileMap.addMarker(markerOptions);
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
        UiSettings settings = profileMap.getUiSettings();
        settings.setZoomControlsEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        settings.setAllGesturesEnabled(false);
        profileMap.setMyLocationEnabled(true);

        profileMap.setInfoWindowAdapter(new MapWindowAdapter(getActivity()));

        profileMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                onMapShoutSelected(marker);

                marker.showInfoWindow();

                return true;
            }
        });
    }

    public void reloadAdapterIfAlreadyLoaded() {
        if (shoutPagerAdapter != null) {
            shoutPagerAdapter = new ShoutsPagerAdapter(getActivity().getSupportFragmentManager(), shouts, "profile");
            shoutViewPager.setAdapter(shoutPagerAdapter);
        }
    }
}
