package com.streetshout.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
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
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

public class ShoutFragment extends Fragment {
    TextView userNameView = null;

    TextView descriptionView = null;

    TextView timeStampView = null;

    ImageView imageView = null;

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
        timeStampView = (TextView) getView().findViewById(R.id.shout_stamp);
        imageView = (ImageView) getView().findViewById(R.id.shout_fragment_image);

        userNameView.setText(getString(R.string.no_shout_displayed));

        getView().findViewById(R.id.shout_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDisplayedShout != null) {
                    zoomOnShoutListener.zoomOnShout(currentDisplayedShout);
                }
            }
        });
    }

    //TODO: rename to intializeFragment or something
    public void displayShoutInFragment(final ShoutModel shout, Location myLocation) {
        imageView.setImageResource(R.drawable.ic_default_image);

        currentDisplayedShout = shout;

        userNameView.setText(shout.displayName);
        descriptionView.setText('"' + shout.description + '"');
        String shoutStamp = TimeUtils.shoutAgeToString(getActivity(), TimeUtils.getShoutAge(shout.created));
        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);
            shoutStamp += ", " + LocationUtils.formattedDistance(getActivity(), myLocation, shoutLocation);
        }

        if (shout.image != null && shout.image.length() > 0) {
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
        }

        timeStampView.setText(shoutStamp);
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
