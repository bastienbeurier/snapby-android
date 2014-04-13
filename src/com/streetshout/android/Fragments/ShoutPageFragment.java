package com.streetshout.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.activities.MainActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;
import org.json.JSONObject;

/**
 * Created by bastien on 3/3/14.
 */
public class ShoutPageFragment extends Fragment {

    private Shout shout = null;

    private AQuery aq = null;

    private ImageView imageView = null;

    private TextView ageView = null;

    private TextView usernameView = null;

    private ImageView userProfilePic = null;

    private LinearLayout userContainer = null;

    private View likeContainer = null;

    private ImageView likeIcon = null;

    private TextView likeCount = null;

    private View imageContainer = null;

    private boolean liked = false;

    private MainActivity mainActivity = null;

    private String type = null;

    public static ShoutPageFragment newInstance(Shout shout) {
        ShoutPageFragment shoutPageFragment = new ShoutPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("shout", shout);
        shoutPageFragment.setArguments(args);

        return shoutPageFragment;
    }

    public static ShoutPageFragment newInstance(Shout shout, String type) {
        ShoutPageFragment shoutPageFragment = new ShoutPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("shout", shout);
        args.putString("type", type);
        shoutPageFragment.setArguments(args);

        return shoutPageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        aq = GeneralUtils.getAquery(mainActivity);

        if ((getArguments().getString("type") != null && getArguments().getString("type").equals("profile"))) {
            type = "profile";
        } else {
            type = "explore";
        }

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.shout_page_fragment, container, false);
        shout = getArguments().getParcelable("shout");

        imageView = (ImageView) rootView.findViewById(R.id.explore_shout_image);
        ageView = (TextView) rootView.findViewById(R.id.explore_shout_age);
        usernameView = (TextView) rootView.findViewById(R.id.explore_shout_username);
        userProfilePic = (ImageView) rootView.findViewById(R.id.explore_shout_user_picture);
        userContainer = (LinearLayout) rootView.findViewById(R.id.explore_shout_user_container);
        imageContainer = rootView.findViewById(R.id.shout_image_view_container);
        likeContainer = rootView.findViewById(R.id.explore_shout_like_container);
        likeIcon = (ImageView) rootView.findViewById(R.id.explore_shout_like_button);
        likeCount = (TextView) rootView.findViewById(R.id.explore_shout_like_count);

        if (Constants.PRODUCTION) {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        } else {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        }

        if (shout.anonymous || type.equals("profile")) {
            userContainer.setVisibility(View.GONE);
        } else {
            userContainer.setVisibility(View.VISIBLE);
            imageContainer.setVisibility(View.VISIBLE);
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

        setLikeCountUI(shout.likeCount);

        if (mainActivity.myLikes.contains(shout.id)) {
            liked = true;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        } else {
            liked = false;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        }

        likeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeContainer.setEnabled(false);

                if (liked) {
                    unlikeShout();

                    ApiUtils.removeLike(mainActivity, shout.id, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            likeContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.shout_unlike_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                likeShout();
                            }
                        }
                    });
                } else {
                    likeShout();

                    ApiUtils.createLike(mainActivity, shout, 0, 0, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            likeContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.shout_like_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                unlikeShout();
                            }
                        }
                    });
                }
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO REIMPLEMENT
            }
        });

        return rootView;
    }

    private void likeShout() {
        liked = true;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        mainActivity.myLikes.add(shout.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreShouts();
        } else {
            mainActivity.reloadProfileShouts();
        }

        shout.likeCount++;
        setLikeCountUI(shout.likeCount);
    }

    private void unlikeShout() {
        liked = false;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        mainActivity.myLikes.remove(shout.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreShouts();
        } else {
            mainActivity.reloadProfileShouts();
        }

        shout.likeCount--;
        setLikeCountUI(shout.likeCount);
    }

    private void setLikeCountUI(int count) {
        if (count > 1) {
            likeCount.setText(Integer.toString(count));
        } else {
            likeCount.setText(Integer.toString(count));
        }
    }
}