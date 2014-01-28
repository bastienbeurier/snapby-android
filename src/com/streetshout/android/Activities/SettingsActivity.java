package com.streetshout.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.facebook.Session;
import com.facebook.Settings;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Spinner distanceSpinner = null;

    private AppPreferences appPrefs = null;

    private EditText settingsUsernameEditText = null;

    private ConnectivityManager connectivityManager = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        appPrefs = new AppPreferences(getApplicationContext());

        settingsUsernameEditText = (EditText) findViewById(R.id.settings_username_editText);

        setCurrentUsername();

        settingsUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    boolean errors = false;

                    settingsUsernameEditText.setError(null);

                    String username = settingsUsernameEditText.getText().toString();

                    if (!GeneralUtils.isValidUsername(username)) {
                        settingsUsernameEditText.setError(getString(R.string.invalid_username_error));
                        errors = true;
                    }

                    if (username.length() < 6 || username.length() > 20) {
                        settingsUsernameEditText.setError(getString(R.string.username_length_error));
                        errors = true;
                    }

                    if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
                        Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                        toast.show();
                    } else if (errors) {
                        setCurrentUsername();
                    } else {
                        settingsUsernameEditText.setError(null);

                        ApiUtils.updateUsername(SettingsActivity.this, GeneralUtils.getAquery(SettingsActivity.this), username, new AjaxCallback<JSONObject>() {
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
                                } else if (status.getError() == null && status.getCode() == 222) {
                                    setCurrentUsername();
                                    Toast toast = Toast.makeText(SettingsActivity.this, getString(R.string.username_taken_error), Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    setCurrentUsername();
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

                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "info@street-shout.com" });
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
                SessionUtils.wipeOffCredentials(SettingsActivity.this);

                Intent welcome = new Intent(SettingsActivity.this, WelcomeActivity.class);
                welcome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(welcome);
                SettingsActivity.this.finish();
            }
        });

        ((TextView) findViewById(R.id.app_name_and_version)).setText(getString(R.string.app_full_name) + " (v." + GeneralUtils.getAppVersion(this) + ")");
    }

    private void setCurrentUsername() {
        settingsUsernameEditText.setText(SessionUtils.getCurrentUser(this).username);
        settingsUsernameEditText.setSelection(settingsUsernameEditText.getText().length());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        return true;
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
}