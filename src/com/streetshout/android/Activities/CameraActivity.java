package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.streetshout.android.R;
import com.streetshout.android.custom.CameraPreview;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.SessionUtils;
import com.streetshout.android.utils.StreetShoutApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
public class CameraActivity extends Activity {
    private Camera mCamera = null;

    private CameraPreview mPreview = null;

    public static final int MEDIA_TYPE_IMAGE = 1;

    private boolean frontCamera = true;

    private int imageCamera = 0;

    private FrameLayout preview = null;

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

    private String imagePath = null;


    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        exploreButton = (ImageView) findViewById(R.id.camera_explore_button);
        profileButton = (ImageView) findViewById(R.id.camera_profile_button);
        cameraBottomBar = (FrameLayout) findViewById(R.id.camera_bottom_bar);
        createBottomBar = (LinearLayout) findViewById(R.id.create_bottom_bar);
        cancelButton = (ImageView) findViewById(R.id.create_cancel_button);
        sendButton = (ImageView) findViewById(R.id.create_send_button);
        refineButton = (ImageView) findViewById(R.id.create_refine_button);
        anonymousButton = (ImageView) findViewById(R.id.create_mask_button);
        shoutImageView = (ImageView) findViewById(R.id.create_shout_image);
        activitiesUnreadCount = (TextView) findViewById(R.id.camera_shout_count);
        activitiesUnreadCountContainer = (FrameLayout) findViewById(R.id.camera_shout_count_container);

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
                //Redo explore

//                if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
//                    Intent exploreIntent = new Intent(CameraActivity.this, ExploreActivity.class);
//                    exploreIntent.putExtra("myLocation", myLocation);
//                    startActivityForResult(exploreIntent, Constants.EXPLORE_REQUEST);
//                } else {
//                    Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.waiting_for_location), Toast.LENGTH_LONG);
//                    toast.show();
//                }
            }
        });

        profileButton = (ImageView) findViewById(R.id.camera_profile_button);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Redo profile

//                Intent profile = new Intent(CameraActivity.this, ProfileActivity.class);
//
//                if (myLocation != null && myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
//                    profile.putExtra("myLocation", myLocation);
//                }
//
//                startActivityForResult(profile, Constants.PROFILE_REQUEST);
            }
        });

        preview = (FrameLayout) findViewById(R.id.camera_preview);

        Button captureButton = (Button) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Redo capture

//                        if (myLocation != null && myLocation.getLongitude() != 0 && myLocation.getLatitude() != 0 && mCamera != null) {
//                            // get an image from the camera
//                            mCamera.takePicture(null, null, mPicture);
//                        } else if (mCamera == null) {
//                            Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.no_camera), Toast.LENGTH_SHORT);
//                            toast.show();
//                        } else {
//                            Toast toast = Toast.makeText(CameraActivity.this, getString(R.string.no_location), Toast.LENGTH_LONG);
//                            toast.show();
//                        }
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

        if (getIntent().hasExtra("notificationShoutId")) {
            final ProgressDialog progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), false);

            ApiUtils.getShout(GeneralUtils.getAquery(this), getIntent().getIntExtra("notificationShoutId", 0), new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    super.callback(url, object, status);

                    if (status.getError() == null && object != null) {

                        Shout shout = null;

                        try {
                            JSONObject result = object.getJSONObject("result");
                            JSONObject rawShout = result.getJSONObject("shout");
                            shout = Shout.rawShoutToInstance(rawShout);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent displayShout = new Intent(CameraActivity.this, DisplayActivity.class);
                        displayShout.putExtra("shout", shout);
                        displayShout.putExtra("expiredShout", true);

                        CameraActivity.this.startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
                    } else {
                        Toast toast = Toast.makeText(CameraActivity.this, CameraActivity.this.getString(R.string.failed_to_retrieve_shout), Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    progressDialog.cancel();
                }
            });
        } else if (getIntent().hasExtra("notificationShout")) {
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

            imagePath = pictureFile.getAbsolutePath().toString();

            formattedPicture = ImageUtils.decodeFileAndShrinkBitmap(imagePath, Constants.SHOUT_BIG_RES);

            if (formattedPicture.getHeight() < formattedPicture.getWidth()) {
                if (frontCamera) {
                    imageCamera = 0;
                    formattedPicture = ImageUtils.rotateImage(formattedPicture);
                } else {
                    imageCamera = 1;
                    formattedPicture = ImageUtils.reverseRotateImage(formattedPicture);
                    formattedPicture = ImageUtils.mirrorBitmap(formattedPicture);
                }
            }

            startCreationMode();
        }
    };

    private void saveImageToGallery(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        if (imageCamera == 0) {
            bitmap = ImageUtils.rotateImage(bitmap);
        } else {
            bitmap = ImageUtils.reverseRotateImage(bitmap);
            bitmap = ImageUtils.mirrorBitmap(bitmap);
        }

        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(imagePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);

        galleryAddPic(imagePath);

        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

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
//        if (requestCode == Constants.REFINE_LOCATION_ACTIVITY_REQUEST) {
//            if (resultCode == RESULT_OK) {
//                shoutLocation = data.getParcelableExtra("accurateShoutLocation");
//                shoutLocationRefined = true;
//            }
//        }
//
//        if (requestCode == Constants.PROFILE_REQUEST) {
//            activitiesUnreadCount.setVisibility(View.GONE);
//            activitiesUnreadCountContainer.setVisibility(View.GONE);
//            appPrefs.setLastActivitiesRead();
//
//            if (resultCode == RESULT_OK) {
//                if (data.hasExtra("notificationShout")) {
//                    Intent redirectToShout = new Intent(CameraActivity.this, ExploreActivity.class);
//                    redirectToShout.putExtra("newShout", data.getParcelableExtra("notificationShout"));
//                    if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
//                        redirectToShout.putExtra("myLocation", myLocation);
//                    }
//                    startActivityForResult(redirectToShout, Constants.EXPLORE_REQUEST);
//                }
//            }
//        }
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

//        ApiUtils.createShout(this, GeneralUtils.getAquery(this), shoutLocation.getLatitude(), shoutLocation.getLongitude(), descriptionView.getText().toString(), anonymousUser, encodedImage, new AjaxCallback<JSONObject>() {
//            @Override
//            public void callback(String url, JSONObject object, AjaxStatus status) {
//                super.callback(url, object, status);
//
//                if (status.getCode() == 401) {
//                    SessionUtils.logOut(CameraActivity.this);
//                    return;
//                }
//
//                if (status.getError() == null && object != null) {
//                    JSONObject rawShout = null;
//
//                    try {
//                        JSONObject result = object.getJSONObject("result");
//                        rawShout = result.getJSONObject("shout");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                    Shout newShout = Shout.rawShoutToInstance(rawShout);
//
//                    TrackingUtils.trackCreateShout(CameraActivity.this);
//
//                    quitCreationMode();
//
//                    createShoutDialog.cancel();
//
//                    Intent redirectToShout = new Intent(CameraActivity.this, ExploreActivity.class);
//                    redirectToShout.putExtra("newShout", newShout);
//                    if (myLocation != null & myLocation.getLatitude() != 0 && myLocation.getLongitude() != 0) {
//                        redirectToShout.putExtra("myLocation", myLocation);
//                    }
//                    startActivityForResult(redirectToShout, Constants.EXPLORE_REQUEST);
//                } else {
//                    shoutCreationFailed();
//                }
//            }
//        });

        if (imagePath != null) {
            SaveImageToGallery runner = new SaveImageToGallery();
            runner.execute(imagePath);
        }
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

    private class SaveImageToGallery extends AsyncTask<String, String, String> {
        protected String doInBackground(String... params) {
            saveImageToGallery(params[0]);
            return "";
        }
    }

//    private void updateLocalShoutCount() {
//        ApiUtils.getLocalShoutsCount(this, new AjaxCallback<JSONObject>() {
//            @Override
//            public void callback(String url, JSONObject object, AjaxStatus status) {
//                super.callback(url, object, status);
//
//                if (status.getError() == null && object != null) {
//
//                    Integer shoutCount = 0;
//
//                    try {
//                        JSONObject result = object.getJSONObject("result");
//                        shoutCount = Integer.parseInt(result.getString("shouts_count"));
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
////                    localShoutsCount.setText(shoutCount + " shouts");
////                    localShoutsCount.setVisibility(View.GONE);
//                }
//            }
//        });
//    }
}