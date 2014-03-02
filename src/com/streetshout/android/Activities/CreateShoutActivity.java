package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.aws.AmazonClientManager;
import com.streetshout.android.aws.S3;
import com.streetshout.android.custom.SquareFrameLayout;
import com.streetshout.android.custom.SquareImageView;
import com.streetshout.android.models.Shout;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.TrackingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

public class CreateShoutActivity extends Activity {
    private AQuery aq;

    private ConnectivityManager connectivityManager = null;

    public static AmazonClientManager clientManager = null;

    private Location shoutLocation = null;

    private boolean shoutLocationRefined = false;

    private String photoName = null;

    private String photoUrl = null;

    private ProgressDialog createShoutDialog;

    private String shoutDescription = null;

    private SquareImageView shoutImageView = null;

    private String shoutPhotoPath = null;

    private EditText descriptionView = null;

    private ImageView cancelButton = null;

    private ImageView sendButton = null;

    private ImageView rotateButton = null;

    private ImageView refineButton = null;

    private ImageView anonymousButton = null;

    private TextView descriptionCharCount = null;

    private SquareFrameLayout photoContainer = null;

    private boolean anonymousUser = false;

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
        rotateButton = (ImageView) findViewById(R.id.create_rotate_button);
        refineButton = (ImageView) findViewById(R.id.create_refine_button);
        anonymousButton = (ImageView) findViewById(R.id.create_anonymous_button);
        photoContainer = (SquareFrameLayout) findViewById(R.id.create_photo_container);
        descriptionCharCount = (TextView) findViewById(R.id.create_description_count_text);

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

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipPhoto();
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
                    ImageUtils.setBackground(CreateShoutActivity.this, anonymousButton, R.drawable.create_anonymous_button_pressed_selector);
                    Toast toast = Toast.makeText(CreateShoutActivity.this, getString(R.string.anonymous_mode_enabled), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    ImageUtils.setBackground(CreateShoutActivity.this, anonymousButton, R.drawable.create_anonymous_button_selector);
                    Toast toast = Toast.makeText(CreateShoutActivity.this, getString(R.string.anonymous_mode_disabled), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        descriptionView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                descriptionView.setError(null);
                descriptionCharCount.setText(String.format("%d", Constants.MAX_DESCRIPTION_LENGTH - s.length()));
            }
        });

        shoutImageView = (SquareImageView) findViewById(R.id.new_shout_upload_photo);

        displayImage();

        final View root = findViewById(R.id.create_root_view);

        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
            public void onGlobalLayout(){
                findViewById(R.id.create_description_container).setY(Math.min(photoContainer.getHeight() - descriptionView.getHeight(),
                                                                       root.getHeight() - descriptionView.getHeight()));
            }
        });
    }

    private void displayImage() {
        shoutPhotoPath = getIntent().getStringExtra("imagePath");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap formattedPicture = BitmapFactory.decodeFile(shoutPhotoPath, options);

        shoutImageView.setImageBitmap(formattedPicture);

        photoName = GeneralUtils.getDeviceId(this) + "--" + (new Date()).getTime();
        photoUrl = Constants.S3_URL + photoName;
    }

    private void flipPhoto() {
        Bitmap flippedBitmap = ImageUtils.rotateImage(((BitmapDrawable) shoutImageView.getDrawable()).getBitmap());

        shoutImageView.setImageBitmap(flippedBitmap);

        ImageUtils.storeBitmapInFile(shoutPhotoPath, flippedBitmap);
    }

    @Override
    protected void onResume() {
        super.onResume();

        InputMethodManager imm = (InputMethodManager)this.getSystemService(Service.INPUT_METHOD_SERVICE);

        descriptionView.requestFocus();

        imm.showSoftInput(descriptionView, 0);

        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
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

        Intent refineIntent = new Intent(CreateShoutActivity.this, RefineShoutLocationActivity.class);

        if (shoutLocationRefined) {
            refineIntent.putExtra("shoutRefinedLocation", shoutLocation);
        } else {
            refineIntent.putExtra("shoutRefinedLocation", shoutLocation);
        }

        startActivityForResult(refineIntent, Constants.REFINE_LOCATION_ACTIVITY_REQUEST);
    }

    public void validateShoutInfo() {
        boolean errors = false;

        descriptionView.setError(null);

        shoutDescription = descriptionView.getText().toString();

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
            uploadImageBeforeCreatingShout();
        }
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
        ApiUtils.createShout(this, aq, shoutLocation.getLatitude(), shoutLocation.getLongitude(), shoutDescription, photoUrl, anonymousUser, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                if (status.getError() == null && object != null) {
                    JSONObject rawShout = null;

                    try {
                        JSONObject result = object.getJSONObject("result");
                        rawShout = result.getJSONObject("shout");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Shout newShout = Shout.rawShoutToInstance(rawShout);

                    TrackingUtils.trackCreateShout(CreateShoutActivity.this);

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

    public void shoutCreationFailed() {
        createShoutDialog.cancel();
        Toast toast = Toast.makeText(CreateShoutActivity.this, getString(R.string.create_shout_failure), Toast.LENGTH_LONG);
        toast.show();
    }
}