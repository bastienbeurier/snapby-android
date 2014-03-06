package com.streetshout.android.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.activities.ExploreActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.SessionUtils;
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

    private View coloredMetaDataContainer = null;

    private LinearLayout coloredButtonContainer = null;

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

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.shout_slide_page_fragment, container, false);
        shout = getArguments().getParcelable("shout");

        imageView = (ImageView) rootView.findViewById(R.id.explore_shout_image);
        ageView = (TextView) rootView.findViewById(R.id.explore_shout_age);
        distanceView = (TextView) rootView.findViewById(R.id.explore_shout_distance);
        likeCountView = (TextView) rootView.findViewById(R.id.explore_shout_like_count);
        commentCountView = (TextView) rootView.findViewById(R.id.explore_shout_comment_count);
        usernameView = (TextView) rootView.findViewById(R.id.explore_shout_username);
        descriptionView = (TextView) rootView.findViewById(R.id.explore_shout_description);
        coloredBar = rootView.findViewById(R.id.explore_shout_colored_bar);
        coloredMetaDataContainer = rootView.findViewById(R.id.explore_shout_colored_meta_data_container);
        coloredButtonContainer = (LinearLayout) rootView.findViewById(R.id.explore_shout_colored_button_container);

        likeCountView.setText(Integer.toString(shout.likeCount));
        commentCountView.setText(Integer.toString(shout.commentCount));

        aq.id(imageView).image(shout.image + "--400", true, false, 0, 0, null, AQuery.FADE_IN);

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

        if (shout.id == SessionUtils.getCurrentUser(getActivity()).id) {
            coloredBar.setBackgroundColor(getResources().getColor(R.color.myShoutPurple));
            coloredButtonContainer.setBackgroundColor(getResources().getColor(R.color.myShoutPurple));
            ImageUtils.setBackground(getActivity(), coloredMetaDataContainer, R.drawable.my_shout_meta_info);
        } else if (shout.anonymous) {
            coloredBar.setBackgroundColor(getResources().getColor(R.color.anonymousGrey));
            coloredButtonContainer.setBackgroundColor(getResources().getColor(R.color.anonymousGrey));
            ImageUtils.setBackground(getActivity(), coloredMetaDataContainer, R.drawable.anonymous_shout_meta_info);
        } else {
            coloredBar.setBackgroundColor(getResources().getColor(R.color.publicYellow));
            coloredButtonContainer.setBackgroundColor(getResources().getColor(R.color.publicYellow));
            ImageUtils.setBackground(getActivity(), coloredMetaDataContainer, R.drawable.public_shout_meta_info);
        }

        if (shout.anonymous) {
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
            usernameView.setText(getResources().getString(R.string.anonymous_name));
        } else {
            usernameView.setText("@" + shout.username);
        }

        descriptionView.setText(shout.description);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ExploreActivity) getActivity()).startDisplayActivity(shout);
            }
        });

        return rootView;
    }
}