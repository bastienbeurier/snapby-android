package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.adapters.ActivitiesAdapter;
import com.streetshout.android.adapters.ExpiredShoutsAdapter;
import com.streetshout.android.custom.EndlessListView;
import com.streetshout.android.models.Shout;
import com.streetshout.android.models.User;
import com.streetshout.android.models.UserActivity;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 3/10/14.
 */
public class ProfileActivity extends Activity {

    private User user = null;
    private int userId = 0;

    private ImageView profilePicture = null;
    private TextView username = null;
    private FrameLayout profilePictureContainer = null;
    private TextView shoutCountView = null;
    private boolean imageLoaded = false;
    public ProgressDialog progressDialog = null;

    private boolean myProfileOptionsEnabled = false;

    private ActivitiesAdapter activitiesAdapter = null;
    private ExpiredShoutsAdapter expiredShoutsAdapter = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicture = (ImageView) findViewById(R.id.profile_user_picture);
        username = (TextView) findViewById(R.id.profile_username);
        profilePictureContainer = (FrameLayout) findViewById(R.id.profile_profile_picture_container);
        shoutCountView = (TextView) findViewById(R.id.profile_shout_count);

        //Admin capability
        if (Constants.ADMIN) {
            if (Constants.PRODUCTION) {
                shoutCountView.setTextColor(getResources().getColor(R.color.shoutBlue));
            } else {
                shoutCountView.setTextColor(getResources().getColor(R.color.shoutPink));
            }

            shoutCountView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Constants.PRODUCTION = !Constants.PRODUCTION;

                    if (Constants.PRODUCTION) {
                        shoutCountView.setTextColor(getResources().getColor(R.color.shoutBlue));
                    } else {
                        shoutCountView.setTextColor(getResources().getColor(R.color.shoutPink));
                    }

                    SessionUtils.logOut(ProfileActivity.this);
                }
            });
        }

        //Not my profile
        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getIntExtra("userId", 0);
        //My profile
        } else {
            userId = SessionUtils.getCurrentUser(this).id;
            myProfileOptionsEnabled = true;
        }

        if (userId == SessionUtils.getCurrentUser(this).id) {

            if (myProfileOptionsEnabled) {
                profilePictureContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent settings = new Intent(ProfileActivity.this, SettingsActivity.class);
                        settings.putExtra("chooseProfilePicture", true);
                        startActivityForResult(settings, Constants.SETTINGS_REQUEST);
                    }
                });
            }
        }

        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), false);
    }

    protected void onResume() {
        super.onResume();

        getUserInfo(userId);
    }

    private void getUserInfo(int userId) {

        ApiUtils.getUserInfo(this, userId, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (progressDialog != null) {
                    progressDialog.cancel();
                }

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

                    if (imageLoaded) {
                        updateUI(false);
                    } else {
                        updateUI(true);
                        imageLoaded = true;
                    }
                }
            }
        });
    }

    private void updateUI(boolean reloadPicture) {
        shoutCountView.setText("" + user.shoutCount);


        if (reloadPicture) {
            GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(GeneralUtils.getProfileBigPicturePrefix() + user.id, false, false, 0, 0, null, AQuery.FADE_IN);
        }

        username.setText("@" + user.username);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Only display settings button if my profile
        if (myProfileOptionsEnabled) {
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
                updateUI(true);
            }
        }
    }




}