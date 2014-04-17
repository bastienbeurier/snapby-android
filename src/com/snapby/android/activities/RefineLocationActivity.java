package com.snapby.android.activities;

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
import com.snapby.android.R;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.LocationUtils;

public class RefineLocationActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private Location myLocation = null;

    private Location snapbyLocation = null;

    private GoogleMap mMap = null;

    private Marker snapbyLocationArrow = null;

    private LocationManager locationManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.refine);

        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        setUpMap();
        mapLoaded();
        letUserRefineSnapbyPosition();

        findViewById(R.id.refresh_snapby_perimeter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapCameraPositionAndSnapbyPositionOnUserLocation();
            }
        });
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    private void setUpMap() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.snapby_map)).getMap();

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

                //User has already refined his snapby location and is doing it again
                if (getIntent().hasExtra("snapbyRefinedLocation")) {
                    snapbyLocation = getIntent().getParcelableExtra("snapbyRefinedLocation");
                    setMapCameraPositionOnSnapbyLocation();
                    updateSnapbyAccuratePosition(snapbyLocation.getLatitude(), snapbyLocation.getLongitude());
                //First time user refines his snapby location
                } else {
                    //Update snapby position to most recent user position if available
                    boolean myLocationAvailable = setMapCameraPositionAndSnapbyPositionOnUserLocation();

                    //Otherwise use position given by CreateActivity
                    if (!myLocationAvailable) {
                        snapbyLocation = getIntent().getParcelableExtra("snapbyInitialLocation");
                        setMapCameraPositionOnSnapbyLocation();
                        updateSnapbyAccuratePosition(snapbyLocation.getLatitude(), snapbyLocation.getLongitude());
                    }
                }
            }
        });
    }

    private void setMapCameraPositionOnSnapbyLocation() {
        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, snapbyLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
    }

    private boolean setMapCameraPositionAndSnapbyPositionOnUserLocation() {
        Location myMapLocation = mMap.getMyLocation();

        if (myMapLocation != null) {
            myLocation = myMapLocation;
        }

        if (myLocation == null) {
            myLocation = LocationUtils.getLastLocationWithLocationManager(RefineLocationActivity.this, locationManager);
        }

        if (myLocation == null) {
            return false;
        }

        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, myLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));

        snapbyLocation = myLocation;
        updateSnapbyAccuratePosition(myLocation.getLatitude(), myLocation.getLongitude());

        return true;
    }

    private void letUserRefineSnapbyPosition() {
        //Let user tap to indicate his accurate position
        GoogleMap.OnMapClickListener updateSnapbyLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                updateSnapbyAccuratePosition(latLng.latitude, latLng.longitude);
            }
        };

        //Let user also drag the location arrow to indicate his accurate position
        GoogleMap.OnMarkerDragListener updateSnapbyLocOnDrag = new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                updateSnapbyAccuratePosition(marker.getPosition().latitude, marker.getPosition().longitude);
            }
        };

        GoogleMap.OnMapLongClickListener updateSnapbyLocOnLongClick = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                updateSnapbyAccuratePosition(latLng.latitude, latLng.longitude);
            }
        };

        mMap.setOnMapClickListener(updateSnapbyLocOnClick);
        mMap.setOnMarkerDragListener(updateSnapbyLocOnDrag);
        mMap.setOnMapLongClickListener(updateSnapbyLocOnLongClick);
    }

    private void updateSnapbyAccuratePosition(double lat, double lng) {
        snapbyLocation = new Location("");
        snapbyLocation.setLatitude(lat);
        snapbyLocation.setLongitude(lng);

        if (snapbyLocationArrow != null) {
            snapbyLocationArrow.remove();
            snapbyLocationArrow = null;
        }

        //Display marker the user is going to drag to specify his accurate position
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(snapbyLocation.getLatitude(), snapbyLocation.getLongitude()));
        marker.draggable(true);
        snapbyLocationArrow = mMap.addMarker(marker);
    }

    public void endSnapbyLocationRefining() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("accurateSnapbyLocation", snapbyLocation);
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
            endSnapbyLocationRefining();
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }
}