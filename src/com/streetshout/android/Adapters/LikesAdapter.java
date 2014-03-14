package com.streetshout.android.adapters;

import android.app.Activity;
import android.content.Context;
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
import com.streetshout.android.R;
import com.streetshout.android.activities.ProfileActivity;
import com.streetshout.android.models.Like;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class LikesAdapter extends BaseAdapter{
    private Activity activity = null;

    private ArrayList<Like> items = null;

    private Location shoutLocation = null;

    public LikesAdapter(Activity activity, ArrayList<Like> likes, Location shoutLocation) {
        this.activity = activity;
        this.items = likes;
        this.shoutLocation = shoutLocation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout likeView;

        if (convertView != null) {
            likeView = (LinearLayout) convertView;
        } else {
            likeView = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.like_feed_view, null);
        }

        final Like like = items.get(position);

        if (like != null) {
            ImageView userPicture = (ImageView) likeView.findViewById(R.id.like_feed_user_picture);
            GeneralUtils.getAquery(activity).id(userPicture).image(Constants.PROFILE_PICS_URL_PREFIX + like.likerId, true, false, 0, 0, null, AQuery.FADE_IN);

            ((TextView) likeView.findViewById(R.id.like_feed_username_textView)).setText("@" + like.likerUsername);

            String stamp = "";

            if (like.lat != 0 && like.lng != 0) {
                Location likeLocation = new Location("");
                likeLocation.setLatitude(like.lat);
                likeLocation.setLongitude(like.lng);

                String[] distanceStrings = LocationUtils.formattedDistanceStrings(activity, likeLocation, shoutLocation);
                stamp += distanceStrings[0] + distanceStrings[1] + " away";
            }

            ((TextView) likeView.findViewById(R.id.like_feed_stamp_textView)).setText(stamp);

            likeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent profile = new Intent(activity, ProfileActivity.class);
                    profile.putExtra("userId", like.likerId);
                    activity.startActivityForResult(profile, Constants.PROFILE_REQUEST);
                }
            });
        }

        return likeView;
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
    public Like getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
