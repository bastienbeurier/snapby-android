package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.aws.AmazonClientManager;
import com.streetshout.android.aws.S3;
import com.streetshout.android.models.Shout;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import com.streetshout.android.utils.TrackingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

public class CreateActivity extends Activity {
    private AQuery aq;

    private ConnectivityManager connectivityManager = null;

    public static AmazonClientManager clientManager = null;

    private Location shoutLocation = null;

    private boolean shoutLocationRefined = false;

    private ProgressDialog createShoutDialog;

    private String shoutDescription = null;

    private ImageView shoutImageView = null;

    private String shoutPhotoPath = null;

    private EditText descriptionView = null;

    private ImageView cancelButton = null;

    private ImageView sendButton = null;

    private ImageView refineButton = null;

    private ImageView anonymousButton = null;

    private TextView descriptionCharCount = null;

    private FrameLayout photoContainer = null;

    private boolean anonymousUser = false;

    private View buttonContainer = null;

    private Bitmap formattedPicture = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_shout);

        aq = new AQuery(this);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        clientManager = new AmazonClientManager(getSharedPreferences("com.streetshout.android", Context.MODE_PRIVATE));

        Location savedInstanceStateShoutLocation = null;

        if (savedInstanceState != null) {
            savedInstanceStateShoutLocation = savedInstanceState.getParcelable("shoutLocation");
        }

        shoutLocation = getIntent().getParcelableExtra("myLocation");

        if (savedInstanceStateShoutLocation != null) {
            shoutLocation = savedInstanceStateShoutLocation;
            shoutLocationRefined = true;
        }

        descriptionView = (EditText) findViewById(R.id.shout_descr_dialog_descr);
        cancelButton = (ImageView) findViewById(R.id.create_cancel_button);
        sendButton = (ImageView) findViewById(R.id.create_send_button);
        refineButton = (ImageView) findViewById(R.id.create_refine_button);
        anonymousButton = (ImageView) findViewById(R.id.create_anonymous_button);
        photoContainer = (FrameLayout) findViewById(R.id.create_photo_container);
        descriptionCharCount = (TextView) findViewById(R.id.create_description_count_text);
        buttonContainer = findViewById(R.id.create_description_container);

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
                (new File(shoutPhotoPath)).delete();
                finish();
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
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button_pressed_selector));
                    Toast toast = Toast.makeText(CreateActivity.this, getString(R.string.anonymous_mode_enabled), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    anonymousButton.setImageDrawable(getResources().getDrawable(R.drawable.create_anonymous_button_selector));
                    Toast toast = Toast.makeText(CreateActivity.this, getString(R.string.anonymous_mode_disabled), Toast.LENGTH_SHORT);
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

        shoutImageView = (ImageView) findViewById(R.id.new_shout_upload_photo);

        displayImage();

        photoContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            public void onGlobalLayout(){
                Rect r = new Rect();
                photoContainer.getWindowVisibleDisplayFrame(r);

                int windowHeight = photoContainer.getRootView().getHeight();
                int heightDiff = windowHeight - (r.bottom - r.top);

                buttonContainer.setY(windowHeight - heightDiff - buttonContainer.getHeight());

                if (heightDiff > 150) {
                    descriptionView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void displayImage() {
        shoutPhotoPath = getIntent().getStringExtra("imagePath");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        formattedPicture = BitmapFactory.decodeFile(shoutPhotoPath, options);

        shoutImageView.setImageBitmap(formattedPicture);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void refineShoutLocation() {
        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Intent refineIntent = new Intent(CreateActivity.this, RefineLocationActivity.class);

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

        shoutDescription = descriptionView.getText().toString();

        if (shoutDescription.length() > Constants.MAX_DESCRIPTION_LENGTH) {
            descriptionView.setError(getString(R.string.description_too_long));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            createShout();
        }
    }

    private void createShout() {
        createShoutDialog = ProgressDialog.show(this, "", getString(R.string.shout_processing), false);

        galleryAddPic();

        //Convert bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        formattedPicture.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] bmData = stream.toByteArray();
        String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

        ApiUtils.createShout(this, aq, shoutLocation.getLatitude(), shoutLocation.getLongitude(), shoutDescription, anonymousUser, encodedImage, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getCode() == 401) {
                    SessionUtils.logOut(CreateActivity.this);
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

                    TrackingUtils.trackCreateShout(CreateActivity.this);

                    createShoutDialog.cancel();

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("newShout", newShout);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    shoutCreationFailed();
                }
            }
        });
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        //Activity often gets destroyed when taking a picture
        if (shoutLocationRefined == true) {
            savedInstanceState.putParcelable("shoutLocation", shoutLocation);
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REFINE_LOCATION_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                shoutLocation = data.getParcelableExtra("accurateShoutLocation");
                shoutLocationRefined = true;
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(shoutPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void shoutCreationFailed() {
        createShoutDialog.cancel();
        Toast toast = Toast.makeText(CreateActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
        toast.show();
    }
}