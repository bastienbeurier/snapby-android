package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

    private Geocoder geocoder = null;

    private LatLngBounds mapLatLngBounds = null;

    private LocationManager locationManager = null;

    private EditText addressView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_shout_location);

        geocoder = new Geocoder(this);

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        setUpMap();
        mapLoaded();
        letUserRefineShoutPosition();

        addressView = (EditText) findViewById(R.id.shout_address_view);

        addressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) v).setError(null);
            }
        });

        addressView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_SEARCH) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(addressView.getWindowToken(), 0);
                    geocodeAddress(v);
                }
                return false;
            }
        });

        findViewById(R.id.refresh_shout_perimeter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMapCameraPositionOnUserLocation();
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

                setMapCameraPositionOnUserLocation();

                if (getIntent().hasExtra("shoutRefinedLocation")) {
                    shoutLocation = getIntent().getParcelableExtra("shoutRefinedLocation");
                    updateShoutAccuratePosition(shoutLocation.getLatitude(), shoutLocation.getLongitude());
                } else {
                    updateShoutAccuratePosition(myLocation.getLatitude(), myLocation.getLongitude());
                }
            }
        });
    }

    private void setMapCameraPositionOnUserLocation() {
        Location myMapLocation = mMap.getMyLocation();

        if (myMapLocation != null) {
            myLocation = myMapLocation;
        } else if (myLocation == null) {
            myLocation = LocationUtils.getLastLocationWithLocationManager(RefineShoutLocationActivity.this, locationManager);
        }

        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, myLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
        mapLatLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }

    private void letUserRefineShoutPosition() {
        //Let user tap to indicate his accurate position
        GoogleMap.OnMapClickListener updateShoutLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                updateShoutAccuratePosition(latLng.latitude, latLng.longitude);
                addressView.setText("");
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
                addressView.setText("");
            }
        };

        GoogleMap.OnMapLongClickListener updateShoutLocOnLongClick = new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                updateShoutAccuratePosition(latLng.latitude, latLng.longitude);
                addressView.setText("");
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
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
        marker.anchor((float) 0.5, (float) 0.95);
        shoutLocationArrow = mMap.addMarker(marker);
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void createNewShoutFromInfo(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("accurateShoutLocation", shoutLocation);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void geocodeAddress(final TextView editTextView) {
        String dialogText = String.format(getString(R.string.create_shout_address_geocoding_processing), '"' + editTextView.getText().toString() + '"');
        final ProgressDialog addressDialog = ProgressDialog.show(RefineShoutLocationActivity.this, "", dialogText, false);

        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == UPDATE_SHOUT_LOCATION) {
                    Address address = (Address) msg.obj;

                    if (address != null) {
                        try {
                            double addressLat = address.getLatitude();
                            double addressLng = address.getLongitude();


                            if (addressLat > mapLatLngBounds.southwest.latitude
                                    && addressLat < mapLatLngBounds.northeast.latitude
                                    && addressLng > mapLatLngBounds.southwest.longitude
                                    && addressLng < mapLatLngBounds.northeast.longitude) {
                                updateShoutAccuratePosition(addressLat, addressLng);
                                addressDialog.cancel();
                                Toast toast = Toast.makeText(RefineShoutLocationActivity.this, getString(R.string.new_shout_geocoding_successful), Toast.LENGTH_SHORT);
                                toast.show();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    addressDialog.cancel();
                    editTextView.setError(getString(R.string.new_shout_geocoding_failed));
                    editTextView.setText("");
                }

                super.handleMessage(msg);
            }
        };

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Address address = LocationUtils.geocodeAddress(geocoder, editTextView.getText().toString(), mapLatLngBounds);
                    Message msg = handler.obtainMessage();
                    msg.what = UPDATE_SHOUT_LOCATION;
                    msg.obj = address;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }
}