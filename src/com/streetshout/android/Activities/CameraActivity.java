package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.streetshout.android.R;
import com.streetshout.android.custom.CameraPreview;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.LocationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private boolean createShoutRedirect = false;

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

        //Front camera button
        ImageView flipCameraView = (ImageView) findViewById(R.id.camera_flip_button);
        if (Camera.getNumberOfCameras() > 1 ) {
            flipCameraView.setVisibility(View.VISIBLE);

            flipCameraView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (frontCamera) {
                        setUpCamera(1);
                    } else {
                        setUpCamera(0);
                    }

                    frontCamera = !frontCamera;
                }
            });
        }

        ImageView exploreButtonView = (ImageView) findViewById(R.id.camera_explore_button);

        exploreButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exploreIntent = new Intent(CameraActivity.this, ExploreActivity.class);
                if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    exploreIntent.putExtra("myLocation", myLocation);
                }

                startActivityForResult(exploreIntent, Constants.EXPLORE_REQUEST);
            }
        });

        ImageView settingsButtonView = (ImageView) findViewById(R.id.camera_settings_button);

        settingsButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(CameraActivity.this, SettingsActivity.class);
                startActivityForResult(settings, Constants.SETTINGS_REQUEST);
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
    }

    private Camera.Size getOptimalCameraSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;

        Camera.Size optimalSize = null;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (size.width >= Constants.SHOUT_BIG_RES || size.height >= Constants.SHOUT_BIG_RES) {
                optimalSize = size;
            }
        }

        if (optimalSize == null) {
            optimalSize = sizes.get(0);
        }

        return optimalSize;
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
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        View root = findViewById(R.id.camera_activity_frame);
        Camera.Size optimalSize = getOptimalCameraSize(sizes, root.getWidth(), root.getHeight());
        parameters.setPictureSize(optimalSize.width, optimalSize.height);

        //Set continuous autofocus
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.setParameters(parameters);

        if (mPreview != null) {
            preview.removeView(mPreview);
        }

        //Create Preview view
        mPreview = new CameraPreview(this, mCamera);

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

    private void goToCreateShout(String imagePath) {
        Intent createShout = new Intent(this, CreateActivity.class);
        createShout.putExtra("myLocation", myLocation);
        createShout.putExtra("imagePath", imagePath);
        startActivityForResult(createShout, Constants.CREATE_SHOUT_REQUEST);
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

            Bitmap formattedPicture = ImageUtils.decodeFileAndShrinkBitmap(imagePath);

            //TODO: check with Philippe's phone
            if (formattedPicture.getHeight() < formattedPicture.getWidth()) {
                if (frontCamera) {
                    formattedPicture = ImageUtils.rotateImage(formattedPicture);
                } else {
                    formattedPicture = ImageUtils.reverseRotateImage(formattedPicture);
                    formattedPicture = ImageUtils.mirrorBitmap(formattedPicture);
                }
            }

            //Save the small res image in the imagePath
            ImageUtils.storeBitmapInFile(imagePath, formattedPicture);

            goToCreateShout(imagePath);
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
        releaseCamera();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (createShoutRedirect) {
            createShoutRedirect = false;
            return;
        }

        LocationUtils.checkLocationServicesEnabled(this, locationManager);

        if (frontCamera) {
            setUpCamera(0);
        } else {
            setUpCamera(1);
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
        if (requestCode == Constants.CREATE_SHOUT_REQUEST) {
            if (resultCode == RESULT_OK) {
                createShoutRedirect = true;

                Shout shout = data.getParcelableExtra("newShout");

                Intent redirectToShout = new Intent(this, ExploreActivity.class);
                redirectToShout.putExtra("newShout", shout);
                if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
                    redirectToShout.putExtra("myLocation", myLocation);
                }
                startActivityForResult(redirectToShout, Constants.EXPLORE_REQUEST);
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
}