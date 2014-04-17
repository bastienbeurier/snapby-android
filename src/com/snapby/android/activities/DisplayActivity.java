package com.snapby.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.snapby.android.R;
import com.snapby.android.models.Snapby;
import com.snapby.android.models.User;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.SessionUtils;
import org.json.JSONObject;


/**
 * Created by bastien on 1/29/14.
 */
public class DisplayActivity extends Activity {

    private Snapby snapby = null;

    private ImageView imageView = null;

    private boolean imageFullScreen = false;

    private boolean expiredSnapby = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);

        Intent intent = getIntent();

        if (intent.hasExtra("expiredSnapby")) {
            expiredSnapby = true;
        }

        final ImageView dismissButton = (ImageView) findViewById(R.id.display_dismiss_button);
        final ImageView moreButton = (ImageView) findViewById(R.id.display_more_button);

        snapby = getIntent().getParcelableExtra("snapby");

        imageView = (ImageView) findViewById(R.id.display_snapby_image_view);

        Bitmap preset = GeneralUtils.getAquery(this).getCachedImage(GeneralUtils.getSnapbySmallPicturePrefix() + snapby.id + "--400");
        GeneralUtils.getAquery(this).id(imageView).image(GeneralUtils.getSnapbyBigPicturePrefix() + snapby.id + "--400", true, false, 0, 0, preset, AQuery.FADE_IN);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageFullScreen) {
                    dismissButton.setVisibility(View.VISIBLE);
                    moreButton.setVisibility(View.VISIBLE);

                    imageFullScreen = false;
                } else {
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
            inflater.inflate(R.menu.display_snapby_more_menu_admin, menu);
        } else if (currentUser.id == snapby.userId) {
            inflater.inflate(R.menu.display_snapby_more_menu_my_snapby, menu);
        } else {
            inflater.inflate(R.menu.display_snapby_more_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.share_item:
                GeneralUtils.shareSnapby(this, snapby);
                return true;
            case R.id.directions_item:
                String uri = "http://maps.google.com/maps?daddr=" + snapby.lat + "," + snapby.lng;
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                startActivity(intent);
                return true;
            case R.id.report_item:
                ApiUtils.reportSnapby(DisplayActivity.this, snapby.id, "general", new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        if (status.getError() != null) {
                            Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.report_snapby_failed), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });

                Toast toast = Toast.makeText(DisplayActivity.this, getString(R.string.report_snapby_successful), Toast.LENGTH_SHORT);
                toast.show();
                return true;
            case R.id.delete_item:
                ApiUtils.removeSnapby(DisplayActivity.this, snapby.id, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        Toast toast = null;

                        if (status.getError() != null) {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.failed_delete_snapby), Toast.LENGTH_SHORT);
                        } else {
                            toast = Toast.makeText(DisplayActivity.this, getString(R.string.successful_delete_snapby), Toast.LENGTH_SHORT);
                        }

                        toast.show();

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("delete", "whatever");
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    }
                });

                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}