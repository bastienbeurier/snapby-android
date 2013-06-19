package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.CameraUpdate;
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
import com.streetshout.android.Utils.AppPreferences;
import com.streetshout.android.Utils.Constants;
import com.streetshout.android.Utils.LocationUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class CreateShoutActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {
    private static int SHOUT_DESCR_LINES = 6;

    private AppPreferences appPrefs = null;

    private ConnectivityManager connectivityManager = null;

    private Location myLocation = null;

    private Location shoutAccurateLocation = null;

    private GoogleMap mMap = null;

    private Marker shoutLocationArrow = null;

    private AQuery aq = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_shout);
        getActionBar().setHomeButtonEnabled(true);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        aq = new AQuery(this);

        myLocation = getIntent().getParcelableExtra("myLocation");

        setUpMap();
        setUpCameraPosition();
        letUserRefineShoutPosition();

        //Set user name if we have it
        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        final EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        descriptionView.setHorizontallyScrolling(false);
        descriptionView.setLines(SHOUT_DESCR_LINES);
        final TextView charCountView = (TextView) findViewById(R.id.shout_descr_dialog_count);

        appPrefs = new AppPreferences(getApplicationContext());

        String savedUserName = appPrefs.getUserNamePref();
        if (savedUserName.length() > 0) {
            userNameView.setText(savedUserName);
            userNameView.clearFocus();
            descriptionView.requestFocus();
        }

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                descriptionView.setError(null);
                charCountView.setText((Constants.MAX_DESCRIPTION_LENGTH - s.length()) + " " + getString(R.string.characters));
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

        //Update the camera to fit this perimeter
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15);

        //Update the camera to fit this perimeter (use of listener is a hack to know when map is loaded)
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
                mMap.setOnCameraChangeListener(null);
            }
        });
    }

    private void letUserRefineShoutPosition() {
        //Let user tap to indicate his accurate position
        GoogleMap.OnMapClickListener updateShoutLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //Convert LatLng to Location
                shoutAccurateLocation = new Location("");
                shoutAccurateLocation.setLatitude(latLng.latitude);
                shoutAccurateLocation.setLongitude(latLng.longitude);
                updateShoutAccuratePosition();
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
                shoutAccurateLocation = new Location("");
                shoutAccurateLocation.setLatitude(marker.getPosition().latitude);
                shoutAccurateLocation.setLongitude(marker.getPosition().longitude);
                updateShoutAccuratePosition();
            }
        };

        mMap.setOnMapClickListener(updateShoutLocOnClick);
        mMap.setOnMarkerDragListener(updateShoutLocOnDrag);
    }

    private void updateShoutAccuratePosition() {
        if (shoutLocationArrow != null) {
            shoutLocationArrow.remove();
            shoutLocationArrow = null;
        }

        //Display marker the user is going to drag to specify his accurate position
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(shoutAccurateLocation.getLatitude(), shoutAccurateLocation.getLongitude()));
        marker.draggable(true);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_arrow));
        marker.anchor((float) 0.43, (float) 0.95);
        shoutLocationArrow = mMap.addMarker(marker);
    }

    public void validateShoutInfo(View view) {
        boolean errors = false;

        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        userNameView.setError(null);
        descriptionView.setError(null);

        String userName = userNameView.getText().toString();
        String description = descriptionView.getText().toString();

        if (userName.length() == 0) {
            userNameView.setError(getString(R.string.name_not_empty));
            errors = true;
        }

        if (userName.length() > Constants.MAX_USER_NAME_LENGTH) {
            userNameView.setError(getString(R.string.name_too_long));
            errors = true;
        }

        if (description.length() == 0) {
            descriptionView.setError(getString(R.string.description_not_empty));
            errors = true;
        }

        if (description.length() > Constants.MAX_DESCRIPTION_LENGTH) {
            descriptionView.setError(getString(R.string.description_too_long));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            createNewShoutFromInfo(userName, description);
        }
    }

    /** User confirmed shout creation after scpecifying accurate location and shout description */
    public void createNewShoutFromInfo(String userName, String description) {
        //Save user name in prefs
        appPrefs.setUserNamePref(userName);

        double lat = shoutAccurateLocation == null ? myLocation.getLatitude() : shoutAccurateLocation.getLatitude();
        double lng = shoutAccurateLocation == null ? myLocation.getLongitude() : shoutAccurateLocation.getLongitude();

        final ProgressDialog dialog = ProgressDialog.show(CreateShoutActivity.this, "",getString(R.string.shout_processing), false);

        ShoutModel.createShout(aq, lat, lng, userName, description, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                Log.d("BAB", "object: " + object);

                if (status.getError() == null && object != null) {
                    JSONObject rawShout = null;

                    try {
                        rawShout = object.getJSONObject("result");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    dialog.cancel();

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("newShout", ShoutModel.rawShoutToInstance(rawShout));
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    dialog.cancel();
                    Toast toast = Toast.makeText(CreateShoutActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }
}