package com.snapby.android.adapters;

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
import com.snapby.android.R;
import com.snapby.android.models.Comment;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.LocationUtils;
import com.snapby.android.utils.SessionUtils;
import com.snapby.android.utils.TimeUtils;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class CommentsAdapter extends BaseAdapter{
    private Activity activity = null;

    private ArrayList<Comment> items = null;

    private Location snapbyLocation = null;

    private boolean anonymous = false;

    public CommentsAdapter(Activity activity, ArrayList<Comment> comments, Location snapbyLocation, boolean anonymous) {
        this.activity = activity;
        this.items = comments;
        this.snapbyLocation = snapbyLocation;
        this.anonymous = anonymous;
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

            TextView username = ((TextView) commentView.findViewById(R.id.comment_feed_username_textView));
            username.setText(comment.commenterUsername + " (" + comment.commenterScore + ")");

            if (SessionUtils.getCurrentUser(activity).id == comment.snapbyerId) {
                username.setTextColor(activity.getResources().getColor(R.color.snapbyPink));

                if (anonymous) {
                    username.setText(activity.getString(R.string.anonymous_name));
                    userPicture.setVisibility(View.GONE);
                } else {
                    userPicture.setVisibility(View.VISIBLE);
                }
            } else {
                username.setTextColor(activity.getResources().getColor(R.color.darkGrey));
            }


            ((TextView) commentView.findViewById(R.id.comment_feed_description_textView)).setText(comment.description);

            String[] ageStrings = TimeUtils.snapbyAgeToShortStrings(TimeUtils.getSnapbyAge(comment.created));

            String stamp = ageStrings[0] + ageStrings[1];

            if (comment.lat != 0 && comment.lng != 0) {
                Location commentLocation = new Location("");
                commentLocation.setLatitude(comment.lat);
                commentLocation.setLongitude(comment.lng);

                String[] distanceStrings = LocationUtils.formattedDistanceStrings(activity, commentLocation, snapbyLocation);
                stamp += " | " + distanceStrings[0] + distanceStrings[1];
            }

            ((TextView) commentView.findViewById(R.id.comment_feed_stamp_textView)).setText(stamp);
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
