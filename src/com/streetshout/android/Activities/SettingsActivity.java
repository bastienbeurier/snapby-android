package com.streetshout.android.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Spinner distanceSpinner = null;

    private AppPreferences appPrefs = null;

    private EditText settingsUsernameEditText = null;

    private ImageView profilePictureView = null;

    private FrameLayout profilePictureContainer = null;

    private ConnectivityManager connectivityManager = null;

    private User currentUser = null;

    private boolean profileUpdated = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        appPrefs = new AppPreferences(getApplicationContext());

        currentUser = SessionUtils.getCurrentUser(this);

        settingsUsernameEditText = (EditText) findViewById(R.id.settings_username_editText);
        profilePictureContainer = (FrameLayout) findViewById(R.id.settings_edit_profile_picture_container);
        profilePictureView = (ImageView) findViewById(R.id.settings_profile_picture);

        updateUI();

        profilePictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePictureContainer.setEnabled(false);

                letUserChooseProfilePicture();
            }
        });

        settingsUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    boolean errors = false;

                    settingsUsernameEditText.setError(null);

                    String username = settingsUsernameEditText.getText().toString();

                    if (username.length() < Constants.MIN_USERNAME_LENGTH || username.length() > Constants.MAX_USERNAME_LENGTH) {
                        settingsUsernameEditText.setError(getString(R.string.username_length_error));
                        errors = true;
                    }

                    if (!GeneralUtils.isValidUsername(username)) {
                        settingsUsernameEditText.setError(getString(R.string.invalid_username_error));
                        errors = true;
                    }

                    if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
                        Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                        toast.show();
                    } else if (errors) {
                        updateUI();
                    } else {
                        settingsUsernameEditText.setError(null);

                        ApiUtils.updateUserInfoWithLocation(SettingsActivity.this, GeneralUtils.getAquery(SettingsActivity.this), null, null, username, new AjaxCallback<JSONObject>() {
                            @Override
                            public void callback(String url, JSONObject object, AjaxStatus status) {
                                super.callback(url, object, status);

                                if (status.getError() == null && object != null && status.getCode() != 222) {
                                    JSONObject rawUser = null;

                                    try {
                                        JSONObject result = object.getJSONObject("result");

                                        rawUser = result.getJSONObject("user");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    SessionUtils.updateCurrentUserInfoInPhone(SettingsActivity.this, User.rawUserToInstance(rawUser));

                                    Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.username_successfully_updated), Toast.LENGTH_SHORT);
                                    toast.show();

                                    profileUpdated = true;
                                } else if (status.getError() == null && status.getCode() == 222) {
                                    updateUI();
                                    Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.username_taken_error), Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    updateUI();
                                    Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });
                    }

                }

                return false;
            }
        });

        ((Button) findViewById(R.id.rate_me_button)).setText(getString(R.string.rate_me_preference_button));

        findViewById(R.id.feedback_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("plain/text");

                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "info@shouthereandnow.com" });
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feed_back_mail_title, GeneralUtils.getAppVersion(SettingsActivity.this)));
                startActivity(Intent.createChooser(feedbackIntent, ""));
            }
        });

        findViewById(R.id.rate_me_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = SettingsActivity.this.getApplicationContext();

                Uri uri = Uri.parse("market://details?id=" + ctx.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ctx, getString(R.string.no_google_play_store), Toast.LENGTH_LONG).show();
                }
            }
        });

        distanceSpinner = (Spinner) findViewById(R.id.distance_preference_spinner);
        setDistanceSpinnerAdapter();

        findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionUtils.logOut(SettingsActivity.this);
            }
        });

        ((TextView) findViewById(R.id.app_name_and_version)).setText(getString(R.string.app_full_name) + " (v." + GeneralUtils.getAppVersion(this) + ")");

        if (getIntent().hasExtra("chooseProfilePicture")) {
            letUserChooseProfilePicture();
        }
    }

    private void letUserChooseProfilePicture() {
        if (ImageUtils.isSDPresent() == false){
            Toast toast = Toast.makeText(this, this.getString(R.string.no_sd_card), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        Intent chooserIntent = ImageUtils.getPhotoChooserIntent(this);

        startActivityForResult(chooserIntent, Constants.CHOOSE_PROFILE_PICTURE_REQUEST);
    }

    private void updateUI() {
        settingsUsernameEditText.setText(currentUser.username);

        GeneralUtils.getAquery(this).id(profilePictureView).image(GeneralUtils.getProfilePicturePrefix() + currentUser.id, false, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        finishActivity();
        return true;
    }

    @Override
    public void onBackPressed() {
        finishActivity();
    }

    private void finishActivity() {
        Intent returnIntent = new Intent();
        if (profileUpdated) {
            returnIntent.putExtra("profileUpdated", true);
        }
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        if (parent.getId() == R.id.distance_preference_spinner) {
            appPrefs.setDistanceUnitPref(pos);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        //Nothing
    }

    private void setDistanceSpinnerAdapter() {
        int distancePref = appPrefs.getDistanceUnitPref();

        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this, R.array.distance_preference_array, R.layout.settings_spinner_item);
        distanceAdapter.setDropDownViewResource(R.layout.settings_spinner_item);
        distanceSpinner.setAdapter(distanceAdapter);
        distanceSpinner.setOnItemSelectedListener(this);

        if (distancePref != -1) {
            distanceSpinner.setSelection(distancePref);
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CHOOSE_PROFILE_PICTURE_REQUEST) {
            profilePictureContainer.setEnabled(true);

            if (resultCode == RESULT_OK) {
                Bitmap formattedPicture = null;

                //From camera?
                if (data.hasExtra("data")) {
                    formattedPicture = ImageUtils.makeThumb((Bitmap) data.getExtras().get("data"));
                //From library
                } else {
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
                }

                //Convert bitmap to byte array
                Bitmap bitmap = formattedPicture;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                byte[] bmData = stream.toByteArray();
                String encodedImage = Base64.encodeToString(bmData, Base64.DEFAULT);

                ApiUtils.updateUserInfoWithLocation(SettingsActivity.this, GeneralUtils.getAquery(SettingsActivity.this), null, encodedImage, null, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject object, AjaxStatus status) {
                        super.callback(url, object, status);

                        if (status.getError() == null && object != null && status.getCode() != 222) {
                            JSONObject result = null;
                            JSONObject rawUser = null;


                            try {
                                result = object.getJSONObject("result");

                                rawUser = result.getJSONObject("user");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            currentUser = User.rawUserToInstance(rawUser);
                            SessionUtils.updateCurrentUserInfoInPhone(SettingsActivity.this, currentUser);
                            Toast toast = Toast.makeText(SettingsActivity.this, SettingsActivity.this.getString(R.string.update_profile_picture_success), Toast.LENGTH_LONG);
                            toast.show();
                            updateUI();
                            profileUpdated = true;
                        } else {
                            Toast toast = Toast.makeText(SettingsActivity.this, SettingsActivity.this.getString(R.string.update_profile_picture_failure), Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });
            }
        }
    }
}