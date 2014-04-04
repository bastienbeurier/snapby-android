package com.streetshout.android.adapters;

import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.activities.FollowerActivity;
import com.streetshout.android.activities.ProfileActivity;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 3/14/14.
 */
public class FollowerAdapter  extends BaseAdapter {
    private FollowerActivity activity = null;

    private ArrayList<User> items = null;

    private Location myLocation = null;

    public FollowerAdapter(FollowerActivity activity, ArrayList<User> users, Location myLocation) {
        this.activity = activity;
        this.items = users;
        this.myLocation = myLocation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout followerView;

        if (convertView != null) {
            followerView = (LinearLayout) convertView;
        } else {
            followerView = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.follower_feed_view, null);
        }

        final User user = items.get(position);

        if (user != null) {
            ImageView userPicture = (ImageView) followerView.findViewById(R.id.follower_feed_user_picture);
            GeneralUtils.getAquery(activity).id(userPicture).image(GeneralUtils.getProfileThumbPicturePrefix() + user.id, true, false, 0, 0, null, AQuery.FADE_IN);

            ((TextView) followerView.findViewById(R.id.follower_feed_username_textView)).setText("@" + user.username);

            final TextView followLabel = (TextView) followerView.findViewById(R.id.follower_feed_follow_label);

            final View followButton = followerView.findViewById(R.id.follower_feed_follow_button);

            if (user.id == SessionUtils.getCurrentUser(activity).id) {
                followButton.setVisibility(View.GONE);
            } else {
                updateUI(activity.followedByMe(user.id), followLabel, followButton);

                followButton.setVisibility(View.VISIBLE);

                followerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFollow(user.id, followLabel, followButton);
                    }
                });
            }

            if (user.shoutCount > 1) {
                ((TextView) followerView.findViewById(R.id.follower_feed_shout_count_textView)).setText(user.shoutCount + " " + activity.getResources().getString(R.string.shouts_so_far));
            } else {
                ((TextView) followerView.findViewById(R.id.follower_feed_shout_count_textView)).setText(user.shoutCount + " " + activity.getResources().getString(R.string.shout_so_far));
            }

            String stamp = "";

            TextView distanceView = (TextView) followerView.findViewById(R.id.follower_feed_stamp_textView);

            if (user.lat != 0 && user.lng != 0 && myLocation != null) {
                Location followerLocation = new Location("");
                followerLocation.setLatitude(user.lat);
                followerLocation.setLongitude(user.lng);

                String[] distanceStrings = LocationUtils.formattedDistanceStrings(activity, myLocation, followerLocation);
                stamp += distanceStrings[0] + distanceStrings[1] + " " + activity.getResources().getString(R.string.away);

                distanceView.setText(stamp);
                distanceView.setVisibility(View.VISIBLE);
            } else {
                distanceView.setVisibility(View.GONE);
            }

            View.OnClickListener profileClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent profile = new Intent(activity, ProfileActivity.class);
                    profile.putExtra("userId", user.id);
                    activity.startActivityForResult(profile, Constants.PROFILE_REQUEST);
                }
            };

            followerView.findViewById(R.id.follower_feed_username_container).setOnClickListener(profileClick);
            userPicture.setOnClickListener(profileClick);
        }

        return followerView;
    }

    private void toggleFollow(final int userId, final TextView followLabel, final View followButton) {
        if (activity.followedByMe(userId)) {
            ApiUtils.unfollowUser(activity, userId, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getError() != null) {
                        updateUI(true, followLabel, followButton);
                        activity.addToFollowedByMe(userId);
                    }
                }
            });

            updateUI(false, followLabel, followButton);
            activity.removeFromFollowedByMe(userId);
        } else {
            ApiUtils.followUser(activity, userId, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getError() != null) {
                        updateUI(false, followLabel, followButton);
                        activity.removeFromFollowedByMe(userId);
                    }
                }
            });

            updateUI(true, followLabel, followButton);
            activity.addToFollowedByMe(userId);
        }
    }

    private void updateUI(boolean followedByMe, TextView followLabel, View followButton) {
        if (followedByMe) {
            followLabel.setText(activity.getResources().getString(R.string.following_cap));
            ImageUtils.setBackground(activity, followButton, R.drawable.following_button);
        } else {
            followLabel.setText(activity.getResources().getString(R.string.follow_cap));
            ImageUtils.setBackground(activity, followButton, R.drawable.follow_button);
        }
    }

    @Override
    public int getCount() {
        if (items != null)
            return items.size();
        else
            return 0;
    }

    @Override
    public boolean isEmpty() {
        if (this.getCount() == 0)
            return true;

        return false;
    }

    @Override
    public User getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
