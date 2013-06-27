package com.streetshout.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

public class ShoutFragment extends Fragment {
    TextView userNameView = null;

    TextView descriptionView = null;

    TextView timeStampView = null;

    ShoutModel currentDisplayedShout = null;

    private OnShoutSelectedListener shoutSelectedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shout_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        userNameView = (TextView) getView().findViewById(R.id.shout_title);
        descriptionView = (TextView) getView().findViewById(R.id.shout_body);
        timeStampView = (TextView) getView().findViewById(R.id.shout_stamp);

        userNameView.setText(getString(R.string.no_shout_displayed));

        getView().findViewById(R.id.shout_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDisplayedShout != null) {
                    shoutSelectedListener.onShoutSelected(currentDisplayedShout);
                }
            }
        });
    }

    public void displayShoutInFragment(ShoutModel shout, Location myLocation) {
        currentDisplayedShout = shout;

        userNameView.setText(shout.displayName);
        descriptionView.setText('"' + shout.description + '"');
        String shoutStamp = TimeUtils.shoutAgeToString(getActivity(), TimeUtils.getShoutAge(shout.created));
        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);
            shoutStamp += ", " + LocationUtils.formatedDistance(getActivity(), myLocation, shoutLocation);
        }

        timeStampView.setText(shoutStamp);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            shoutSelectedListener = (OnShoutSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnShoutSelectedListener");
        }
    }

    public interface OnShoutSelectedListener {
        public void onShoutSelected(ShoutModel shout);
    }
}
