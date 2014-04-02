package com.streetshout.android.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.models.UserActivity;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;

import java.util.ArrayList;

/**
 * Created by bastien on 4/2/14.
 */
public class ActivitiesAdapter extends BaseAdapter {

    private Activity activity = null;

    private ArrayList<UserActivity> items = null;

    public ActivitiesAdapter(Activity activity, ArrayList<UserActivity> userActivities) {
        this.activity = activity;
        this.items = userActivities;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout userActivityView;

        if (convertView != null) {
            userActivityView = (LinearLayout) convertView;
        } else {
            userActivityView = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.user_activity_feed_view, null);
        }

        final UserActivity userActivity = items.get(position);

        if (userActivity != null) {
            ImageView activityUserImage = (ImageView) userActivityView.findViewById(R.id.user_activity_feed_user_picture);
            ImageView activityShoutImage = (ImageView) userActivityView.findViewById(R.id.user_activity_feed_shout_picture);

            if (userActivity.redirectType.equals("User")) {
                activityShoutImage.setVisibility(View.GONE);
                activityUserImage.setVisibility(View.VISIBLE);
                GeneralUtils.getAquery(activity).id(activityUserImage).image(userActivity.image, true, false, 0, 0, null, AQuery.FADE_IN);
            } else {
                activityShoutImage.setVisibility(View.VISIBLE);
                activityUserImage.setVisibility(View.GONE);
                GeneralUtils.getAquery(activity).id(activityShoutImage).image(userActivity.image, true, false, 0, 0, null, AQuery.FADE_IN);
            }

            ((TextView) userActivityView.findViewById(R.id.user_activity_feed_message_textView)).setText(userActivity.message);

            String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(userActivity.created));

            String stamp = ageStrings[0] + ageStrings[1];

            ((TextView) userActivityView.findViewById(R.id.user_activity_feed_stamp_textView)).setText(stamp);

            userActivityView.findViewById(R.id.user_activity_feed_user_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO: redirect!
                }
            });
        }

        return userActivityView;
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
    public UserActivity getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
