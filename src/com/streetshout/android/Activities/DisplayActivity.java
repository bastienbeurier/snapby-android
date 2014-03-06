package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.models.Shout;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONObject;


/**
 * Created by bastien on 1/29/14.
 */
public class DisplayActivity extends Activity {

    private Location myLocation = null;

    private Shout shout = null;

    private ImageView imageView = null;

    private ConnectivityManager connectivityManager = null;

    private ImageView createLikeButton = null;

    private boolean imageFullScreen = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_shout);

        Intent intent = getIntent();

        if (intent.hasExtra("myLocation")) {
            myLocation = intent.getParcelableExtra("myLocation");
        }

        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        final TextView descriptionView = (TextView) findViewById(R.id.display_description_text);
        final ImageView dismissButton = (ImageView) findViewById(R.id.display_dismiss_button);
        final ImageView moreButton = (ImageView) findViewById(R.id.display_more_button);

        shout = getIntent().getParcelableExtra("shout");

        imageView = (ImageView) findViewById(R.id.display_shout_image_view);
        GeneralUtils.getAquery(this).id(imageView).image(shout.image + "--400", true, false, 0, 0, null, AQuery.FADE_IN);

        //Don't see TextView if no text in Shout
        if (shout.description.length() == 0) {
            descriptionView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        } else {
            descriptionView.setText(shout.description);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageFullScreen) {
                    descriptionView.setVisibility(View.VISIBLE);
                    dismissButton.setVisibility(View.VISIBLE);
                    moreButton.setVisibility(View.VISIBLE);

                    imageFullScreen = false;
                } else {
                    descriptionView.setVisibility(View.INVISIBLE);
                    dismissButton.setVisibility(View.INVISIBLE);
                    moreButton.setVisibility(View.INVISIBLE);

                    imageFullScreen = true;
                }
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        User currentUser = SessionUtils.getCurrentUser(this);

        if (currentUser.id < 3) {
            inflater.inflate(R.menu.display_shout_more_menu_admin, menu);
        } else if (currentUser.id == shout.userId) {
            inflater.inflate(R.menu.display_shout_more_menu_my_shout, menu);
        } else {
            inflater.inflate(R.menu.display_shout_more_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.share_item:
                GeneralUtils.shareShout(this, shout);
                return true;
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
            case R.id.delete_item:
                ApiUtils.removeShout(DisplayActivity.this, shout.id, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        Toast toast = null;

                        if (status.getError() != null) {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.failed_delete_shout), Toast.LENGTH_SHORT);
                        } else {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.successful_delete_shout), Toast.LENGTH_SHORT);
                        }

                        toast.show();

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("delete", "whatever");
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }
                });

                return true;
            case R.id.trending_item:
                ApiUtils.makeTrendingShout(DisplayActivity.this, shout.id, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        Toast toast = null;

                        if (status.getError() != null) {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.failed_trending_shout), Toast.LENGTH_SHORT);
                        } else {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.successful_trending_shout), Toast.LENGTH_SHORT);
                        }

                        toast.show();

                        finish();
                    }
                });

                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}