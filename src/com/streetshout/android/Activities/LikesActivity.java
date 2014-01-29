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
import com.streetshout.android.adapters.CommentsAdapter;
import com.streetshout.android.adapters.LikesAdapter;
import com.streetshout.android.models.Comment;
import com.streetshout.android.models.Like;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class LikesActivity extends ListActivity {

    private View progressBarWrapper = null;

    private View feedWrapperView = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.likes);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        progressBarWrapper = findViewById(R.id.likes_feed_progress_bar);
        feedWrapperView = findViewById(R.id.likes_feed_wrapper);

        Shout shout = getIntent().getParcelableExtra("shout");

        final Location shoutLocation = new Location("");
        shoutLocation.setLatitude(shout.lat);
        shoutLocation.setLongitude(shout.lng);

        showFeedProgressBar();

        ApiUtils.getLikes(this, shout, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawLikes = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawLikes = result.getJSONArray("likes");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (rawLikes != null) {
                        ArrayList<Like> comments = Like.rawLikesToInstances(rawLikes);
                        hideFeedProgressBar();
                        setAdapter(LikesActivity.this, comments, shoutLocation);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });
    }

    private void showNoConnectionInFeedMessage() {
        hideFeedProgressBar();
        findViewById(R.id.likes_feed_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.no_connection_feed).setVisibility(View.VISIBLE);
        findViewById(R.id.likes_feed_wrapper).setVisibility(View.GONE);
    }

    public void showFeedProgressBar() {
        progressBarWrapper.setVisibility(View.VISIBLE);
        feedWrapperView.setVisibility(View.GONE);
    }

    public void hideFeedProgressBar() {
        progressBarWrapper.setVisibility(View.GONE);
        feedWrapperView.setVisibility(View.VISIBLE);
    }

    public void setAdapter(Activity activity, ArrayList<Like> likes, Location shoutLocation) {
        setListAdapter(new LikesAdapter(activity, likes, shoutLocation));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}