package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.ImageOptions;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.TreeSet;

/**
 * Created by bastien on 3/10/14.
 */
public class ProfileActivity extends Activity {

    private User currentUser = null;

    private ImageView profilePicture = null;
    private TextView username = null;
    private TextView followerCount = null;
    private TextView followingCount = null;
    private LinearLayout followersButton = null;
    private LinearLayout followingButton = null;
    private FrameLayout profilePictureContainer = null;
    private File profilePictureFile = null;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        profilePicture = (ImageView) findViewById(R.id.profile_user_picture);
        username = (TextView) findViewById(R.id.profile_username);
        followerCount = (TextView) findViewById(R.id.profile_follower_count);
        followingCount = (TextView) findViewById(R.id.profile_following_count);
        followersButton = (LinearLayout) findViewById(R.id.profile_followers_button);
        followingButton = (LinearLayout) findViewById(R.id.profile_following_button);
        profilePictureContainer = (FrameLayout) findViewById(R.id.profile_profile_picture_container);


        //TODO change method to get user info
        ApiUtils.getUserInfo(this, SessionUtils.getCurrentUser(this).id, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null) {
                    JSONObject result = null;
                    JSONObject rawUser = null;

                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    currentUser = User.rawUserToInstance(rawUser);

                    ImageOptions options = new ImageOptions();
                    options.round = 4;
                    options.memCache = true;
                    options.animation = AQuery.FADE_IN;

                    GeneralUtils.getAquery(ProfileActivity.this).id(profilePicture).image(currentUser.profilePicture, options);

                    username.setText("@" + currentUser.username);
                }
            }
        });

        findViewById(R.id.profile_find_follow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        followersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        followingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        profilePictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePictureContainer.setEnabled(false);

                letUserChooseImage();
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

    public void letUserChooseImage() {
        if (ImageUtils.isSDPresent() == false){
            Toast toast = Toast.makeText(this, this.getString(R.string.no_sd_card), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(galleryIntent, Constants.PHOTO_GALLERY_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.PHOTO_GALLERY_REQUEST) {
            profilePictureContainer.setEnabled(true);

            if (resultCode == RESULT_OK) {
                Bitmap formattedPicture = null;

                //New Kitkat way of doing things
                if (Build.VERSION.SDK_INT < 19) {
                    String libraryPhotoPath = ImageUtils.getPathFromUri(this, data.getData());
                    formattedPicture = ImageUtils.decodeAndMakeThumb(libraryPhotoPath);
                } else {
                    ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = getContentResolver().openFileDescriptor(data.getData(), "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        formattedPicture = ImageUtils.makeThumb(BitmapFactory.decodeFileDescriptor(fileDescriptor));
                        parcelFileDescriptor.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //Convert bitmap to byte array
                Bitmap bitmap = formattedPicture;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                byte[] bmData = stream.toByteArray();
                String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

                ApiUtils.updateUserInfoWithLocation(ProfileActivity.this, GeneralUtils.getAquery(ProfileActivity.this), null, encodedImage, null, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        if (status.getCode() == 401) {
                            //TODO
                        } else {
                            //TODO ERROR
                        }
                    }
                });

                profilePicture.setImageBitmap(formattedPicture);
            }
        }
    }
}