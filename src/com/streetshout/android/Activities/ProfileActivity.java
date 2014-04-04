package com.streetshout.android.activities;

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
public class ProfileActivity extends ListActivity implements EndlessListView.EndlessListener {

    private int ITEM_PER_REQUEST = 20;
    private int expiredShoutsPage = 1;
    private int activitiesPage = 1;

    private User user = null;
    private int userId = 0;

    private ImageView profilePicture = null;
    private TextView username = null;
    private TextView followerCountView = null;
    private TextView followingCountView = null;
    private LinearLayout followersButton = null;
    private LinearLayout followingButton = null;
    private LinearLayout shoutCountButton = null;
    private FrameLayout profilePictureContainer = null;
    private Location myLocation = null;
    private int followerCount = 0;
    private int followingCount = 0;
    private boolean following = false;
    private FrameLayout findFollowButton = null;
    private TextView findFollowLabel = null;
    private TextView shoutCountView = null;
    private boolean imageLoaded = false;
    public ProgressDialog progressDialog = null;
    private View progressBarWrapper = null;

    private View feedWrapperView = null;

    private View categoryActivityContainer = null;
    private View categoryShoutsContainer = null;
    private View categoryActivityMarker = null;
    private View categoryShoutsMarker = null;

    private boolean myProfileOptionsEnabled = false;

    private ActivitiesAdapter activitiesAdapter = null;
    private ExpiredShoutsAdapter expiredShoutsAdapter = null;

    private String adapterType = "";

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
        findFollowButton = (FrameLayout) findViewById(R.id.profile_find_follow_button);
        findFollowLabel = (TextView) findViewById(R.id.profile_find_follow_label);
        shoutCountView = (TextView) findViewById(R.id.profile_shout_count);
        shoutCountButton = (LinearLayout) findViewById(R.id.profile_shouts_button);

        categoryActivityContainer = findViewById(R.id.profile_category_activity_title_container);
        categoryShoutsContainer = findViewById(R.id.profile_category_shouts_title_container);
        categoryActivityMarker = findViewById(R.id.profile_category_activity_marker);
        categoryShoutsMarker = findViewById(R.id.profile_category_shouts_marker);

        progressBarWrapper = findViewById(R.id.profile_feed_progress_bar);
        feedWrapperView = findViewById(R.id.profile_feed_wrapper);

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
            //For suggested friends distance
            if (getIntent().hasExtra("myLocation")) {
                myLocation = getIntent().getParcelableExtra("myLocation");
            }

            userId = SessionUtils.getCurrentUser(this).id;
            myProfileOptionsEnabled = true;
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

            if (myProfileOptionsEnabled) {
                profilePictureContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent settings = new Intent(ProfileActivity.this, SettingsActivity.class);
                        settings.putExtra("chooseProfilePicture", true);
                        startActivityForResult(settings, Constants.SETTINGS_REQUEST);
                    }
                });
            } else {
                findFollowButton.setVisibility(View.GONE);
            }

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

        getListView().setDivider(null);

        if (myProfileOptionsEnabled) {
            categoryActivityContainer.setVisibility(View.VISIBLE);

            categoryActivityContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setActivitiesAdapter();
                    adapterType = "activity";
                    updateTitlesUI();
                }
            });

            View.OnClickListener displayShoutsListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setExpiredShoutsAdapter();
                    adapterType = "shouts";
                    updateTitlesUI();
                }
            };

            categoryShoutsContainer.setOnClickListener(displayShoutsListener);
            shoutCountButton.setOnClickListener(displayShoutsListener);

            setActivitiesAdapter();
            adapterType = "activity";
            updateTitlesUI();
        } else {
            setExpiredShoutsAdapter();
        }

        progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), false);
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

                if (progressDialog != null) {
                    progressDialog.cancel();
                }

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
        shoutCountView.setText("" + user.shoutCount);

        if (userId == SessionUtils.getCurrentUser(this).id) {
            findFollowLabel.setText(getString(R.string.find_friends));
            ImageUtils.setBackground(this, findFollowButton, R.drawable.follow_button);
        } else {
            if (following) {
                findFollowLabel.setText(this.getResources().getString(R.string.following_cap));

                ImageUtils.setBackground(this, findFollowButton, R.drawable.following_button);
            } else {
                findFollowLabel.setText(this.getResources().getString(R.string.follow_cap));
                ImageUtils.setBackground(this, findFollowButton, R.drawable.follow_button);
            }
        }

        if (reloadPicture) {
            GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(GeneralUtils.getProfileBigPicturePrefix() + user.id, false, false, 0, 0, null, AQuery.FADE_IN);
        }

        username.setText("@" + user.username);

        findFollowButton.setEnabled(true);
        followersButton.setEnabled(true);
        followingButton.setEnabled(true);
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

    private void showNoConnectionInFeedMessage() {
        hideFeedProgressBar();
        findViewById(R.id.profile_feed_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.no_connection_feed).setVisibility(View.VISIBLE);
        findViewById(R.id.profile_feed_wrapper).setVisibility(View.GONE);
    }

    public void showFeedProgressBar() {
        progressBarWrapper.setVisibility(View.VISIBLE);
        feedWrapperView.setVisibility(View.GONE);
    }

    public void hideFeedProgressBar() {
        progressBarWrapper.setVisibility(View.GONE);
        feedWrapperView.setVisibility(View.VISIBLE);
    }

    private void setExpiredShoutsAdapter() {
        if (expiredShoutsAdapter != null) {
            setListAdapter(expiredShoutsAdapter);
            return;
        }

        showFeedProgressBar();

        ApiUtils.getShouts(this, userId, expiredShoutsPage, ITEM_PER_REQUEST, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawShouts = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawShouts = result.getJSONArray("shouts");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (rawShouts != null) {
                        ArrayList<Shout> shouts = Shout.rawShoutsToInstances(rawShouts);
                        hideFeedProgressBar();
                        expiredShoutsPage++;
                        expiredShoutsAdapter = new ExpiredShoutsAdapter(ProfileActivity.this, shouts);
                        ((EndlessListView) getListView()).setAdapter(expiredShoutsAdapter);
                        ((EndlessListView) getListView()).setLoadingView(R.layout.loading);
                        ((EndlessListView) getListView()).setListener(ProfileActivity.this);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void setActivitiesAdapter() {
        if (activitiesAdapter != null) {
            setListAdapter(activitiesAdapter);
            return;
        }

        showFeedProgressBar();

        ApiUtils.getActivities(this, activitiesPage, ITEM_PER_REQUEST, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawActivities = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawActivities = result.getJSONArray("activities");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (rawActivities != null) {
                        ArrayList<UserActivity> userActivities = UserActivity.rawUserActivitiesToInstances(rawActivities);
                        hideFeedProgressBar();
                        activitiesPage++;
                        activitiesAdapter = new ActivitiesAdapter(ProfileActivity.this, userActivities);
                        ((EndlessListView) getListView()).setAdapter(activitiesAdapter);
                        ((EndlessListView) getListView()).setLoadingView(R.layout.loading);
                        ((EndlessListView) getListView()).setListener(ProfileActivity.this);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });

    }

    private void updateTitlesUI() {
        if (adapterType.equals("activity")) {
            categoryActivityContainer.setBackgroundColor(getResources().getColor(R.color.veryLightShoutBlue));
            categoryActivityMarker.setVisibility(View.VISIBLE);

            categoryShoutsContainer.setBackgroundColor(getResources().getColor(R.color.veryVeryLightGrey));
            categoryShoutsMarker.setVisibility(View.GONE);
        } else {
            categoryShoutsContainer.setBackgroundColor(getResources().getColor(R.color.veryLightShoutBlue));
            categoryShoutsMarker.setVisibility(View.VISIBLE);

            categoryActivityContainer.setBackgroundColor(getResources().getColor(R.color.veryVeryLightGrey));
            categoryActivityMarker.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadData() {
        if (adapterType.equals("activity")) {
            ApiUtils.getActivities(this, activitiesPage, ITEM_PER_REQUEST, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getError() == null && object != null) {
                        JSONArray rawActivities = null;

                        try {
                            JSONObject result = object.getJSONObject("result");
                            rawActivities = result.getJSONArray("activities");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (rawActivities != null) {
                            ArrayList<UserActivity> userActivities = UserActivity.rawUserActivitiesToInstances(rawActivities);
                            addNewActivities(userActivities);
                        }
                    } else {
                        showNoConnectionInFeedMessage();
                    }
                }
            });
        } else {
            ApiUtils.getShouts(this, userId, expiredShoutsPage, ITEM_PER_REQUEST, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getError() == null && object != null) {
                        JSONArray rawShouts = null;

                        try {
                            JSONObject result = object.getJSONObject("result");
                            rawShouts = result.getJSONArray("shouts");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (rawShouts != null) {
                            ArrayList<Shout> shouts = Shout.rawShoutsToInstances(rawShouts);
                            addNewExpiredShouts(shouts);
                        }
                    } else {
                        showNoConnectionInFeedMessage();
                    }
                }
            });
        }
    }

    public void addNewExpiredShouts(ArrayList<Shout> newShouts) {
        ((EndlessListView) getListView()).newDataAdded();

        int newShoutsCount = newShouts.size();
        for (int i = 0; i < newShoutsCount; i++) {
            expiredShoutsAdapter.items.add(newShouts.get(i));
        }

        expiredShoutsAdapter.notifyDataSetChanged();

        expiredShoutsPage++;
    }

    public void addNewActivities(ArrayList<UserActivity> newUserActivities) {
        ((EndlessListView) getListView()).newDataAdded();

        int newActivitiesCount = newUserActivities.size();
        for (int i = 0; i < newActivitiesCount; i++) {
            activitiesAdapter.items.add(newUserActivities.get(i));
        }

        activitiesAdapter.notifyDataSetChanged();

        activitiesPage++;
    }
}