package com.streetshout.android.adapters;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.streetshout.android.R;
import com.streetshout.android.models.Like;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class LikesAdapter extends BaseAdapter{
    private Context context = null;

    private ArrayList<Like> items = null;

    private Location shoutLocation = null;

    public LikesAdapter(Context context, ArrayList<Like> likes, Location shoutLocation) {
        this.context = context;
        this.items = likes;
        this.shoutLocation = shoutLocation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout commentView;

        if (convertView != null) {
            commentView = (LinearLayout) convertView;
        } else {
            commentView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.like_feed_view, null);
        }

        final Like like = items.get(position);

        if (like != null) {
            ((TextView) commentView.findViewById(R.id.like_feed_username_textView)).setText("@" + like.likerUsername);

            String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(like.created));

            String stamp = ageStrings[0] + ageStrings[1];

            if (like.lat != 0 && like.lng != 0) {
                Location commentLocation = new Location("");
                commentLocation.setLatitude(like.lat);
                commentLocation.setLongitude(like.lng);

                String[] distanceStrings = LocationUtils.formattedDistanceStrings(context, commentLocation, shoutLocation);
                stamp += " | " + distanceStrings[0] + distanceStrings[1];
            }

            ((TextView) commentView.findViewById(R.id.like_feed_stamp_textView)).setText(stamp);
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
    public Like getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
