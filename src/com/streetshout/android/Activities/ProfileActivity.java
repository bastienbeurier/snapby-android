package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.ImageOptions;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.TreeSet;

/**
 * Created by bastien on 3/10/14.
 */
public class ProfileActivity extends Activity {

    private User currentUser = null;

    private ImageView profilePicture = null;
    private TextView username = null;
    private TextView followerCount = null;
    private TextView followingCount = null;
    private LinearLayout followersButton = null;
    private LinearLayout followingButton = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicture = (ImageView) findViewById(R.id.profile_user_picture);
        username = (TextView) findViewById(R.id.profile_username);
        followerCount = (TextView) findViewById(R.id.profile_follower_count);
        followingCount = (TextView) findViewById(R.id.profile_following_count);
        followersButton = (LinearLayout) findViewById(R.id.profile_followers_button);
        followingButton = (LinearLayout) findViewById(R.id.profile_following_button);



        ApiUtils.updateUserInfoWithLocation(this, GeneralUtils.getAquery(this), null, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null) {
                    JSONObject result = null;
                    JSONObject rawUser = null;

                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    currentUser = User.rawUserToInstance(rawUser);

                    ImageOptions options = new ImageOptions();
                    options.round = 4;
                    options.memCache = true;
                    options.animation = AQuery.FADE_IN;

                    GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(currentUser.profilePicture, options);

                    username.setText("@" + currentUser.username);
                }
            }
        });

        findViewById(R.id.profile_find_follow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        followersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        followingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}