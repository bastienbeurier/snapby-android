package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
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
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 3/10/14.
 */
public class ProfileActivity extends Activity {

    private User user = null;
    private int userId = 0;

    private ImageView profilePicture = null;
    private TextView username = null;
    private TextView followerCount = null;
    private TextView followingCount = null;
    private LinearLayout followersButton = null;
    private LinearLayout followingButton = null;
    private FrameLayout profilePictureContainer = null;

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
        profilePictureContainer = (FrameLayout) findViewById(R.id.profile_profile_picture_container);

        if (!getIntent().hasExtra("userId")) {
            user = SessionUtils.getCurrentUser(this);
            userId = user.id;
            updateUI();
        } else {
            userId = getIntent().getIntExtra("userId", 0);

            ApiUtils.getUserInfo(this, userId, new AjaxCallback<JSONObject>() {
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

                        user = User.rawUserToInstance(rawUser);
                        updateUI();
                    }
                }
            });
        }

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

        profilePictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO let user choose image if no image
            }
        });
    }

    private void updateUI() {
        ImageOptions options = new ImageOptions();
        options.round = 4;
        options.memCache = true;
        options.animation = AQuery.FADE_IN;

        GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(user.profilePicture, options);

        username.setText("@" + user.username);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Only display settings button if my profile
        if (userId == SessionUtils.getCurrentUser(this).id) {
            // Inflate the menu items for use in the action bar
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.my_profile_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_settings) {
            Intent settings = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivityForResult(settings, Constants.SETTINGS_REQUEST);
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.SETTINGS_REQUEST) {
            if (data.hasExtra("profileUpdated")) {
                user = SessionUtils.getCurrentUser(this);
                updateUI();
            }
        }
    }
}