package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
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
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.Utils.Constants;
import com.streetshout.android.Utils.LocationUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class NewShoutLocationActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private static final int UPDATE_SHOUT_LOCATION = 1437;

    private Location myLocation = null;

    private Location shoutLocation = null;

    private GoogleMap mMap = null;

    private Marker shoutLocationArrow = null;

    private ConnectivityManager connectivityManager = null;

    private AQuery aq = null;

    private Geocoder geocoder = null;

    private LatLngBounds mapLatLngBounds = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_shout_location);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        aq = new AQuery(this);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        myLocation = getIntent().getParcelableExtra("myLocation");

        geocoder = new Geocoder(this);

        setUpMap();
        setUpCameraPosition();
        letUserRefineShoutPosition();

        EditText addressView = (EditText) findViewById(R.id.shout_address_view);

        addressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) v).setError(null);
            }
        });

        addressView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_DONE) {
                    geocodeAddress(v);
                }
                return false;
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

    private void setUpCameraPosition() {
        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, myLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);

        //Update the camera to fit this perimeter (use of listener is a hack to know when map is loaded)
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                mMap.setOnCameraChangeListener(null);
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
                mapLatLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                updateShoutAccuratePosition(myLocation.getLatitude(), myLocation.getLongitude());
            }
        });
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
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
        marker.anchor((float) 0.5, (float) 0.95);
        shoutLocationArrow = mMap.addMarker(marker);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void createNewShoutFromInfo(View view) {
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        String userName = getIntent().getStringExtra("userName");
        String shoutDescription = getIntent().getStringExtra("shoutDescription");

        final ProgressDialog createShoutDialog = ProgressDialog.show(NewShoutLocationActivity.this, "",getString(R.string.shout_processing), false);

        ShoutModel.createShout(NewShoutLocationActivity.this, aq, shoutLocation.getLatitude(), shoutLocation.getLongitude(), userName, shoutDescription, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONObject rawShout = null;

                    try {
                        rawShout = object.getJSONObject("result");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    createShoutDialog.cancel();

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("newShout", ShoutModel.rawShoutToInstance(rawShout));
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    createShoutDialog.cancel();
                    Toast toast = Toast.makeText(NewShoutLocationActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    private void geocodeAddress(final TextView editTextView) {
        String dialogText = String.format(getString(R.string.create_shout_address_geocoding_processing), '"' + editTextView.getText().toString() + '"');
        final ProgressDialog addressDialog = ProgressDialog.show(NewShoutLocationActivity.this, "", dialogText, false);

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
                                Toast toast = Toast.makeText(NewShoutLocationActivity.this, getString(R.string.new_shout_geocoding_successful), Toast.LENGTH_SHORT);
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