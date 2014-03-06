package com.streetshout.android.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
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

    private View coloredTimeDistanceContainer = null;

    private LinearLayout coloredLikeCountButton = null;

    private LinearLayout coloredCommentCountButton = null;

    private ImageView likeButton = null;

    private ImageView shareButton = null;

    private ImageView zoomButton = null;

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
        coloredTimeDistanceContainer = rootView.findViewById(R.id.explore_shout_colored_time_distance_container);
        coloredLikeCountButton = (LinearLayout) rootView.findViewById(R.id.explore_shout_like_count_button);
        coloredCommentCountButton = (LinearLayout) rootView.findViewById(R.id.explore_shout_comment_count_button);
        likeButton = (ImageView) rootView.findViewById(R.id.explore_shout_like_button);
        shareButton = (ImageView) rootView.findViewById(R.id.explore_shout_share_button);
        zoomButton = (ImageView) rootView.findViewById(R.id.explore_shout_zoom_button);

        aq.id(imageView).image(shout.image + "--400", true, false, 0, 0, null, AQuery.FADE_IN);

        setLikeCountUI(shout.likeCount);
        setCommentCountUI(shout.commentCount);

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
            updateShoutUI(R.color.myShoutPurple);
        } else if (shout.anonymous) {
            updateShoutUI(R.color.anonymousGrey);
        } else {
            updateShoutUI(R.color.publicYellow);
        }

        if (shout.anonymous) {
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
            usernameView.setText(getResources().getString(R.string.anonymous_name));
        } else {
            usernameView.setText("@" + shout.username);
        }

        if (shout.trending) {
            rootView.findViewById(R.id.explore_shout_trending_mark).setVisibility(View.VISIBLE);
        }

        descriptionView.setText(shout.description);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ExploreActivity) getActivity()).startDisplayActivity(shout);
            }
        });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeButton.setEnabled(false);
                //TODO: Add to liked shouts

                setLikeCountUI(shout.likeCount + 1);
                likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));

                double lat = 0;
                double lng = 0;

                Location myLocation = ((ExploreActivity) getActivity()).myLocation;
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

                            //TODO: Remove from liked shouts
                            setLikeCountUI(shout.likeCount);
                            likeButton.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
                        }
                    }
                });
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = ApiUtils.getUserSiteUrl() + "/shouts/" + shout.id;
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_shout_text, url));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_shout_subject));
                sendIntent.setType("text/plain");

                startActivity(sendIntent);
            }
        });

        zoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ExploreActivity) getActivity()).redirectToShout(shout);
            }
        });

        coloredLikeCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent likes = new Intent(getActivity(), LikesActivity.class);
                likes.putExtra("shout", shout);

                Location myLocation = ((ExploreActivity) getActivity()).myLocation;
                if (myLocation != null  && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    likes.putExtra("myLocation", myLocation);
                }

                startActivity(likes);
            }
        });

        coloredCommentCountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent comments = new Intent(getActivity(), CommentsActivity.class);
                comments.putExtra("shout", shout);

                Location myLocation = ((ExploreActivity) getActivity()).myLocation;
                if (myLocation != null  && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    comments.putExtra("myLocation", myLocation);
                }

                startActivity(comments);
            }
        });

        return rootView;
    }

    private void updateShoutUI(int color) {
        switch(color){
            case R.color.myShoutPurple:
                ImageUtils.setBackground(getActivity(), coloredTimeDistanceContainer, R.drawable.my_shout_meta_info);
                ImageUtils.setBackground(getActivity(), coloredLikeCountButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.my_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.my_shout_count_button_selector);
                return;
            case R.color.anonymousGrey:
                ImageUtils.setBackground(getActivity(), coloredTimeDistanceContainer, R.drawable.anonymous_shout_meta_info);
                ImageUtils.setBackground(getActivity(), coloredLikeCountButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), likeButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), shareButton, R.drawable.anonymous_count_button_selector);
                ImageUtils.setBackground(getActivity(), zoomButton, R.drawable.anonymous_count_button_selector);
                return;
            case R.color.publicYellow:
                ImageUtils.setBackground(getActivity(), coloredTimeDistanceContainer, R.drawable.public_shout_meta_info);
                ImageUtils.setBackground(getActivity(), coloredLikeCountButton, R.drawable.public_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), coloredCommentCountButton, R.drawable.public_shout_count_button_selector);
                ImageUtils.setBackground(getActivity(), likeButton, R.drawable.public_shout_count_button_selector);
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