package com.snapby.android.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.LatLngBounds;
import com.snapby.android.R;
import com.snapby.android.activities.MainActivity;
import com.snapby.android.custom.CameraPreview;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.AppPreferences;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.ImageUtils;
import com.snapby.android.utils.SessionUtils;
import com.snapby.android.utils.SnapbyApplication;
import com.snapby.android.utils.TrackingUtils;
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
 * Created by bastien on 4/11/14.
 */
public class CameraFragment extends Fragment {

    private Camera mCamera = null;

    private CameraPreview mPreview = null;

    public static final int MEDIA_TYPE_IMAGE = 1;

    private boolean frontCamera = true;

    private FrameLayout preview = null;

    public ImageView exploreButton = null;

    private ImageView profileButton = null;

    private FrameLayout cameraBottomBar = null;

    public Location refinedSnapbyLocation = null;

    private ProgressDialog createSnapbyDialog;

    private ImageView sendButton = null;

    private ImageView refineButton = null;

    private ImageView anonymousButton = null;

    private LinearLayout createBottomBar = null;

    private ImageView cancelButton = null;

    private ImageView snapbyImageView = null;

    private boolean anonymousUser = false;

    private Bitmap formattedPicture = null;

    private View exploreButtonContainer = null;

    private TextView localSnapbyCount = null;

    private FrameLayout localSnapbyCountContainer = null;

    private AppPreferences appPrefs = null;

    private String imagePath = null;

    private boolean firstLocalSnapbyCount = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.camera, container, false);

        exploreButton = (ImageView) rootView.findViewById(R.id.camera_explore_button);
        profileButton = (ImageView) rootView.findViewById(R.id.camera_profile_button);
        cameraBottomBar = (FrameLayout) rootView.findViewById(R.id.camera_bottom_bar);
        createBottomBar = (LinearLayout) rootView.findViewById(R.id.create_bottom_bar);
        cancelButton = (ImageView) rootView.findViewById(R.id.create_cancel_button);
        sendButton = (ImageView) rootView.findViewById(R.id.create_send_button);
        refineButton = (ImageView) rootView.findViewById(R.id.create_refine_button);
        anonymousButton = (ImageView) rootView.findViewById(R.id.create_mask_button);
        snapbyImageView = (ImageView) rootView.findViewById(R.id.create_snapby_image);
        localSnapbyCount = (TextView) rootView.findViewById(R.id.camera_snapby_count);
        localSnapbyCountContainer = (FrameLayout) rootView.findViewById(R.id.camera_snapby_count_container);
        exploreButtonContainer = rootView.findViewById(R.id.camera_explore_button_container);

        appPrefs = ((SnapbyApplication) getActivity().getApplicationContext()).getAppPrefs();

        //Front camera button
        ImageView flipCameraView = (ImageView) rootView.findViewById(R.id.camera_flip_button);
        if (Camera.getNumberOfCameras() > 1 ) {
            flipCameraView.setVisibility(View.VISIBLE);

            flipCameraView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    frontCamera = !frontCamera;

                    setUpCamera();
                }
            });
        }

        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).mainViewPager.setCurrentItem(0);
            }
        });

        profileButton = (ImageView) rootView.findViewById(R.id.camera_profile_button);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).mainViewPager.setCurrentItem(2);
            }
        });

        preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);

        Button captureButton = (Button) rootView.findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSnapby();
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
                refineSnapbyLocation();
            }
        });

        anonymousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anonymousUser = !anonymousUser;

                if (anonymousUser) {
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button_pressed));
                    Toast toast = Toast.makeText(getActivity(), getString(R.string.anonymous_mode_enabled), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button));
                    Toast toast = Toast.makeText(getActivity(), getString(R.string.anonymous_mode_disabled), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        setUpCamera();

        return rootView;
    }

    public void setUpCamera() {
        releaseCamera();

        // Create an instance of Camera
        if (frontCamera) {
            mCamera = getCameraInstance(0);
        } else {
            mCamera = getCameraInstance(1);
        }

        if (mCamera == null) {
            Toast toast = Toast.makeText(getActivity(), getString(R.string.no_camera), Toast.LENGTH_SHORT);
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
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenWidth = size.x;
        int screenHeight = size.y;
        float screenRatio = ((float) screenWidth)/screenHeight;

        //Create Preview view
        mPreview = new CameraPreview(getActivity(), mCamera);
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
            Toast toast = Toast.makeText(getActivity(), getString(R.string.no_camera_avaiable), Toast.LENGTH_LONG);
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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            formattedPicture = BitmapFactory.decodeFile(imagePath, options);

            if (formattedPicture.getHeight() < formattedPicture.getWidth()) {
                if (frontCamera) {
                    formattedPicture = ImageUtils.rotateImage(formattedPicture);
                } else {
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
        getActivity().sendBroadcast(mediaScanIntent);
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
                Environment.DIRECTORY_PICTURES), "Snapby");

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

    private void pictureFailed() {
        Toast toast = Toast.makeText(getActivity(), getString(R.string.picture_failed), Toast.LENGTH_LONG);
        toast.show();
    }

    private void startCreationMode() {
        snapbyImageView.setImageBitmap(formattedPicture);
        preview.setVisibility(View.GONE);
        snapbyImageView.setVisibility(View.VISIBLE);

        exploreButtonContainer.setVisibility(View.GONE);
        profileButton.setVisibility(View.GONE);
        cameraBottomBar.setVisibility(View.GONE);
        createBottomBar.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void quitCreationMode() {
        refinedSnapbyLocation = null;
        snapbyImageView.setVisibility(View.GONE);

        anonymousUser = false;

        anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button));

        createBottomBar.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        exploreButtonContainer.setVisibility(View.VISIBLE);
        profileButton.setVisibility(View.VISIBLE);
        cameraBottomBar.setVisibility(View.VISIBLE);
        preview.setVisibility(View.VISIBLE);

        setUpCamera();
    }

    public void refineSnapbyLocation() {
        Location myLocation = ((MainActivity) getActivity()).myLocation;

        if (refinedSnapbyLocation != null) {
            ((MainActivity) getActivity()).refineSnapbyLocation(refinedSnapbyLocation);
        } else if (myLocation != null && myLocation.getLatitude() != 0 &&  myLocation.getLongitude() != 0) {
            ((MainActivity) getActivity()).refineSnapbyLocation(myLocation);
        } else {
            Toast toast = Toast.makeText(getActivity(), getString(R.string.no_location), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void snapbyCreationFailed() {
        createSnapbyDialog.cancel();
        Toast toast = Toast.makeText(getActivity(), getString(R.string.create_snapby_failure), Toast.LENGTH_LONG);
        toast.show();
    }

    private void createSnapby() {
        Location shoutLocation = null;

        if (refinedSnapbyLocation != null) {
            shoutLocation = refinedSnapbyLocation;
        } else {
            Location myLocation = ((MainActivity) getActivity()).myLocation;

            if (myLocation == null || myLocation.getLatitude() == 0 || myLocation.getLongitude() == 0) {
                Toast toast = Toast.makeText(getActivity(), getString(R.string.no_location), Toast.LENGTH_SHORT);
                toast.show();
                return;
            } else {
                shoutLocation = myLocation;
            }
        }

        createSnapbyDialog = ProgressDialog.show(getActivity(), "", getString(R.string.snapby_processing), false);

        formattedPicture = ImageUtils.getResizedBitmap(formattedPicture, Constants.SHOUT_BIG_RES);

        //Convert bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        formattedPicture.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] bmData = stream.toByteArray();
        String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

        ImageUtils.storeBitmapInFile(imagePath, formattedPicture);

        if (imagePath != null) {
            SaveImageToGallery runner = new SaveImageToGallery();
            runner.execute(imagePath);
        }

        ApiUtils.createSnapby(getActivity(), GeneralUtils.getAquery(getActivity()), shoutLocation.getLatitude(), shoutLocation.getLongitude(), "", anonymousUser, encodedImage, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(getActivity());
                    return;
                }

                if (status.getError() == null && object != null) {
                    TrackingUtils.trackCreateSnapby(getActivity());

                    if (createSnapbyDialog != null) {
                        createSnapbyDialog.cancel();
                    }

                    quitCreationMode();

                    Toast toast = Toast.makeText(getActivity(), getString(R.string.create_snapby_success), Toast.LENGTH_SHORT);
                    toast.show();

                    ((MainActivity) getActivity()).repullSnapbies();
                } else {
                    snapbyCreationFailed();
                }
            }
        });
    }

    private class SaveImageToGallery extends AsyncTask<String, String, String> {
        protected String doInBackground(String... params) {
            saveImageToGallery(params[0]);
            return "";
        }
    }

    public void updateLocalSnapbyCount(LatLngBounds latLngBounds) {
        ApiUtils.getLocalSnapbiesCount(getActivity(), latLngBounds.northeast.latitude, latLngBounds.northeast.longitude, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    Integer snapbyCount = 0;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        snapbyCount = Integer.parseInt(result.getString("snapbies_count"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    localSnapbyCount.setText("" + snapbyCount);
                    localSnapbyCount.setVisibility(View.VISIBLE);
                    localSnapbyCountContainer.setVisibility(View.VISIBLE);

                    if (firstLocalSnapbyCount) {
                        firstLocalSnapbyCount = false;

                        Toast toast = Toast.makeText(getActivity(), "Be #" + (snapbyCount + 1) + " to snapby this area!", Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            }
        });
    }

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

}
