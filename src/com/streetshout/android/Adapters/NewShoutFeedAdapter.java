package com.streetshout.android.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.custom.CircleImageView;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

public class NewShoutFeedAdapter extends BaseAdapter {
    private Context context = null;

    private ArrayList<ShoutModel> items = null;

    private AQuery feedFragmentAQuery = null;


    public NewShoutFeedAdapter(Context context, ArrayList<ShoutModel> shouts) {
        this.context = context;
        this.items = shouts;
        feedFragmentAQuery = new AQuery(context);
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
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_username)).setText("by " + shout.displayName);

            String[] ageStrings = TimeUtils.shoutAgeToStrings((Activity) context, TimeUtils.getShoutAge(shout.created));

            ((TextView) shoutView.findViewById(R.id.feed_shout_age)).setText(ageStrings[0]);
            ((TextView) shoutView.findViewById(R.id.feed_shout_age_unit)).setText(ageStrings[1]);

            CircleImageView shoutImageView = (CircleImageView) shoutView.findViewById(R.id.feed_shout_image);
            if (shout.image != null && shout.image.length() > 0) {
                feedFragmentAQuery.id(shoutImageView).image(R.drawable.image_shout_place_holder);
                feedFragmentAQuery.id(shoutImageView).image(shout.image + "--400");
            } else {
                shoutImageView.setVisibility(View.GONE);
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
    public ShoutModel getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
