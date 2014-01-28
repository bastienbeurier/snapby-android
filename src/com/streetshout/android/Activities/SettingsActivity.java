package com.streetshout.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.AppPreferences;
import com.streetshout.android.utils.GeneralUtils;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Spinner notificationSpinner = null;

    private Spinner distanceSpinner = null;

    private AppPreferences appPrefs = null;

    private AQuery aq = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);
//        getActionBar().setDisplayShowHomeEnabled(false);

        appPrefs = new AppPreferences(getApplicationContext());
        aq = new AQuery(this);

        ((Button) findViewById(R.id.rate_me_button)).setText(getString(R.string.rate_me_preference_button) + " (v. " + GeneralUtils.getAppVersion(this) + ")");

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

        findViewById(R.id.notification_help_button_wrapper).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setMessage(R.string.notification_preference_dialog_instruction).setTitle(R.string.notification_preference_dialog_title);
                builder.create().show();
            }
        });

        notificationSpinner = (Spinner) findViewById(R.id.notification_preference_spinner);
        setNotificationSpinnerAdapter();
        notificationSpinner.setOnItemSelectedListener(this);

        distanceSpinner = (Spinner) findViewById(R.id.distance_preference_spinner);
        setDistanceSpinnerAdapter();
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

            //Modify the notification spinner according to right distance unit
            setNotificationSpinnerAdapter();
        }

        //0 -> no notification / 1 -> 100m / 2 -> 1km / 3 -> 10km
        if (parent.getId() == R.id.notification_preference_spinner) {
            appPrefs.setNotificationPref(pos);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        //Nothing
    }

    private void setNotificationSpinnerAdapter() {
        ArrayAdapter<CharSequence> notificationAdapter = null;

        int distancePref = appPrefs.getDistanceUnitPref();

        if (distancePref == 1) {
            notificationAdapter = ArrayAdapter.createFromResource(this, R.array.miles_notification_preference_array, R.layout.settings_spinner_item);
            notificationAdapter.setDropDownViewResource(R.layout.settings_spinner_item);
        } else {
            notificationAdapter = ArrayAdapter.createFromResource(this, R.array.meters_notification_preference_array, R.layout.settings_spinner_item);
            notificationAdapter.setDropDownViewResource(R.layout.settings_spinner_item);
        }

        notificationSpinner.setAdapter(notificationAdapter);

        int notificationPref = appPrefs.getNotificationPref();
        if (notificationPref != -1) {
            notificationSpinner.setSelection(notificationPref);
        } else {
            notificationSpinner.setSelection(3);
        }
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