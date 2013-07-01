package com.streetshout.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

public class NewShoutFeedAdapter extends BaseAdapter {
    private Context context = null;

    private ArrayList<ShoutModel> items = null;

    private Location myLocation = null;

    public NewShoutFeedAdapter(Context context, ArrayList<ShoutModel> shouts, Location myLocation) {
        this.context = context;
        this.items = shouts;
        this.myLocation = myLocation;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout shoutView;

        if (convertView != null) {
            shoutView = (LinearLayout) convertView;
        } else {
            shoutView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.feed_shout_view, null);
        }

        final ShoutModel shout = items.get(position);

        if (shout != null) {
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_body)).setText('"' + shout.description + '"');

            String shoutStamp = TimeUtils.shoutAgeToString((Activity) context, TimeUtils.getShoutAge(shout.created));

            if (myLocation != null) {
                Location shoutLocation = new Location("");
                shoutLocation.setLatitude(shout.lat);
                shoutLocation.setLongitude(shout.lng);
                shoutStamp += ", " + LocationUtils.formatedDistance(context, myLocation, shoutLocation) + ",";
            }

            shoutStamp += " by " + shout.displayName;
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_stamp)).setText(shoutStamp);

            if (shout.image != null && shout.image.length() > 0) {
                Log.d("BAB", "VISIBLE: " + shout.image);
                shoutView.findViewById(R.id.photo_presence_indicator).setVisibility(View.VISIBLE);
            } else {
                Log.d("BAB", "GONE: " + shout.image);
                shoutView.findViewById(R.id.photo_presence_indicator).setVisibility(View.GONE);
            }
        }

        return shoutView;
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
    public ShoutModel getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
