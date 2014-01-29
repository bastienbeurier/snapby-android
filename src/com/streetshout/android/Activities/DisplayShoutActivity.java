package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.TimeUtils;

/**
 * Created by bastien on 1/29/14.
 */
public class DisplayShoutActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private GoogleMap mMap = null;

    private Location myLocation = null;

    private Shout shout = null;

    private ImageView imageViewPlaceHolder = null;

    private ImageView imageView = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_shout);

        shout = getIntent().getParcelableExtra("shout");

        imageView = (ImageView) findViewById(R.id.display_shout_image_view);
        imageViewPlaceHolder = (ImageView) findViewById(R.id.display_shout_image_view_place_holder);

        ((TextView) findViewById(R.id.display_shout_username_textView)).setText("@" + shout.username);

        String[] ageStrings = TimeUtils.shoutAgeToShortStrings(TimeUtils.getShoutAge(shout.created));

        String stamp = ageStrings[0] + ageStrings[1] + " | ";

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
            GeneralUtils.getAquery(this).id(imageView).image(shout.image + "--400");
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        setUpMap();
        mapLoaded();
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