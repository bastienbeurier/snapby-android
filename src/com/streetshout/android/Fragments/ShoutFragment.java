package com.streetshout.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.activities.DisplayImageActivity;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

public class ShoutFragment extends Fragment {
    TextView userNameView = null;

    TextView descriptionView = null;

    TextView shoutAgeView = null;

    TextView shoutAgeUnitView = null;

    TextView shoutDistanceView = null;

    TextView shoutDistanceUnitView = null;

    ImageView imageView = null;

    ImageView imageViewPlaceHolder = null;

    ShoutModel currentDisplayedShout = null;

    private OnZoomOnShoutListener zoomOnShoutListener;

    private AQuery fragmentAQuery = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentAQuery = new AQuery(getActivity());

        return inflater.inflate(R.layout.shout_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        userNameView = (TextView) getView().findViewById(R.id.shout_title);
        descriptionView = (TextView) getView().findViewById(R.id.shout_body);
        imageView = (ImageView) getView().findViewById(R.id.shout_fragment_image);
        imageViewPlaceHolder = (ImageView) getView().findViewById(R.id.shout_fragment_image_place_holder);
        shoutAgeView = (TextView) getView().findViewById(R.id.shout_fragment_shout_age);
        shoutAgeUnitView = (TextView) getView().findViewById(R.id.shout_fragment_shout_age_unit);
        shoutDistanceView = (TextView) getView().findViewById(R.id.shout_fragment_shout_distance);
        shoutDistanceUnitView = (TextView) getView().findViewById(R.id.shout_fragment_shout_age_distance_unit);

        userNameView.setText(getString(R.string.no_shout_displayed));
    }

    //TODO: rename to intializeFragment or something
    public void displayShoutInFragment(final ShoutModel shout, Location myLocation) {
        getView().findViewById(R.id.shout_container).setBackgroundColor(GeneralUtils.getShoutAgeColor(getActivity(), shout));

        getView().findViewById(R.id.shout_fragment_back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        getView().findViewById(R.id.shout_fragment_zoom_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDisplayedShout != null) {
                    zoomOnShoutListener.zoomOnShout(currentDisplayedShout);
                }
            }
        });

        imageView.setImageResource(R.drawable.ic_default_image);

        currentDisplayedShout = shout;

        userNameView.setText(getString(R.string.shout_by) + " " + shout.displayName);
        descriptionView.setText(shout.description);

        String[] ageStrings = TimeUtils.shoutAgeToStrings(getActivity(), TimeUtils.getShoutAge(shout.created));

        shoutAgeView.setText(ageStrings[0]);
        if (ageStrings[1] != "") {
            shoutAgeUnitView.setText(ageStrings[1] + " " + getText(R.string.ago));
        } else {
            shoutAgeUnitView.setText("");
        }

        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);

            String[] distanceStrings = LocationUtils.formattedDistanceStrings(getActivity(), myLocation, shoutLocation);
            shoutDistanceView.setText(distanceStrings[0]);
            if (distanceStrings[1] != "") {
                shoutDistanceUnitView.setText(distanceStrings[1] + " " + getText(R.string.away));
            } else {
                shoutDistanceUnitView.setText("");
            }

        }

        if (shout.image != null && shout.image.length() > 0) {
            imageViewPlaceHolder.setVisibility(View.VISIBLE);

            fragmentAQuery.id(imageView).image(shout.image + "--400");
            imageView.setVisibility(View.VISIBLE);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent displayImage = new Intent(getActivity(), DisplayImageActivity.class);
                    displayImage.putExtra("image", shout.image);
                    startActivityForResult(displayImage, Constants.DISPLAY_PHOTO_REQUEST);
                }
            });
        } else {
            imageView.setVisibility(View.GONE);
            imageViewPlaceHolder.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            zoomOnShoutListener = (OnZoomOnShoutListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnShoutSelectedListener");
        }
    }

    public interface OnZoomOnShoutListener {
        public void zoomOnShout(ShoutModel shout);
    }
}
