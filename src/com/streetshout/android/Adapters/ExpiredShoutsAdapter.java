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
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 4/4/14.
 */
public class ExpiredShoutsAdapter extends BaseAdapter {

    private ProfileActivity activity = null;

    private ArrayList<Shout> items = null;

    public ExpiredShoutsAdapter(ProfileActivity activity, ArrayList<Shout> expiredShouts) {
        this.activity = activity;
        this.items = expiredShouts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout expiredShoutView;

        if (convertView != null) {
            expiredShoutView = (LinearLayout) convertView;
        } else {
            expiredShoutView = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.expired_shout_feed_view, null);
        }

        final Shout shout = items.get(position);

        if (shout != null) {
            ImageView shoutImage = (ImageView) expiredShoutView.findViewById(R.id.expired_shout_feed_shout_picture);

            GeneralUtils.getAquery(activity).id(shoutImage).image(GeneralUtils.getShoutSmallPicturePrefix() + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);

            ((TextView) expiredShoutView.findViewById(R.id.expired_shout_feed_message_textView)).setText(shout.description);

            String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));

            String stamp = ageStrings[0] + ageStrings[1];

            if (shout.likeCount < 2) {
                stamp += " (" + shout.likeCount + " like)";
            } else {
                stamp += " (" + shout.likeCount + " likes)";
            }

            ((TextView) expiredShoutView.findViewById(R.id.expired_shout_feed_stamp_textView)).setText(stamp);

            expiredShoutView.findViewById(R.id.expired_shout_feed_user_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (shout.expired) {
                        Intent displayShout = new Intent(activity, DisplayActivity.class);
                        displayShout.putExtra("shout", shout);
                        displayShout.putExtra("expiredShout", true);

                        activity.startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
                    } else {
                        activity.progressDialog = ProgressDialog.show(activity, "", activity.getString(R.string.loading), false);

                        ApiUtils.getShout(GeneralUtils.getAquery(activity), shout.id, new AjaxCallback<JSONObject>() {
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

                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("notificationShout", shout);
                                    activity.setResult(Activity.RESULT_OK, returnIntent);
                                    activity.finish();
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

        return expiredShoutView;
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
