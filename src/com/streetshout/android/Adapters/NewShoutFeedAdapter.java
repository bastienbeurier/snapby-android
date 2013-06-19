package com.streetshout.android.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.Utils.TimeUtils;

import java.util.ArrayList;

public class NewShoutFeedAdapter extends BaseAdapter {
    private Context context = null;

    private ArrayList<ShoutModel> items = null;

    public NewShoutFeedAdapter(Context context, ArrayList<ShoutModel> shouts) {
        this.context = context;
        this.items = shouts;
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
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_body)).setText(shout.description);

            String shoutStamp = TimeUtils.shoutAgeToString((Activity) context, TimeUtils.getShoutAge(shout.created));
            shoutStamp += " by " + shout.displayName;
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_stamp)).setText(shoutStamp);
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
