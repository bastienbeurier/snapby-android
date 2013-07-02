package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.streetshout.android.R;
import com.streetshout.android.aws.AmazonClientManager;
import com.streetshout.android.aws.S3;
import com.streetshout.android.tvmclient.Response;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.ImageUtils;
import com.streetshout.android.utils.StreetShoutApplication;

import java.util.Date;

public class NewShoutContentActivity extends Activity {
    private static int MAX_SHOUT_DESCR_LINES = 6;

    private AppPreferences appPrefs = null;

    private ConnectivityManager connectivityManager = null;

    private Uri photoUri = null;

    private String photoPath = null;

    public static AmazonClientManager clientManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_shout_content);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        clientManager = new AmazonClientManager(getSharedPreferences(
                "com.streetshout.android", Context.MODE_PRIVATE));

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

        //TODO: go to next screen

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            //Save user name in prefs
            appPrefs.setUserNamePref(userName);

            String photoUrl = null;

            if (photoPath != null) {
                String photoName = userName + "--" + (new Date()).getTime();
                photoUrl = Constants.S3_URL + photoName;

                new ValidateCredentialsTask().execute(photoName);
            }

            Intent newShoutNextStep = new Intent(NewShoutContentActivity.this, NewShoutLocationActivity.class);
            newShoutNextStep.putExtra("userName", userName);
            newShoutNextStep.putExtra("shoutDescription", shoutDescription);
            newShoutNextStep.putExtra("myLocation", getIntent().getParcelableExtra("myLocation"));
            newShoutNextStep.putExtra("shoutImageUrl", photoUrl);
            startActivityForResult(newShoutNextStep, Constants.NEW_SHOUT_CONTENT_ACTIVITY_REQUEST);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.NEW_SHOUT_CONTENT_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("newShout", data.getParcelableExtra("newShout"));
                setResult(RESULT_OK, returnIntent);
                finish();
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
}