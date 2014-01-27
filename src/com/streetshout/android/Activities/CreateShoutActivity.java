package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.streetshout.android.R;
import com.streetshout.android.aws.AmazonClientManager;
import com.streetshout.android.aws.S3;
import com.streetshout.android.models.Shout;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.StreetShoutApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

public class CreateShoutActivity extends Activity {
    private static int MAX_SHOUT_DESCR_LINES = 6;

    private AQuery aq;

    private AppPreferences appPrefs = null;

    private ConnectivityManager connectivityManager = null;

    public static AmazonClientManager clientManager = null;

    private GoogleMap mMap = null;

    private Location shoutLocation = null;

    private boolean shoutLocationRefined = false;

    private String photoName = null;

    private String photoUrl = null;

    private ProgressDialog createShoutDialog;

    private String userName = null;

    private String shoutDescription = null;

    private ImageView removePhotoButton = null;

    private ImageView flipPhotoButton = null;

    private ImageView shoutImageView = null;

    private File shoutPhotoFile = null;

    private String shoutPhotoPath = null;

    private Menu menu = null;

    private View mapView = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_shout);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        aq = new AQuery(this);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        clientManager = new AmazonClientManager(getSharedPreferences(
                "com.streetshout.android", Context.MODE_PRIVATE));

        Location savedInstanceStateShoutLocation = null;

        if (savedInstanceState != null) {
            shoutPhotoFile = (File) savedInstanceState.getSerializable("highResCameraPictureFile");
            savedInstanceStateShoutLocation = savedInstanceState.getParcelable("shoutLocation");
        }

        shoutLocation = getIntent().getParcelableExtra("myLocation");

        if (savedInstanceStateShoutLocation != null) {
            shoutLocation = savedInstanceStateShoutLocation;
            shoutLocationRefined = true;
        }

        //Set user name if we have it
        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        final EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        descriptionView.setHorizontallyScrolling(false);
        descriptionView.setMaxLines(MAX_SHOUT_DESCR_LINES);

        appPrefs = ((StreetShoutApplication) getApplicationContext()).getAppPrefs();

        String savedUserName = appPrefs.getUserNamePref();

        InputMethodManager imm = (InputMethodManager)this.getSystemService(Service.INPUT_METHOD_SERVICE);

        if (savedUserName.length() > 0) {
            userNameView.setText(savedUserName);
            descriptionView.requestFocus();
            imm.showSoftInput(descriptionView, 0);
        } else {
            userNameView.requestFocus();
            imm.showSoftInput(userNameView, 0);
        }

        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                descriptionView.setError(null);
                setCharCountItemTitle(String.format("%d", Constants.MAX_DESCRIPTION_LENGTH - s.length()));
            }
        });

        removePhotoButton = (ImageView) findViewById(R.id.remove_photo_button);

        removePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePhoto();
            }
        });

        flipPhotoButton = (ImageView) findViewById(R.id.flip_photo_button);

        flipPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipPhoto();
            }
        });

        shoutImageView = (ImageView) findViewById(R.id.new_shout_upload_photo);

        RelativeLayout newShouPhotoWrapper = (RelativeLayout) findViewById(R.id.new_shout_photo_wrapper);
        resizeSquareOptionalViews(newShouPhotoWrapper);
    }

    private void removePhoto() {
        //Prevents from sending photo with shout
        photoUrl = null;

        shoutImageView.setImageResource(R.drawable.ic_photo);
        removePhotoButton.setVisibility(View.GONE);
        flipPhotoButton.setVisibility(View.GONE);
    }

    private void flipPhoto() {
        Bitmap flippedBitmap = ImageUtils.rotateImage(((BitmapDrawable) shoutImageView.getDrawable()).getBitmap());

        shoutImageView.setImageBitmap(flippedBitmap);

        ImageUtils.storeBitmapInFile(shoutPhotoPath, flippedBitmap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMap == null) {
            setUpMap();
        }
    }

    private void resizeSquareOptionalViews(View view) {
        int marginLeft = 10;
        int marginRight = 10;
        int middleSpace = 40;

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = (width / 2) - marginLeft - marginRight - middleSpace;
        view.setLayoutParams(params);
    }

    private void setUpMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.refine_location_map);
        mMap = mapFragment.getMap();

        mapView = mapFragment.getView();

        //We want a square map
        resizeSquareOptionalViews(mapView);

        if (!shoutLocationRefined) {
            mapView.setVisibility(View.INVISIBLE);
        }

        //Set map settings
        UiSettings settings = mMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setRotateGesturesEnabled(false);
        settings.setTiltGesturesEnabled(false);
        settings.setScrollGesturesEnabled(false);
        settings.setZoomGesturesEnabled(false);

        updateShoutMarkerLocation();
        setUpCameraPosition();
    }

    public void refineShoutLocation(View view) {
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Intent newShoutNextStep = new Intent(CreateShoutActivity.this, RefineShoutLocationActivity.class);

        if (shoutLocationRefined) {
            newShoutNextStep.putExtra("shoutRefinedLocation", shoutLocation);
        } else {
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
        mMap.addMarker(marker);
    }

    public void validateShoutInfo() {
        boolean errors = false;

        EditText userNameView = (EditText) findViewById(R.id.shout_descr_dialog_name);
        EditText descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        userNameView.setError(null);
        descriptionView.setError(null);

        userName = userNameView.getText().toString();
        shoutDescription = descriptionView.getText().toString();

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

            uploadImageBeforeCreatingShout();
        }
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        //Activity often gets destroyed when taking a picture
        if (shoutPhotoFile != null) {
            savedInstanceState.putSerializable("highResCameraPictureFile", shoutPhotoFile);

            if (shoutLocationRefined == true) {
                savedInstanceState.putParcelable("shoutLocation", shoutLocation);
            }
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.NEW_SHOUT_CONTENT_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                mapView.setVisibility(View.VISIBLE);

                shoutLocation = data.getParcelableExtra("accurateShoutLocation");
                updateShoutMarkerLocation();
                updateCameraPosition();
                shoutLocationRefined = true;
            }
        }

        if (requestCode == Constants.UPLOAD_PHOTO_REQUEST) {
            shoutImageView.setEnabled(true);
            if (resultCode == RESULT_OK) {

                boolean photoTakenWithCamera = false;


                shoutPhotoPath = shoutPhotoFile.getAbsolutePath();

                if (shoutPhotoPath != null) {
                    Bitmap formattedPicture = null;

                    //Case where image chosen with camera -> image is already in shoutPhotoFile
                    if (data == null || data.getData() == null) {
                        photoTakenWithCamera = true;
                        formattedPicture = ImageUtils.decodeFileAndShrinkAndMakeSquareBitmap(shoutPhotoPath);
                    //Case where image chosen with library
                    } else {
                        String libraryPhotoPath = ImageUtils.getPathFromUri(this, data.getData());
                        formattedPicture = ImageUtils.decodeFileAndShrinkAndMakeSquareBitmap(libraryPhotoPath);
                    }

                    shoutImageView.setImageBitmap(formattedPicture);

                    //Save the small res image in the shoutPhotoPath, do not alter library photos
                    ImageUtils.storeBitmapInFile(shoutPhotoPath, formattedPicture);

                    removePhotoButton.setVisibility(View.VISIBLE);
                    flipPhotoButton.setVisibility(View.VISIBLE);

                    photoName = GeneralUtils.getDeviceId(this) + "--" + (new Date()).getTime();
                    photoUrl = Constants.S3_URL + photoName;

                    //Save the small res image, otherwise we get memory crashes
                    if (photoTakenWithCamera) {
                        ImageUtils.savePictureToGallery(this, shoutPhotoPath);
                    }
                } else {
                    Toast toast = Toast.makeText(this, this.getString(R.string.photo_not_found), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
    }

    public void letUserChooseImage(View view) {
        //Avoid double clicking (crash)
        shoutImageView.setEnabled(false);

        if (ImageUtils.isSDPresent() == false){
            Toast toast = Toast.makeText(this, this.getString(R.string.no_sd_card), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        if (shoutPhotoFile == null) {
            shoutPhotoFile = ImageUtils.getFileToStoreImage();
        }

        if (shoutPhotoFile == null) {
            Toast toast = Toast.makeText(this, this.getString(R.string.no_space_picture), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Intent chooserIntent = ImageUtils.getPhotoChooserIntent(this, shoutPhotoFile);

        startActivityForResult(chooserIntent, Constants.UPLOAD_PHOTO_REQUEST);
    }

    private class ValidateCredentialsTask extends
            AsyncTask<Void, Void, Response> {

        protected Response doInBackground(Void... params) {
            return CreateShoutActivity.clientManager.validateCredentials();
        }

        protected void onPostExecute(Response response) {
            if (response != null && response.requestWasSuccessful()) {
                final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        if (S3.addImageInBucket(shoutPhotoPath, photoName)) {
                            return "success";
                        } else {
                            return "failure";
                        }
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if (result.equals("success")) {
                            createNewShoutFromInfo();
                        } else {
                            shoutCreationFailed();
                        }
                    }
                };
                task.execute((Void[])null);

                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (task.getStatus() == Status.RUNNING) {
                            task.cancel(true);
                            shoutCreationFailed();
                        }
                    }
                }, 30000);
            }
        }
    }

    public void uploadImageBeforeCreatingShout() {
        createShoutDialog = ProgressDialog.show(this, "", getString(R.string.shout_processing), false);

        if (photoUrl != null) {
            new ValidateCredentialsTask().execute();
        } else {
            createNewShoutFromInfo();
        }
    }

    public void createNewShoutFromInfo() {
        ApiUtils.createShout(this, aq, shoutLocation.getLatitude(), shoutLocation.getLongitude(), userName, shoutDescription, photoUrl, new AjaxCallback<JSONObject>() {
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
                    returnIntent.putExtra("newShout", Shout.rawShoutToInstance(rawShout));
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    shoutCreationFailed();
                }
            }
        });
    }

    public void shoutCreationFailed() {
        createShoutDialog.cancel();
        Toast toast = Toast.makeText(CreateShoutActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_shout_actions, menu);

        MenuItem item = menu.findItem(R.id.action_shout);
        if (Constants.PRODUCTION) {
            item.setTitle("SHOUT!");
        } else {
            item.setTitle("SHATTE!");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_shout) {
            validateShoutInfo();
            return false;
        } else if (item.getItemId() == R.id.char_count) {
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }

    private void setCharCountItemTitle(String title)
    {
        MenuItem item = menu.findItem(R.id.char_count);
        item.setTitle(title);
    }

}