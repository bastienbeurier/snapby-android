package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.R;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.LocationUtils;

public class RefineShoutLocationActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private static final int UPDATE_SHOUT_LOCATION = 1437;

    private Location myLocation = null;

    private Location shoutLocation = null;

    private GoogleMap mMap = null;

    private Marker shoutLocationArrow = null;

    private LocationManager locationManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_shout_location);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        setUpMap();
        mapLoaded();
        letUserRefineShoutPosition();

        findViewById(R.id.refresh_shout_perimeter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapCameraPositionAndShoutPositionOnUserLocation();
            }
        });
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    private void setUpMap() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.shout_map)).getMap();

        //Set map settings
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setRotateGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setZoomGesturesEnabled(false);

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

        if (Constants.ADMIN) {
            settings.setScrollGesturesEnabled(true);
            settings.setZoomGesturesEnabled(true);
        }
    }

    private void mapLoaded() {
        //Update the camera to fit this perimeter (use of listener is a hack to know when map is loaded)
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                mMap.setOnCameraChangeListener(null);

                //User has already refined his shout location and is doing it again
                if (getIntent().hasExtra("shoutRefinedLocation")) {
                    shoutLocation = getIntent().getParcelableExtra("shoutRefinedLocation");
                    setMapCameraPositionOnShoutLocation();
                    updateShoutAccuratePosition(shoutLocation.getLatitude(), shoutLocation.getLongitude());
                //First time user refines his shout location
                } else {
                    //Update shout position to most recent user position if available
                    boolean myLocationAvailable = setMapCameraPositionAndShoutPositionOnUserLocation();

                    //Otherwise use position given by CreateShoutActivity
                    if (!myLocationAvailable) {
                        shoutLocation = getIntent().getParcelableExtra("shoutInitialLocation");
                        setMapCameraPositionOnShoutLocation();
                        updateShoutAccuratePosition(shoutLocation.getLatitude(), shoutLocation.getLongitude());
                    }
                }
            }
        });
    }

    private void setMapCameraPositionOnShoutLocation() {
        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, shoutLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
    }

    private boolean setMapCameraPositionAndShoutPositionOnUserLocation() {
        Location myMapLocation = mMap.getMyLocation();

        if (myMapLocation != null) {
            myLocation = myMapLocation;
        }

        if (myLocation == null) {
            myLocation = LocationUtils.getLastLocationWithLocationManager(RefineShoutLocationActivity.this, locationManager);
        }

        if (myLocation == null) {
            return false;
        }

        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, myLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));

        shoutLocation = myLocation;
        updateShoutAccuratePosition(myLocation.getLatitude(), myLocation.getLongitude());

        return true;
    }

    private void letUserRefineShoutPosition() {
        //Let user tap to indicate his accurate position
        GoogleMap.OnMapClickListener updateShoutLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                updateShoutAccuratePosition(latLng.latitude, latLng.longitude);
            }
        };

        //Let user also drag the location arrow to indicate his accurate position
        GoogleMap.OnMarkerDragListener updateShoutLocOnDrag = new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updateShoutAccuratePosition(marker.getPosition().latitude, marker.getPosition().longitude);
            }
        };

        GoogleMap.OnMapLongClickListener updateShoutLocOnLongClick = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                updateShoutAccuratePosition(latLng.latitude, latLng.longitude);
            }
        };

        mMap.setOnMapClickListener(updateShoutLocOnClick);
        mMap.setOnMarkerDragListener(updateShoutLocOnDrag);
        mMap.setOnMapLongClickListener(updateShoutLocOnLongClick);
    }

    private void updateShoutAccuratePosition(double lat, double lng) {
        shoutLocation = new Location("");
        shoutLocation.setLatitude(lat);
        shoutLocation.setLongitude(lng);

        if (shoutLocationArrow != null) {
            shoutLocationArrow.remove();
            shoutLocationArrow = null;
        }

        //Display marker the user is going to drag to specify his accurate position
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(shoutLocation.getLatitude(), shoutLocation.getLongitude()));
        marker.draggable(true);
        shoutLocationArrow = mMap.addMarker(marker);
    }

    public void endShoutLocationRefining() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("accurateShoutLocation", shoutLocation);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refine_location_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done_action) {
            endShoutLocationRefining();
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }
}