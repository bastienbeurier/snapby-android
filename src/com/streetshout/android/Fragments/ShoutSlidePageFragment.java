package com.streetshout.android.fragments;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.streetshout.android.activities.CommentsActivity;
import com.streetshout.android.activities.ExploreActivity;
import com.streetshout.android.activities.LikesActivity;
import com.streetshout.android.activities.ProfileActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.SessionUtils;
import com.streetshout.android.utils.TimeUtils;
import org.json.JSONObject;

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

    private LinearLayout coloredLikeCountButton = null;

    private LinearLayout coloredCommentCountButton = null;

    private View coloredBar = null;

    private ImageView likeButton = null;

    private ImageView commentButton = null;

    private ImageView shareButton = null;

    private ImageView zoomButton = null;

    private ImageView userProfilePic = null;

    private LinearLayout userContainer = null;

    private boolean liked = false;

    private ExploreActivity activity = null;

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
        coloredLikeCountButton = (LinearLayout) rootView.findViewById(R.id.explore_shout_like_count_button);
        coloredCommentCountButton = (LinearLayout) rootView.findViewById(R.id.explore_shout_comment_count_button);
        likeButton = (ImageView) rootView.findViewById(R.id.explore_shout_like_button);
        commentButton = (ImageView) rootView.findViewById(R.id.explore_shout_comment_button);
        shareButton = (ImageView) rootView.findViewById(R.id.explore_shout_share_button);
        zoomButton = (ImageView) rootView.findViewById(R.id.explore_shout_zoom_button);
        coloredBar = rootView.findViewById(R.id.explore_shout_colored_bar);
        userProfilePic = (ImageView) rootView.findViewById(R.id.explore_shout_user_picture);
        userContainer = (LinearLayout) rootView.findViewById(R.id.explore_shout_user_container);

        activity = (ExploreActivity) getActivity();

        if (Constants.PRODUCTION) {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        } else {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        }

        if (!shout.anonymous) {
            aq.id(userProfilePic).image(Constants.PROFILE_PICS_URL_PREFIX + shout.userId, true, false, 0, 0, null, AQuery.FADE_IN);

            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent profile = new Intent(activity, ProfileActivity.class);
                    profile.putExtra("userId", shout.userId);
                    startActivityForResult(profile, Constants.PROFILE_REQUEST);
                }
            });
        }

        setLikeCountUI(shout.likeCount);
        setCommentCountUI(shout.commentCount);

        if (activity.currentUserShoutLiked.contains(shout.id)) {
            liked = true;
            likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        } else {
            liked = false;
            likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        }

        String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));
        ageView.setText(ageStrings[0] + ageStrings[1]);

        Location myLocation = activity.myLocation;
        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);

            String[] distanceStrings = LocationUtils.formattedDistanceStrings(getActivity(), myLocation, shoutLocation);
            distanceView.setText(distanceStrings[0] + distanceStrings[1]);
        } else {
            distanceView.setText("?");
        }

        if (shout.userId == SessionUtils.getCurrentUser(getActivity()).id) {
            updateShoutUI(R.color.myShoutPurple);
        } else if (shout.anonymous) {
            updateShoutUI(R.color.anonymousGrey);
        } else {
            updateShoutUI(R.color.publicYellow);
        }

        if (shout.anonymous) {
            usernameView.setText(getResources().getString(R.string.anonymous_name));
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
        } else {
            usernameView.setText("@" + shout.username);
        }

        if (shout.trending) {
            rootView.findViewById(R.id.explore_shout_trending_mark).setVisibility(View.VISIBLE);
        }

        if (shout.description != null && shout.description.length() > 0) {
            descriptionView.setText(shout.description);
            descriptionView.setVisibility(View.VISIBLE);
        } else {
            descriptionView.setVisibility(View.GONE);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startDisplayActivity(shout);
            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeButton.setEnabled(false);

                if (liked) {
                    unlikeShout();

                    ApiUtils.removeLike(getActivity(), shout.id, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            likeButton.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(getActivity(), getString(R.string.shout_unlike_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                likeShout();
                            }
                        }
                    });
                } else {
                    likeShout();

                    double lat = 0;
                    double lng = 0;

                    Location myLocation = activity.myLocation;
                    if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        lat = myLocation.getLatitude();
                        lng = myLocation.getLongitude();
                    }

                    ApiUtils.createLike(getActivity(), shout, lat, lng, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            likeButton.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(getActivity(), getString(R.string.shout_like_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                unlikeShout();
                            }
                        }
                    });
                }
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeneralUtils.shareShout(getActivity(), shout);
            }
        });

        zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.redirectToShout(shout);
            }
        });

        coloredLikeCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent likes = new Intent(getActivity(), LikesActivity.class);
                likes.putExtra("shout", shout);

                Location myLocation = activity.myLocation;
                if (myLocation != null  && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    likes.putExtra("myLocation", myLocation);
                }

                startActivity(likes);
            }
        });

        View.OnClickListener commentListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent comments = new Intent(getActivity(), CommentsActivity.class);
                comments.putExtra("shout", shout);

                Location myLocation = activity.myLocation;
                if (myLocation != null  && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    comments.putExtra("myLocation", myLocation);
                }

                startActivity(comments);
            }
        };

        coloredCommentCountButton.setOnClickListener(commentListener);
        commentButton.setOnClickListener(commentListener);

        return rootView;
    }

    private void likeShout() {
        liked = true;
        likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        activity.currentUserShoutLiked.add(shout.id);

        shout.likeCount++;
        setLikeCountUI(shout.likeCount);
    }

    private void unlikeShout() {
        liked = false;
        likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        activity.currentUserShoutLiked.remove(shout.id);

        shout.likeCount--;
        setLikeCountUI(shout.likeCount);
    }

    private void updateShoutUI(int color) {
        coloredBar.setBackgroundColor(getResources().getColor(color));

        switch(color){
            case R.color.myShoutPurple:
                ImageUtils.setBackground(getActivity(), likeButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), commentButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), shareButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), zoomButton, R.drawable.my_shout_count_button_selector);
                return;
            case R.color.anonymousGrey:
                ImageUtils.setBackground(getActivity(), likeButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), commentButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), shareButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), zoomButton, R.drawable.anonymous_count_button_selector);
                return;
            case R.color.publicYellow:
                ImageUtils.setBackground(getActivity(), likeButton, R.drawable.public_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), commentButton, R.drawable.public_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), shareButton, R.drawable.public_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), zoomButton, R.drawable.public_shout_count_button_selector);
                return;
        }
    }

    private void setLikeCountUI(int count) {
        if (count > 1) {
            likeCountView.setText(Integer.toString(count) + " " + getString(R.string.likes));
        } else {
            likeCountView.setText(Integer.toString(count) + " " + getString(R.string.like));
        }
    }

    private void setCommentCountUI(int count) {
        if (count > 1) {
            commentCountView.setText(Integer.toString(count) + " " + getString(R.string.comments));
        } else {
            commentCountView.setText(Integer.toString(count) + " " + getString(R.string.comment));
        }
    }
}