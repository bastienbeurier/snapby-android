package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.GeneralUtils;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class DisplayActivity extends Activity {

    private Location myLocation = null;

    private Shout shout = null;

    private ImageView imageView = null;

    private ConnectivityManager connectivityManager = null;

    private ImageView createLikeButton = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_shout);

        Intent intent = getIntent();

        if (intent.hasExtra("myLocation")) {
            myLocation = intent.getParcelableExtra("myLocation");
        }

        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        shout = getIntent().getParcelableExtra("shout");

        imageView = (ImageView) findViewById(R.id.display_shout_image_view);

        findViewById(R.id.shout_comment_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCommentsActivity(v);
            }
        });

        findViewById(R.id.shout_share_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = ApiUtils.getUserSiteUrl() + "/shouts/" + shout.id;
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_shout_text, url));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_shout_subject));
                sendIntent.setType("text/plain");

                startActivity(sendIntent);
            }
        });

        findViewById(R.id.shout_more_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });

        createLikeButton = (ImageView) findViewById(R.id.shout_like_button);
        createLikeButton.setEnabled(false);

        createLikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("BAB", "CLICKED !!!!!!");

                v.setEnabled(false);
                if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
                    Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Intent likes = new Intent(DisplayActivity.this, LikesActivity.class);
                    likes.putExtra("shout", shout);

                    if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0)  {
                        likes.putExtra("myLocation", myLocation);
                    }

                    startActivity(likes);
                }

                v.setEnabled(true);
//
//
//                createLikeButton.setEnabled(false);
//                likerIds.add(0, SessionUtils.getCurrentUser(DisplayActivity.this).id);
//                updateUIOnShoutLiked(true);
//
//                double lat = 0;
//                double lng = 0;
//
//                if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
//                    lat = myLocation.getLatitude();
//                    lng = myLocation.getLongitude();
//                }
//
//                ApiUtils.createLike(DisplayActivity.this, shout, lat, lng, new AjaxCallback<JSONObject>() {
//                    @Override
//                    public void callback(String url, JSONObject object, AjaxStatus status) {
//                        super.callback(url, object, status);
//
//                        createLikeButton.setEnabled(true);
//
//                        if (status.getError() != null) {
//                            Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.shout_like_failed), Toast.LENGTH_SHORT);
//                            toast.show();
//                            likerIds.remove(0);
//                            updateUIOnShoutLiked(false);
//                        }
//                    }
//                });
            }
        });

        GeneralUtils.getAquery(this).id(imageView).image(shout.image + "--400");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void startCommentsActivity(View v) {
        v.setEnabled(false);
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Intent comments = new Intent(DisplayActivity.this, CommentsActivity.class);
            comments.putExtra("shout", shout);

            if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0)  {
                comments.putExtra("myLocation", myLocation);
            }

            startActivity(comments);
        }

        v.setEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.display_shout_more_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.directions_item:
                String uri = "http://maps.google.com/maps?daddr=" + shout.lat + "," + shout.lng;
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
                return true;
            case R.id.report_item:
                ApiUtils.reportShout(DisplayActivity.this, shout.id, "general", new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        if (status.getError() != null) {
                            Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.report_shout_failed), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });

                Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.report_shout_successful), Toast.LENGTH_SHORT);
                toast.show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}