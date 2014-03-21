package com.streetshout.android.activities;

import android.app.Activity;
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
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.ImageOptions;
import com.facebook.Session;
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
    private TextView followerCountView = null;
    private TextView followingCountView = null;
    private LinearLayout followersButton = null;
    private LinearLayout followingButton = null;
    private FrameLayout profilePictureContainer = null;
    private Location myLocation = null;
    private int followerCount = 0;
    private int followingCount = 0;
    private boolean following = false;
    private ImageView findFollowButton = null;
    private TextView findFollowLabel = null;
    private TextView shoutCountView = null;
    private boolean imageLoaded = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicture = (ImageView) findViewById(R.id.profile_user_picture);
        username = (TextView) findViewById(R.id.profile_username);
        followerCountView = (TextView) findViewById(R.id.profile_follower_count);
        followingCountView = (TextView) findViewById(R.id.profile_following_count);
        followersButton = (LinearLayout) findViewById(R.id.profile_followers_button);
        followingButton = (LinearLayout) findViewById(R.id.profile_following_button);
        profilePictureContainer = (FrameLayout) findViewById(R.id.profile_profile_picture_container);
        findFollowButton = (ImageView) findViewById(R.id.profile_find_follow_button);
        findFollowLabel = (TextView) findViewById(R.id.profile_find_follow_label);
        shoutCountView = (TextView) findViewById(R.id.profile_shout_count);

        //Not my profile
        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getIntExtra("userId", 0);
        //My profile
        } else {
            //For suggested friends distance
            if (getIntent().hasExtra("myLocation")) {
                myLocation = getIntent().getParcelableExtra("myLocation");
            }

            userId = SessionUtils.getCurrentUser(this).id;
        }

        followersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent followers = new Intent(ProfileActivity.this, FollowerActivity.class);

                if (userId != SessionUtils.getCurrentUser(ProfileActivity.this).id) {
                    followers.putExtra("userId", userId);
                }

                followers.putExtra("adapterType", "followers");

                startActivityForResult(followers, Constants.FOLLOWERS_REQUEST);
            }
        });

        followingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent following = new Intent(ProfileActivity.this, FollowerActivity.class);

                if (userId != SessionUtils.getCurrentUser(ProfileActivity.this).id) {
                    following.putExtra("userId", userId);
                }

                following.putExtra("adapterType", "following");

                startActivityForResult(following, Constants.FOLLOWERS_REQUEST);
            }
        });

        if (userId == SessionUtils.getCurrentUser(this).id) {
            profilePictureContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO let user choose image if no image
                }
            });

            findFollowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent suggestedUsers = new Intent(ProfileActivity.this, FollowerActivity.class);

                    if (myLocation != null) {
                        suggestedUsers.putExtra("myLocation", myLocation);
                    }

                    startActivityForResult(suggestedUsers, Constants.FOLLOWERS_REQUEST);
                }
            });
        } else {
            //updateUI
            findFollowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollow();
                }
            });
        }

        //Don't tap before loading
        findFollowButton.setEnabled(false);
        followersButton.setEnabled(false);
        followingButton.setEnabled(false);
    }

    private void toggleFollow() {
        if (following) {
            ApiUtils.unfollowUser(this, userId, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    getUserInfo(userId);
                }
            });

            following = !following;
            updateUI(false);
        } else {
            ApiUtils.followUser(this, userId, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    getUserInfo(userId);
                }
            });

            following = !following;
            updateUI(false);
        }
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

                if (status.getError() == null) {
                    JSONObject result = null;
                    JSONObject rawUser = null;

                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");

                        followerCount = Integer.parseInt(result.getString("followers_count"));
                        followingCount = Integer.parseInt(result.getString("followed_count"));
                        following = Boolean.parseBoolean(result.getString("is_followed"));

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
        followerCountView.setText("" + followerCount);
        followingCountView.setText("" + followingCount);
        shoutCountView.setText("(" + user.shoutCount + " shouts)");

        if (userId == SessionUtils.getCurrentUser(this).id) {
            findFollowLabel.setText(getString(R.string.find_friends));
        } else {
            if (following) {
                findFollowLabel.setText(this.getResources().getString(R.string.unfollow_cap));
            } else {
                findFollowLabel.setText(this.getResources().getString(R.string.follow_cap));
            }
        }

        if (reloadPicture) {
            ImageOptions options = new ImageOptions();
            options.round = 8;
            //Bust cache in case user changes is profile picture
            options.memCache = false;
            options.fileCache = false;
            options.animation = AQuery.FADE_IN;

            GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(Constants.PROFILE_PICS_URL_PREFIX + user.id, options);
        }

        username.setText("@" + user.username);

        findFollowButton.setEnabled(true);
        followersButton.setEnabled(true);
        followingButton.setEnabled(true);
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
                updateUI(true);
            }
        }
    }
}