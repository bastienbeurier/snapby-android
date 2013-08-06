package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.streetshout.android.R;
import com.streetshout.android.aws.AmazonClientManager;
import com.streetshout.android.aws.S3;
import com.streetshout.android.models.ShoutModel;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.StreetShoutApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class NewShoutContentActivity extends Activity {
    private static int MAX_SHOUT_DESCR_LINES = 6;

    private AQuery aq;

    private AppPreferences appPrefs = null;

    private ConnectivityManager connectivityManager = null;

    private Uri photoUri = null;

    private String photoPath = null;

    public static AmazonClientManager clientManager = null;

    private GoogleMap mMap = null;

    private Location shoutLocation = null;

    private boolean shoutLocationRefined = false;

    private String photoUrl = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_shout_content);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        aq = new AQuery(this);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        clientManager = new AmazonClientManager(getSharedPreferences(
                "com.streetshout.android", Context.MODE_PRIVATE));

        shoutLocation = getIntent().getParcelableExtra("myLocation");

        //Set user name if we have it
        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        final EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        descriptionView.setHorizontallyScrolling(false);
        descriptionView.setMaxLines(MAX_SHOUT_DESCR_LINES);
        final TextView charCountView = (TextView) findViewById(R.id.shout_descr_dialog_count);

        appPrefs = ((StreetShoutApplication) getApplicationContext()).getAppPrefs();

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
    protected void onResume() {
        super.onResume();

        if (mMap == null) {
            setUpMap();
        }
    }

    private void setUpMap() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.refine_location_map)).getMap();

        //Set map settings
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setRotateGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setZoomGesturesEnabled(false);

        //Disable clicking on markers
        GoogleMap.OnMarkerClickListener disableMarkerClick = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                refineShoutLocation();
                return true;
            }
        };

        mMap.setOnMarkerClickListener(disableMarkerClick);

        GoogleMap.OnMapClickListener updateShoutLocOnClick = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                refineShoutLocation();
            }
        };

        mMap.setOnMapClickListener(updateShoutLocOnClick);

        updateShoutMarkerLocation();
        setUpCameraPosition();
    }

    private void refineShoutLocation() {
        Intent newShoutNextStep = new Intent(NewShoutContentActivity.this, NewShoutLocationActivity.class);

        if (shoutLocationRefined) {
            newShoutNextStep.putExtra("shoutRefinedLocation", shoutLocation);
        }

        startActivityForResult(newShoutNextStep, Constants.NEW_SHOUT_CONTENT_ACTIVITY_REQUEST);
    }

    private void setUpCameraPosition() {
        //Update the camera to fit this perimeter (use of listener is a hack to know when map is loaded)
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                mMap.setOnCameraChangeListener(null);
                updateCameraPosition();
            }
        });
    }

    private void updateCameraPosition() {
        //Compute bounds of this perimeter
        LatLng[] boundsResult = LocationUtils.getLatLngBounds(Constants.SHOUT_RADIUS, shoutLocation);
        final LatLngBounds bounds = new LatLngBounds(boundsResult[0], boundsResult[1]);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, Constants.SHOUT_RADIUS / 15));
    }

    private void updateShoutMarkerLocation() {
        mMap.clear();

        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(shoutLocation.getLatitude(), shoutLocation.getLongitude()));
        marker.draggable(false);
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.shout_map_marker_selected));
        marker.anchor((float) 0.5, (float) 0.95);
        mMap.addMarker(marker);
    }

    public void validateShoutInfo(View view) {
        boolean errors = false;

        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        userNameView.setError(null);
        descriptionView.setError(null);

        String userName = userNameView.getText().toString();
        String shoutDescription = descriptionView.getText().toString();

        if (userName.length() == 0) {
            userNameView.setError(getString(R.string.name_not_empty));
            errors = true;
        }

        if (userName.length() > Constants.MAX_USER_NAME_LENGTH) {
            userNameView.setError(getString(R.string.name_too_long));
            errors = true;
        }

        if (shoutDescription.length() == 0) {
            descriptionView.setError(getString(R.string.description_not_empty));
            errors = true;
        }

        if (shoutDescription.length() > Constants.MAX_DESCRIPTION_LENGTH) {
            descriptionView.setError(getString(R.string.description_too_long));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            //Save user name in prefs
            appPrefs.setUserNamePref(userName);

            createNewShoutFromInfo(userName, shoutDescription, photoUrl);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.NEW_SHOUT_CONTENT_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                shoutLocation = data.getParcelableExtra("accurateShoutLocation");
                updateShoutMarkerLocation();
                updateCameraPosition();
                shoutLocationRefined = true;
            }
        }

        if (requestCode == Constants.UPLOAD_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = ImageUtils.getImageUrl(this, data, photoUri);
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                photoPath = cursor.getString(columnIndex);
                cursor.close();

                ImageView imageView = (ImageView) findViewById(R.id.new_shout_upload_photo);
                imageView.setImageBitmap(BitmapFactory.decodeFile(photoPath));
                imageView.setScaleType(ImageView.ScaleType.MATRIX);

                if (photoPath != null) {
                    String photoName = GeneralUtils.getDeviceId(this) + "--" + (new Date()).getTime();
                    photoUrl = Constants.S3_URL + photoName;

                    new ValidateCredentialsTask().execute(photoName);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
    }

    public void uploadPhoto(View view) {
        if (ImageUtils.isSDPresent() == false){
            Toast toast = Toast.makeText(this, this.getString(R.string.no_sd_card), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        photoUri = ImageUtils.reserveUriForPicture(this);
        Intent chooserIntent = ImageUtils.getPhotoChooserIntent(this, photoUri);


        startActivityForResult(chooserIntent, Constants.UPLOAD_PHOTO_REQUEST);
    }

    private class ValidateCredentialsTask extends
            AsyncTask<String, Void, Response> {

        String photoName = null;

        protected Response doInBackground(String... params) {
            photoName = params[0];

            return NewShoutContentActivity.clientManager.validateCredentials();
        }

        protected void onPostExecute(Response response) {
            if (response != null && response.requestWasSuccessful()) {
                Thread t = new Thread() {
                    @Override
                    public void run(){
                        try{
                            S3.addImageInBucket(photoPath, photoName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
            }
        }
    }

    public void createNewShoutFromInfo(String userName, String shoutDescription, String shoutImageUrl) {
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        final ProgressDialog createShoutDialog = ProgressDialog.show(this, "", getString(R.string.shout_processing), false);

        ShoutModel.createShout(this, aq, shoutLocation.getLatitude(), shoutLocation.getLongitude(), userName, shoutDescription, shoutImageUrl, new AjaxCallback<JSONObject>() {
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
                    Toast toast = Toast.makeText(NewShoutContentActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }
}