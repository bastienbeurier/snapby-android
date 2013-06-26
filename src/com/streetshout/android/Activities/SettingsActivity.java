package com.streetshout.android.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.Utils.ApiUtils;
import com.streetshout.android.Utils.AppPreferences;
import com.streetshout.android.Utils.GeneralUtils;
import com.streetshout.android.Utils.PushNotifications;

public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Spinner notificationSpinner = null;

    private Spinner distanceSpinner = null;

    private AppPreferences appPrefs = null;

    private AQuery aq = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        appPrefs = new AppPreferences(getApplicationContext());
        aq = new AQuery(this);

        ((Button) findViewById(R.id.rate_me_button)).setText(getString(R.string.rate_me_preference_button) + " (v. " + GeneralUtils.getAppVersion(this) + ")");

        findViewById(R.id.feedback_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(SettingsActivity.this, "NOT YET IMPLEMENTED!!!", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        findViewById(R.id.rate_me_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(SettingsActivity.this, "NOT YET IMPLEMENTED!!!", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        notificationSpinner = (Spinner) findViewById(R.id.notification_preference_spinner);
        ArrayAdapter<CharSequence> notificationAdapter = null;

        int distancePref = appPrefs.getDistanceUnitPref();

        if (distancePref == 1) {
            notificationAdapter = ArrayAdapter.createFromResource(this, R.array.miles_notification_preference_array, android.R.layout.simple_spinner_item);
            notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } else {
            notificationAdapter = ArrayAdapter.createFromResource(this, R.array.meters_notification_preference_array, android.R.layout.simple_spinner_item);
            notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        notificationSpinner.setAdapter(notificationAdapter);
        notificationSpinner.setOnItemSelectedListener(this);

        int notificationPref = appPrefs.getNotificationPref();
        if (notificationPref != -1) {
            notificationSpinner.setSelection(notificationPref);
        } else {
            notificationSpinner.setSelection(3);
        }

        distanceSpinner = (Spinner) findViewById(R.id.distance_preference_spinner);
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(this, R.array.distance_preference_array, android.R.layout.simple_spinner_item);
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(distanceAdapter);
        distanceSpinner.setOnItemSelectedListener(this);

        if (distancePref != -1) {
            distanceSpinner.setSelection(distancePref);
        }
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
            ArrayAdapter<CharSequence> notificationAdapter = null;
            if (pos == 1) {
                notificationAdapter = ArrayAdapter.createFromResource(this, R.array.miles_notification_preference_array, android.R.layout.simple_spinner_item);
                notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            } else {
                notificationAdapter = ArrayAdapter.createFromResource(this, R.array.meters_notification_preference_array, android.R.layout.simple_spinner_item);
                notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            }
            notificationSpinner.setAdapter(notificationAdapter);

            int notificationPref = appPrefs.getNotificationPref();
            if (notificationPref != -1) {
                notificationSpinner.setSelection(notificationPref);
            } else {
                notificationSpinner.setSelection(3);
            }
        }

        if (parent.getId() == R.id.notification_preference_spinner) {
            appPrefs.setNotificationPref(pos);
            ApiUtils.sendDeviceInfo(this, aq, null);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}