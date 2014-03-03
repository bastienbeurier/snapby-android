package com.streetshout.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.streetshout.android.custom.CircleImageView;
import com.streetshout.android.models.Shout;
import com.streetshout.android.R;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

public class NewShoutFeedAdapter extends BaseAdapter {
    private Context context = null;

    private ArrayList<Shout> items = null;

    public NewShoutFeedAdapter(Context context, ArrayList<Shout> shouts) {
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

        final Shout shout = items.get(position);

        if (shout != null) {
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_body)).setText(shout.description);
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_username)).setText("by " + shout.username);

            String[] ageStrings = TimeUtils.shoutAgeToStrings((Activity) context, TimeUtils.getShoutAge(shout.created));

            ((TextView) shoutView.findViewById(R.id.feed_shout_age)).setText(ageStrings[0]);
            ((TextView) shoutView.findViewById(R.id.feed_shout_age_unit)).setText(ageStrings[1]);

            CircleImageView shoutImageView = (CircleImageView) shoutView.findViewById(R.id.feed_shout_image);
            ImageView shoutImageViewPlaceHolder = (ImageView) shoutView.findViewById(R.id.feed_shout_image_place_holder);

            if (shout.image != null && shout.image.length() > 0) {
                shoutImageViewPlaceHolder.setVisibility(View.VISIBLE);
                GeneralUtils.getAquery(context).id(shoutImageView).image(shout.image + "--400");
                shoutImageView.setVisibility(View.VISIBLE);
            } else {
                shoutImageView.setVisibility(View.GONE);
                shoutImageViewPlaceHolder.setVisibility(View.GONE);
            }

            shoutView.findViewById(R.id.feed_shout_age_container).setBackgroundColor(GeneralUtils.getShoutAgeColor(context, shout));
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
    public Shout getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
