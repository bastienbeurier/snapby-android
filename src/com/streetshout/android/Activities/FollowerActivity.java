package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.adapters.FollowerAdapter;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Created by bastien on 3/13/14.
 */
public class FollowerActivity extends ListActivity {

    private View progressBarWrapper = null;

    private View feedWrapperView = null;

    private Location myLocation = null;

    private TreeSet<Integer> followedByMe = null;

    private int userId = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.follower);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        followedByMe = new TreeSet<Integer>();

        if (getIntent().hasExtra("myLocation")) {
            myLocation = getIntent().getParcelableExtra("myLocation");
        }

        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getIntExtra("userId", 0);
        } else {
            userId = SessionUtils.getCurrentUser(this).id;
        }

        progressBarWrapper = findViewById(R.id.follower_feed_progress_bar);
        feedWrapperView = findViewById(R.id.follower_feed_wrapper);

        showFeedProgressBar();


        if (getIntent().hasExtra("adapterType")) {
            if (getIntent().getStringExtra("adapterType").equals("followers")) {
                getFollowers();
            } else {
                getFollowingUsers();
            }
        } else {
            getSuggestedUsers();
        }
    }

    private JSONArray parseJSON(JSONObject object, String followerType) {
        JSONArray rawUsers = null;

        try {
            JSONObject result = object.getJSONObject("result");
            rawUsers = result.getJSONArray(followerType);

            JSONArray rawFollowedByMe = result.getJSONArray("current_user_followed_user_ids");

            int count = rawFollowedByMe.length();

            for (int i = 0 ; i < count ; i++) {
                followedByMe.add(Integer.parseInt(rawFollowedByMe.getString(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rawUsers;
    }

    private void getFollowers() {
        ApiUtils.getFollowers(this, userId, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawUsers = parseJSON(object, "followers");

                    if (rawUsers != null) {
                        ArrayList<User> users = User.rawUsersToInstances(rawUsers);
                        hideFeedProgressBar();
                        setAdapter(FollowerActivity.this, users, null);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void getFollowingUsers() {
        ApiUtils.getFollowingUsers(this, userId, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawUsers = parseJSON(object, "followed_users");

                    if (rawUsers != null) {
                        ArrayList<User> users = User.rawUsersToInstances(rawUsers);
                        hideFeedProgressBar();
                        setAdapter(FollowerActivity.this, users, null);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void getSuggestedUsers() {
        ApiUtils.getSuggestedFriends(this, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawUsers = parseJSON(object, "suggested_friends");

                    if (rawUsers != null) {
                        ArrayList<User> users = User.rawUsersToInstances(rawUsers);
                        hideFeedProgressBar();
                        setAdapter(FollowerActivity.this, users, myLocation);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    public boolean followedByMe(int userId) {
        return followedByMe.contains(userId);
    }

    public void addToFollowedByMe(int userId) {
        followedByMe.add(userId);
    }

    public void removeFromFollowedByMe(int userId) {
        followedByMe.remove(userId);
    }

    private void showNoConnectionInFeedMessage() {
        hideFeedProgressBar();
        findViewById(R.id.follower_feed_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.no_connection_feed).setVisibility(View.VISIBLE);
        findViewById(R.id.follower_feed_wrapper).setVisibility(View.GONE);
    }

    public void showFeedProgressBar() {
        progressBarWrapper.setVisibility(View.VISIBLE);
        feedWrapperView.setVisibility(View.GONE);
    }

    public void hideFeedProgressBar() {
        progressBarWrapper.setVisibility(View.GONE);
        feedWrapperView.setVisibility(View.VISIBLE);
    }

    public void setAdapter(FollowerActivity activity, ArrayList<User> users, Location myLocation) {
        setListAdapter(new FollowerAdapter(activity, users, myLocation));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}