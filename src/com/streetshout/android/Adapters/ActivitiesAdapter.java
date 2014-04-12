package com.streetshout.android.adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.activities.DisplayActivity;
import com.streetshout.android.activities.ProfileActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.models.UserActivity;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 4/2/14.
 */
public class ActivitiesAdapter extends BaseAdapter {

    private ProfileActivity activity = null;

    public ArrayList<UserActivity> items = null;

    public ActivitiesAdapter(ProfileActivity activity, ArrayList<UserActivity> userActivities) {
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
            View activityUserImagePlaceHolder = userActivityView.findViewById(R.id.user_activity_feed_user_picture_place_holder);
            ImageView activityUserImage = (ImageView) userActivityView.findViewById(R.id.user_activity_feed_user_picture);
            ImageView activityShoutImage = (ImageView) userActivityView.findViewById(R.id.user_activity_feed_shout_picture);

            if (userActivity.redirectType.equals("User")) {
                activityUserImagePlaceHolder.setVisibility(View.VISIBLE);
                activityShoutImage.setVisibility(View.GONE);
                activityUserImage.setVisibility(View.VISIBLE);
                GeneralUtils.getAquery(activity).id(activityUserImage).image(userActivity.image, true, false, 0, 0, null, AQuery.FADE_IN);
            } else if (userActivity.redirectType.equals("Shout")) {
                activityUserImagePlaceHolder.setVisibility(View.GONE);
                activityShoutImage.setVisibility(View.VISIBLE);
                activityUserImage.setVisibility(View.GONE);
                GeneralUtils.getAquery(activity).id(activityShoutImage).image(userActivity.image, true, false, 0, 0, null, AQuery.FADE_IN);
            } else if (userActivity.redirectType.equals("Welcome")) {
                activityUserImagePlaceHolder.setVisibility(View.GONE);
                activityShoutImage.setVisibility(View.VISIBLE);
                activityUserImage.setVisibility(View.GONE);
                GeneralUtils.getAquery(activity).id(activityShoutImage).image(userActivity.image, true, false, 0, 0, null, AQuery.FADE_IN);
            } else {
                activityUserImage.setVisibility(View.GONE);
                activityShoutImage.setVisibility(View.GONE);
                activityUserImagePlaceHolder.setVisibility(View.GONE);
            }

            ((TextView) userActivityView.findViewById(R.id.user_activity_feed_message_textView)).setText(userActivity.message);

            String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(userActivity.created));

            String stamp = ageStrings[0] + ageStrings[1];

            ((TextView) userActivityView.findViewById(R.id.user_activity_feed_stamp_textView)).setText(stamp);

            userActivityView.findViewById(R.id.user_activity_feed_user_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userActivity.redirectType.equals("User")) {
                        Intent profile = new Intent(activity, ProfileActivity.class);
                        profile.putExtra("userId", userActivity.redirectId);
                        activity.startActivityForResult(profile, Constants.PROFILE_REQUEST);
                    } else if (userActivity.redirectType.equals("Shout")) {
                        activity.progressDialog = ProgressDialog.show(activity, "", activity.getString(R.string.loading), false);

                        ApiUtils.getShout(GeneralUtils.getAquery(activity), userActivity.redirectId, new AjaxCallback<JSONObject>() {
                            @Override
                            public void callback(String url, JSONObject object, AjaxStatus status) {
                                super.callback(url, object, status);

                                if (status.getError() == null && object != null) {

                                    Shout shout = null;

                                    try {
                                        JSONObject result = object.getJSONObject("result");
                                        JSONObject rawShout = result.getJSONObject("shout");
                                        shout = Shout.rawShoutToInstance(rawShout);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    Intent displayShout = new Intent(activity, DisplayActivity.class);
                                    displayShout.putExtra("shout", shout);
                                    displayShout.putExtra("expiredShout", true);

                                    activity.startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
                                } else {
                                    Toast toast = Toast.makeText(activity, activity.getString(R.string.failed_to_retrieve_shout), Toast.LENGTH_SHORT);
                                    toast.show();
                                }

                                activity.progressDialog.cancel();
                            }
                        });
                    }
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
