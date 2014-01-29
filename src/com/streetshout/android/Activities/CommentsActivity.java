package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.adapters.CommentsAdapter;
import com.streetshout.android.models.Comment;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class CommentsActivity extends ListActivity {

    private View progressBarWrapper = null;

    private View feedWrapperView = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        progressBarWrapper = findViewById(R.id.comments_feed_progress_bar);
        feedWrapperView = findViewById(R.id.comments_feed_wrapper);

        final Shout shout = getIntent().getParcelableExtra("shout");
        final Location myLocation = getIntent().getParcelableExtra("myLocation");

        final Location shoutLocation = new Location("");
        shoutLocation.setLatitude(shout.lat);
        shoutLocation.setLongitude(shout.lng);

        showFeedProgressBar();

        ApiUtils.getComments(this, shout, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONArray rawComments = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawComments = result.getJSONArray("comments");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (rawComments != null) {
                        ArrayList<Comment> comments = Comment.rawCommentsToInstances(rawComments);
                        hideFeedProgressBar();
                        setAdapter(CommentsActivity.this, comments, shoutLocation);
                    }
                } else {
                    showNoConnectionInFeedMessage();
                }
            }
        });

        final EditText createCommentEditText = (EditText) findViewById(R.id.create_comment_editText);
        createCommentEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    createCommentEditText.setEnabled(false);

                    double lat = 0;
                    double lng = 0;

                    if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        lat = myLocation.getLatitude();
                        lng = myLocation.getLongitude();
                    }

                    ApiUtils.createComment(CommentsActivity.this, createCommentEditText.getText().toString(), shout, lat, lng, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            createCommentEditText.setEnabled(false);

                            if (status.getError() == null && object != null) {

                                JSONArray rawComments = null;

                                try {
                                    JSONObject result = object.getJSONObject("result");
                                    rawComments = result.getJSONArray("comments");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                if (rawComments != null) {
                                    createCommentEditText.setEnabled(true);
                                    createCommentEditText.setText("");
                                    ArrayList<Comment> comments = Comment.rawCommentsToInstances(rawComments);
                                    setAdapter(CommentsActivity.this, comments, shoutLocation);
                                }
                            } else {
                                Toast toast = Toast.makeText(CommentsActivity.this, getString(R.string.create_comment_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                createCommentEditText.setEnabled(true);
                            }
                        }
                    });

                }

                return false;
            }
        });
    }

    private void showNoConnectionInFeedMessage() {
        hideFeedProgressBar();
        findViewById(R.id.comments_feed_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.no_connection_feed).setVisibility(View.VISIBLE);
        findViewById(R.id.comments_feed_wrapper).setVisibility(View.GONE);
    }

    public void showFeedProgressBar() {
        progressBarWrapper.setVisibility(View.VISIBLE);
        feedWrapperView.setVisibility(View.GONE);
    }

    public void hideFeedProgressBar() {
        progressBarWrapper.setVisibility(View.GONE);
        feedWrapperView.setVisibility(View.VISIBLE);
    }

    public void setAdapter(Activity activity, ArrayList<Comment> comments, Location shoutLocation) {
        setListAdapter(new CommentsAdapter(activity, comments, shoutLocation));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}