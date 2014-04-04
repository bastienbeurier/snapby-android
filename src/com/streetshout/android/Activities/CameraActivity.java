package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.streetshout.android.R;
import com.streetshout.android.custom.CameraPreview;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;
import com.streetshout.android.utils.SessionUtils;
import com.streetshout.android.utils.StreetShoutApplication;
import com.streetshout.android.utils.TrackingUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by bastien on 2/24/14.
 */
public class CameraActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private Camera mCamera = null;

    private CameraPreview mPreview = null;

    public static final int MEDIA_TYPE_IMAGE = 1;

    private boolean frontCamera = true;

    private FrameLayout preview = null;

    private LocationClient mLocationClient = null;

    private Location myLocation = null;

    private LocationRequest mLocationRequest = null;

    private LocationManager locationManager = null;

    public static final int UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    private ImageView exploreButton = null;

    private ImageView profileButton = null;

    private FrameLayout cameraBottomBar = null;

    //Create shout variables

    private Location shoutLocation = null;

    private boolean shoutLocationRefined = false;

    private ProgressDialog createShoutDialog;

    private EditText descriptionView = null;

    private ImageView sendButton = null;

    private ImageView refineButton = null;

    private ImageView anonymousButton = null;

    private TextView descriptionCharCount = null;

    private LinearLayout createBottomBar = null;

    private ImageView cancelButton = null;

    private ImageView shoutImageView = null;

    private boolean anonymousUser = false;

    private Bitmap formattedPicture = null;

    private ViewTreeObserver.OnGlobalLayoutListener layoutListener = null;

    private View root = null;

    private boolean creationMode = false;

    private TextView activitiesUnreadCount = null;

    private FrameLayout activitiesUnreadCountContainer = null;

    private AppPreferences appPrefs = null;

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        mLocationRequest = LocationUtils.createLocationRequest(LocationRequest.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_IN_MILLISECONDS);

        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (statusCode == ConnectionResult.SUCCESS) {
            mLocationClient = new LocationClient(this, this, this);
        } else {
            LocationUtils.googlePlayServicesFailure(this);
        }

        exploreButton = (ImageView) findViewById(R.id.camera_explore_button);
        profileButton = (ImageView) findViewById(R.id.camera_profile_button);
        cameraBottomBar = (FrameLayout) findViewById(R.id.camera_bottom_bar);
        createBottomBar = (LinearLayout) findViewById(R.id.create_bottom_bar);
        cancelButton = (ImageView) findViewById(R.id.create_cancel_button);
        descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        sendButton = (ImageView) findViewById(R.id.create_send_button);
        refineButton = (ImageView) findViewById(R.id.create_refine_button);
        anonymousButton = (ImageView) findViewById(R.id.create_mask_button);
        descriptionCharCount = (TextView) findViewById(R.id.create_description_count_text);
        shoutImageView = (ImageView) findViewById(R.id.create_shout_image);
        activitiesUnreadCount = (TextView) findViewById(R.id.camera_unread_count);
        activitiesUnreadCountContainer = (FrameLayout) findViewById(R.id.camera_unread_count_container);

        appPrefs = ((StreetShoutApplication) this.getApplicationContext()).getAppPrefs();

        //Front camera button
        ImageView flipCameraView = (ImageView) findViewById(R.id.camera_flip_button);
        if (Camera.getNumberOfCameras() > 1 ) {
            flipCameraView.setVisibility(View.VISIBLE);

            flipCameraView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    frontCamera = !frontCamera;

                    if (frontCamera) {
                        setUpCamera(0);
                    } else {
                        setUpCamera(1);
                    }
                }
            });
        }

        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exploreIntent = new Intent(CameraActivity.this, ExploreActivity.class);
                if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    exploreIntent.putExtra("myLocation", myLocation);
                }

                startActivityForResult(exploreIntent, Constants.EXPLORE_REQUEST);
            }
        });

        profileButton = (ImageView) findViewById(R.id.camera_profile_button);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profile = new Intent(CameraActivity.this, ProfileActivity.class);

                if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    profile.putExtra("myLocation", myLocation);
                }

                startActivityForResult(profile, Constants.PROFILE_REQUEST);
            }
        });

        preview = (FrameLayout) findViewById(R.id.camera_preview);

        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (myLocation != null && myLocation.getLongitude() != 0 && myLocation.getLatitude() != 0 && mCamera != null) {
                            // get an image from the camera
                            mCamera.takePicture(null, null, mPicture);
                        } else if (mCamera == null) {
                            Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.no_camera), Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                }
        );

        //Hack so that the window doesn't resize when descriptionView is clicked
        descriptionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descriptionView.setVisibility(View.GONE);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateShoutInfo();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quitCreationMode();
            }
        });

        refineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refineShoutLocation();
            }
        });

        anonymousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anonymousUser = !anonymousUser;

                if (anonymousUser) {
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button_pressed));
                    Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.anonymous_mode_enabled), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button));
                    Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.anonymous_mode_disabled), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                descriptionView.setError(null);
                descriptionCharCount.setText(String.format("%d", Constants.MAX_DESCRIPTION_LENGTH - s.length()));
            }
        });

        root = findViewById(R.id.camera_activity_frame);

        layoutListener = new ViewTreeObserver.OnGlobalLayoutListener(){
            public void onGlobalLayout(){
                Rect r = new Rect();
                root.getWindowVisibleDisplayFrame(r);

                int windowHeight = root.getRootView().getHeight();
                int heightDiff = windowHeight - (r.bottom - r.top);

                createBottomBar.setY(windowHeight - heightDiff - createBottomBar.getHeight());

                if (heightDiff > 150) {
                    descriptionView.setVisibility(View.VISIBLE);
                }
            }
        };

        if (getIntent().hasExtra("notificationShout")) {
            Intent explore = new Intent(this, ExploreActivity.class);
            explore.putExtra("notificationShout", getIntent().getStringExtra("notificationShout"));
            startActivityForResult(explore, Constants.EXPLORE_REQUEST);
        }  else if (getIntent().hasExtra("notificationUser")) {
            Intent profile = new Intent(this, ProfileActivity.class);
            profile.putExtra("userId", Integer.parseInt(getIntent().getStringExtra("notificationUser")));
            startActivityForResult(profile, Constants.PROFILE_REQUEST);
        }
    }

    private void setUpCamera(int cameraId) {
        releaseCamera();

        // Create an instance of Camera
        mCamera = getCameraInstance(cameraId);

        if (mCamera == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_camera), Toast.LENGTH_SHORT);
            toast.show();

            return;
        }

        //Portrait mode
        mCamera.setDisplayOrientation(90);

        //Get optimal camera size for screen aspect ratio and min resolution
        Camera.Parameters cameraParameters = mCamera.getParameters();

        //Set continuous autofocus
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        try {
            mCamera.setParameters(cameraParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mPreview != null) {
            preview.removeView(mPreview);
        }

        //Get size information on preview
        int previewWidth = Math.min(cameraParameters.getPreviewSize().height, cameraParameters.getPreviewSize().width);
        int previewHeight = Math.max(cameraParameters.getPreviewSize().height, cameraParameters.getPreviewSize().width);
        float previewRatio = ((float) previewWidth)/previewHeight;

        //Get size information on window
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenWidth = size.x;
        int screenHeight = size.y;
        float screenRatio = ((float) screenWidth)/screenHeight;

        //Create Preview view
        mPreview = new CameraPreview(this, mCamera);
        ViewGroup.LayoutParams params = null;

        //Set preview size so it doesn't strech (equivalent of a center crop)
        if (previewRatio > screenRatio) {
            params = new ViewGroup.LayoutParams((int) (screenHeight * previewRatio), screenHeight);
            mPreview.setX(-(params.width - screenWidth)/2);
        } else {
            params = new ViewGroup.LayoutParams(screenWidth, (int) (screenWidth / previewRatio));
            mPreview.setY(-(params.height - screenHeight)/2);
        }

        mPreview.setLayoutParams(params);

        //Set preview as the content of activity.
        preview.addView(mPreview);
    }

    public Camera getCameraInstance(int cameraId) {
        Camera c = null;

        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.no_camera_avaiable), Toast.LENGTH_LONG);
            toast.show();
        }

        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);

            if (pictureFile == null){
                //Check storage permission
                pictureFailed();
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                //File not found
                pictureFailed();
            } catch (IOException e) {
                //Error accessing file
                pictureFailed();
            }

            String imagePath = pictureFile.getAbsolutePath().toString();

            formattedPicture = ImageUtils.decodeFileAndShrinkBitmap(imagePath, Constants.SHOUT_BIG_RES);

            if (formattedPicture.getHeight() < formattedPicture.getWidth()) {
                if (frontCamera) {
                    formattedPicture = ImageUtils.rotateImage(formattedPicture);
                } else {
                    formattedPicture = ImageUtils.reverseRotateImage(formattedPicture);
                    formattedPicture = ImageUtils.mirrorBitmap(formattedPicture);
                }
            }

            galleryAddPic(imagePath);

            startCreationMode();
        }
    };

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        if (!ImageUtils.isSDPresent()){
            pictureFailed();
            return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Shout");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                //Failed to create directory
                pictureFailed();
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "SHOUT_IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!creationMode) {
            releaseCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        displayUnreadActivitiesCount(appPrefs);

        SessionUtils.synchronizeUserInfo(this, myLocation);

        LocationUtils.checkLocationServicesEnabled(this, locationManager);

        if (!creationMode) {
            if (frontCamera) {
                setUpCamera(0);
            } else {
                setUpCamera(1);
            }
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    private void pictureFailed() {
        Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.picture_failed), Toast.LENGTH_LONG);
        toast.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REFINE_LOCATION_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                shoutLocation = data.getParcelableExtra("accurateShoutLocation");
                shoutLocationRefined = true;
            }
        }

        if (requestCode == Constants.PROFILE_REQUEST) {
            activitiesUnreadCount.setVisibility(View.GONE);
            activitiesUnreadCountContainer.setVisibility(View.GONE);
            appPrefs.setLastActivitiesRead();

            if (resultCode == RESULT_OK) {
                if (data.hasExtra("notificationShout")) {
                    Intent redirectToShout = new Intent(CameraActivity.this, ExploreActivity.class);
                    redirectToShout.putExtra("newShout", data.getParcelableExtra("notificationShout"));
                    if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        redirectToShout.putExtra("myLocation", myLocation);
                    }
                    startActivityForResult(redirectToShout, Constants.EXPLORE_REQUEST);
                }
            }
        }
    }

    /**
     *  Location-related methods
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        if (mLocationClient != null) {
            mLocationClient.connect();
        }
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (mLocationClient != null) {
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(this);
            }

            mLocationClient.disconnect();
        }

        super.onStop();
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {

        Location lastLocation = mLocationClient.getLastLocation();

        if (lastLocation != null) {
            myLocation = lastLocation;
        } else {
            myLocation = LocationUtils.getLastLocationWithLocationManager(this, locationManager);
        }

        // Display the connection status
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        LocationUtils.googlePlayServicesFailure(this);
    }

    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        if (location != null) {
            myLocation = location;
        }
    }

    /**
     *  Creation-related methods
     */

    private void startCreationMode() {
        creationMode = true;

        shoutImageView.setImageBitmap(formattedPicture);
        shoutImageView.setVisibility(View.VISIBLE);

        releaseCamera();

        exploreButton.setVisibility(View.GONE);
        profileButton.setVisibility(View.GONE);
        cameraBottomBar.setVisibility(View.GONE);
        createBottomBar.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);

        shoutLocation = myLocation;

        root.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    private void quitCreationMode() {
        creationMode = false;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            root.getViewTreeObserver().removeGlobalOnLayoutListener(layoutListener);
        } else {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }

        if (frontCamera) {
            setUpCamera(0);
        } else {
            setUpCamera(1);
        }

        shoutImageView.setVisibility(View.GONE);

        shoutLocationRefined = false;
        shoutLocation = null;
        descriptionView.setText(null);
        anonymousUser = false;

        anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button));

        createBottomBar.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        exploreButton.setVisibility(View.VISIBLE);
        profileButton.setVisibility(View.VISIBLE);
        cameraBottomBar.setVisibility(View.VISIBLE);
    }

    public void refineShoutLocation() {
        Intent refineIntent = new Intent(this, RefineLocationActivity.class);

        if (shoutLocationRefined) {
            refineIntent.putExtra("shoutRefinedLocation", shoutLocation);
        } else {
            refineIntent.putExtra("shoutRefinedLocation", shoutLocation);
        }

        startActivityForResult(refineIntent, Constants.REFINE_LOCATION_ACTIVITY_REQUEST);
    }

    public void validateShoutInfo() {
        if (SessionUtils.getCurrentUser(this).isBlackListed) {
            Toast toast = Toast.makeText(this, getString(R.string.user_blacklisted), Toast.LENGTH_SHORT);
            toast.show();

            return;
        }

        boolean errors = false;

        descriptionView.setError(null);

        if (descriptionView.getText().toString().length() > Constants.MAX_DESCRIPTION_LENGTH) {
            descriptionView.setError(getString(R.string.description_too_long));
            errors = true;
        }

        if (!errors) {
            createShout();
        }
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void shoutCreationFailed() {
        createShoutDialog.cancel();
        Toast toast = Toast.makeText(this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
        toast.show();
    }

    private void createShout() {
        createShoutDialog = ProgressDialog.show(this, "", getString(R.string.shout_processing), false);

        //Convert bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        formattedPicture.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] bmData = stream.toByteArray();
        String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

        ApiUtils.createShout(this, GeneralUtils.getAquery(this), shoutLocation.getLatitude(), shoutLocation.getLongitude(), descriptionView.getText().toString(), anonymousUser, encodedImage, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(CameraActivity.this);
                    return;
                }

                if (status.getError() == null && object != null) {
                    JSONObject rawShout = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawShout = result.getJSONObject("shout");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Shout newShout = Shout.rawShoutToInstance(rawShout);

                    TrackingUtils.trackCreateShout(CameraActivity.this);

                    quitCreationMode();

                    createShoutDialog.cancel();

                    Intent redirectToShout = new Intent(CameraActivity.this, ExploreActivity.class);
                    redirectToShout.putExtra("newShout", newShout);
                    if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                        redirectToShout.putExtra("myLocation", myLocation);
                    }
                    startActivityForResult(redirectToShout, Constants.EXPLORE_REQUEST);
                } else {
                    shoutCreationFailed();
                }
            }
        });
    }

    private void displayUnreadActivitiesCount(AppPreferences appPrefs) {
        long lastReadTime = appPrefs.getLastActivitiesRead();
        long secondsSinceLastRead = 0;

        if (lastReadTime != 0) {
            secondsSinceLastRead = (System.currentTimeMillis() - lastReadTime)/1000;
            //If last read has never been set, set it now
        } else {
            appPrefs.setLastActivitiesRead();
        }

        if (secondsSinceLastRead != 0) {
            ApiUtils.getUnreadActivitiesCount(this, secondsSinceLastRead, new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getCode() == 401) {
                        SessionUtils.logOut(CameraActivity.this);
                        return;
                    }

                    if (status.getError() == null && object != null) {
                        long unreadCount = 0;

                        try {
                            JSONObject result = object.getJSONObject("result");
                            unreadCount = result.getLong("unread_activities_count");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (unreadCount != 0) {
                            activitiesUnreadCount.setText("" + unreadCount);
                            activitiesUnreadCount.setVisibility(View.VISIBLE);
                            activitiesUnreadCountContainer.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }

    }
}