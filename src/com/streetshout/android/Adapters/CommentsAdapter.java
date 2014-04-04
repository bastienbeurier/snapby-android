package com.streetshout.android.adapters;

import android.app.Activity;
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
import com.streetshout.android.models.Comment;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class CommentsAdapter extends BaseAdapter{
    private Activity activity = null;

    private ArrayList<Comment> items = null;

    private Location shoutLocation = null;

    public CommentsAdapter(Activity activity, ArrayList<Comment> comments, Location shoutLocation) {
        this.activity = activity;
        this.items = comments;
        this.shoutLocation = shoutLocation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout commentView;

        if (convertView != null) {
            commentView = (LinearLayout) convertView;
        } else {
            commentView = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.comment_feed_view, null);
        }

        final Comment comment = items.get(position);

        if (comment != null) {
            ImageView userPicture = (ImageView) commentView.findViewById(R.id.like_feed_user_picture);
            GeneralUtils.getAquery(activity).id(userPicture).image(GeneralUtils.getProfileThumbPicturePrefix() + comment.commenterId, true, false, 0, 0, null, AQuery.FADE_IN);

            ((TextView) commentView.findViewById(R.id.comment_feed_username_textView)).setText("@" + comment.commenterUsername);

            ((TextView) commentView.findViewById(R.id.comment_feed_description_textView)).setText(comment.description);

            String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(comment.created));

            String stamp = ageStrings[0] + ageStrings[1];

            if (comment.lat != 0 && comment.lng != 0) {
                Location commentLocation = new Location("");
                commentLocation.setLatitude(comment.lat);
                commentLocation.setLongitude(comment.lng);

                String[] distanceStrings = LocationUtils.formattedDistanceStrings(activity, commentLocation, shoutLocation);
                stamp += " | " + distanceStrings[0] + distanceStrings[1];
            }

            ((TextView) commentView.findViewById(R.id.comment_feed_stamp_textView)).setText(stamp);

            commentView.findViewById(R.id.comment_feed_user_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent profile = new Intent(activity, ProfileActivity.class);
                    profile.putExtra("userId", comment.commenterId);
                    activity.startActivityForResult(profile, Constants.PROFILE_REQUEST);
                }
            });
        }

        return commentView;
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
    public Comment getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
