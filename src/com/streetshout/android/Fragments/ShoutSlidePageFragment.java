package com.streetshout.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;

/**
 * Created by bastien on 3/3/14.
 */
public class ShoutSlidePageFragment extends Fragment {

    private Shout shout = null;

    private AQuery aq = null;

    private ImageView imageView = null;

    private TextView ageView = null;

    private TextView usernameView = null;

    private ImageView userProfilePic = null;

    private LinearLayout userContainer = null;

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
        usernameView = (TextView) rootView.findViewById(R.id.explore_shout_username);
        userProfilePic = (ImageView) rootView.findViewById(R.id.explore_shout_user_picture);
        userContainer = (LinearLayout) rootView.findViewById(R.id.explore_shout_user_container);

        if (Constants.PRODUCTION) {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        } else {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        }

        if (!shout.anonymous) {
            aq.id(userProfilePic).image(GeneralUtils.getProfileThumbPicturePrefix() + shout.userId, true, false, 0, 0, null, AQuery.FADE_IN);

            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO REIMPLEMENT
                }
            });
        }

        String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));
        ageView.setText(ageStrings[0] + ageStrings[1]);

        if (shout.anonymous) {
            usernameView.setText(getResources().getString(R.string.anonymous_name));
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
        } else {
            usernameView.setText("@" + shout.username);
        }

        if (shout.trending) {
            rootView.findViewById(R.id.explore_shout_trending_mark).setVisibility(View.VISIBLE);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO REIMPLEMENT
            }
        });

        return rootView;
    }
}