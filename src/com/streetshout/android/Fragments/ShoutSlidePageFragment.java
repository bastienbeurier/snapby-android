package com.streetshout.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.activities.DisplayActivity;
import com.streetshout.android.activities.ExploreActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

/**
 * Created by bastien on 3/3/14.
 */
public class ShoutSlidePageFragment extends Fragment {

    private Shout shout = null;

    private AQuery aq = null;

    private ImageView imageView = null;

    private TextView descriptionView = null;

    private TextView ageView = null;

    private TextView distanceView = null;

    private TextView likeCountView = null;

    private TextView commentCountView = null;

    private TextView usernameView = null;

    private View coloredBar = null;

    private View coloredContainer = null;

    public static ShoutSlidePageFragment newInstance(Shout shout) {
        ShoutSlidePageFragment shoutSlidePageFragment = new ShoutSlidePageFragment();

        Bundle args = new Bundle();
        args.putParcelable("shout", shout);
        shoutSlidePageFragment.setArguments(args);

        return shoutSlidePageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        aq = GeneralUtils.getAquery(getActivity());

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.shout_slide_page_fragment, container, false);
        shout = getArguments().getParcelable("shout");

        imageView = (ImageView) rootView.findViewById(R.id.explore_shout_image);
        ageView = (TextView) rootView.findViewById(R.id.explore_shout_age);
        distanceView = (TextView) rootView.findViewById(R.id.explore_shout_distance);
        likeCountView = (TextView) rootView.findViewById(R.id.explore_shout_distance);
        commentCountView = (TextView) rootView.findViewById(R.id.explore_shout_distance);
        usernameView = (TextView) rootView.findViewById(R.id.explore_shout_username);
        descriptionView = (TextView) rootView.findViewById(R.id.explore_shout_description);
        coloredBar = rootView.findViewById(R.id.explore_shout_colored_bar);
        coloredContainer = rootView.findViewById(R.id.explore_shout_colored_container);

        aq.id(imageView).image(shout.image + "--400");

        String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));
        ageView.setText(ageStrings[0] + ageStrings[1]);

        Location myLocation = ((ExploreActivity) getActivity()).myLocation;
        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);

            String[] distanceStrings = LocationUtils.formattedDistanceStrings(getActivity(), myLocation, shoutLocation);
            distanceView.setText(distanceStrings[0] + distanceStrings[1]);
        } else {
            distanceView.setText("?");
        }

        if (shout.anonymous) {
            usernameView.setText(getResources().getString(R.string.anonymous_name));
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
            coloredBar.setBackgroundColor(getResources().getColor(R.color.anonymousGrey));
            ImageUtils.setBackground(getActivity(), coloredContainer, R.drawable.anonymous_shout_meta_info);
        } else {
            usernameView.setText("@" + shout.username);

            coloredBar.setBackgroundColor(getResources().getColor(R.color.publicYellow));
            ImageUtils.setBackground(getActivity(), coloredContainer, R.drawable.public_shout_meta_info);
        }
        descriptionView.setText(shout.description);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExploreActivity exploreActivity = ((ExploreActivity) getActivity());

                Intent displayShout = new Intent(exploreActivity, DisplayActivity.class);
                displayShout.putExtra("shout", shout);

                if (exploreActivity.myLocation != null && exploreActivity.myLocation.getLatitude() != 0 && exploreActivity.myLocation.getLongitude() != 0)  {
                    displayShout.putExtra("myLocation", exploreActivity.myLocation);
                }
                startActivity(displayShout);
            }
        });

        return rootView;
    }
}