package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.streetshout.android.R;
import com.streetshout.android.custom.SquareImageView;
import com.streetshout.android.models.Like;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class DisplayShoutActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private GoogleMap mMap = null;

    private Location myLocation = null;

    private Shout shout = null;

    private ImageView imageViewPlaceHolder = null;

    private SquareImageView imageView = null;

    private TextView likeCountView = null;

    private TextView commentCountView = null;

    private LocationManager locationManager = null;

    private ConnectivityManager connectivityManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_shout);

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        shout = getIntent().getParcelableExtra("shout");

        likeCountView = (TextView) findViewById(R.id.display_shout_like_count_textView);
        commentCountView = (TextView) findViewById(R.id.display_shout_comment_count_textView);
        imageView = (SquareImageView) findViewById(R.id.display_shout_image_view);
        imageViewPlaceHolder = (ImageView) findViewById(R.id.display_shout_image_view_place_holder);

        commentCountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
                    Toast toast = Toast.makeText(DisplayShoutActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Intent comments = new Intent(DisplayShoutActivity.this, CommentsActivity.class);
                    comments.putExtra("shout", shout);
                    startActivity(comments);
                }

                v.setEnabled(true);
            }
        });

        updateUI();
        setUpMap();
        mapLoaded();
    }

    private void updateLikeCount(int count) {
        if (count > 1) {
            likeCountView.setText(count + " " + getString(R.string.likes));
        } else {
            likeCountView.setText(count + " " + getString(R.string.like));
        }

        likeCountView.setVisibility(View.VISIBLE);
    }

    private void updateCommentCount(int count) {
        if (count > 1) {
            commentCountView.setText(count + " " + getString(R.string.comments));
        } else {
            commentCountView.setText(count + " " + getString(R.string.comment));
        }

        commentCountView.setVisibility(View.VISIBLE);
    }

    private void updateUI() {
        ApiUtils.getShoutMetaData(this, GeneralUtils.getAquery(this), shout.id, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONObject rawShout = null;

                    int commentCount = 0;
                    ArrayList<Integer> likerIds = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        commentCount = result.getInt("comment_count");
                        JSONArray rawlikerIds = result.getJSONArray("liker_ids");
                        likerIds = Like.rawLikerIdsToIntegers(rawlikerIds);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (likerIds != null) {
                        updateLikeCount(likerIds.size());
                    }

                    updateCommentCount(commentCount);
                }
            }
        });

        ((TextView) findViewById(R.id.display_shout_username_textView)).setText("@" + shout.username);

        ((TextView) findViewById(R.id.display_shout_description_textView)).setText(shout.description);

        String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));

        String stamp = ageStrings[0] + ageStrings[1] + " | ";

        if (myLocation == null) {
            myLocation = getIntent().getParcelableExtra("myLocation");
        }

        if (myLocation == null) {
            myLocation = LocationUtils.getLastLocationWithLocationManager(this, locationManager);
        }

        if (myLocation != null) {
            Location shoutLocation = new Location("");
            shoutLocation.setLatitude(shout.lat);
            shoutLocation.setLongitude(shout.lng);

            String[] distanceStrings = LocationUtils.formattedDistanceStrings(this, myLocation, shoutLocation);
            stamp += distanceStrings[0] + distanceStrings[1];
        } else {
            stamp += "?";
        }

        ((TextView) findViewById(R.id.display_shout_stamp_textView)).setText(stamp);

        GeneralUtils.getAquery(this).id(imageViewPlaceHolder).image(R.drawable.shout_image_place_holder_square);

        if (shout.image != null && shout.image.length() > 0) {
            imageView.setVisibility(View.VISIBLE);

            GeneralUtils.getAquery(this).image(shout.image + "--400", true, true, 0, 0, new BitmapAjaxCallback() {
                @Override
                public void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status) {
                    imageView.setBackground(new BitmapDrawable(DisplayShoutActivity.this.getResources(), bm));
                    imageViewPlaceHolder.setVisibility(View.GONE);
                }
            });
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    private void setUpMap() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.display_shout_map)).getMap();

        //Set map settings
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setRotateGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(true);
        settings.setZoomGesturesEnabled(true);

        //Set user location
        mMap.setMyLocationEnabled(true);

        //Set location listener
        mMap.setOnMyLocationChangeListener(this);

        //Disable clicking on markers
        GoogleMap.OnMarkerClickListener disableMarkerClick = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {return true;}
        };

        mMap.setOnMarkerClickListener(disableMarkerClick);
    }

    private void mapLoaded() {
        //Update the camera to fit this perimeter (use of listener is a hack to know when map is loaded)
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                mMap.setOnCameraChangeListener(null);

                setMapCameraPositionOnShoutLocation();
            }
        });
    }

    private void setMapCameraPositionOnShoutLocation() {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(shout.lat, shout.lng), Constants.REDIRECTION_FROM_CREATE_SHOUT);
        mMap.moveCamera(update);
    }
}