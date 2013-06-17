package com.streetshout.android.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.CameraPosition;
import com.streetshout.android.R;
import com.streetshout.android.Utils.Constants;
import com.streetshout.android.Utils.LocationUtils;

public class NavActivity extends Activity implements GoogleMap.OnMyLocationChangeListener {

    private ConnectivityManager connectivityManager = null;

    private LocationManager locationManager = null;

    private GoogleMap mMap = null;

    private Location myLocation = null;

    private CameraPosition savedCameraPosition = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav);

        this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (savedInstanceState != null) {
            savedCameraPosition = savedInstanceState.getParcelable("cameraPosition");
        }

        checkLocationServicesEnabled();
    }

    private void checkLocationServicesEnabled() {
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {}

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getText(R.string.no_location_dialog_title));
            dialog.setMessage(getText(R.string.no_location_dialog_message));
            dialog.setPositiveButton(getText(R.string.settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent settings = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    NavActivity.this.startActivity(settings);
                }
            });
            dialog.setNegativeButton(getText(R.string.skip), null);
            dialog.show();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        myLocation = location;
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean newMap = setUpMapIfNeeded();
        myLocation = getMyInitialLocation();

        //If the map is new, camera hasn't been initialized to user position, let's do it if we have the user location
        if (newMap) {
            if (savedCameraPosition != null) {
                initializeCameraWithCameraPosition(savedCameraPosition);
                savedCameraPosition = null;
            } else if (myLocation != null) {
                initializeCameraWithLocation(myLocation);
            }
        }
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (mMap == null) {
                return false;
            }

            //Set map settings
            UiSettings settings = mMap.getUiSettings();
            settings.setZoomControlsEnabled(false);
            settings.setCompassEnabled(true);
            settings.setMyLocationButtonEnabled(true);
            settings.setRotateGesturesEnabled(false);
            settings.setTiltGesturesEnabled(false);

            //Set user location
            mMap.setMyLocationEnabled(true);

            //Set location listener
            mMap.setOnMyLocationChangeListener(this);

            return true;
        }

        return false;
    }

    private Location getMyInitialLocation() {
        if (locationManager == null) {
            this.locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        }

        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        return locationManager.getLastKnownLocation(provider);
    }

    private void initializeCameraWithLocation(Location location) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtils.toLatLng(location), Constants.INITIAL_ZOOM);
        mMap.moveCamera(update);
    }

    private void initializeCameraWithCameraPosition(CameraPosition cameraPosition) {
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(update);
    }
}