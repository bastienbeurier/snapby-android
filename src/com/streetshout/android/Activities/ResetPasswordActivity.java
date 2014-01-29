package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 1/27/14.
 */
public class ResetPasswordActivity extends Activity {

    private EditText forgotPasswordEmailEditText = null;

    private ConnectivityManager connectivityManager = null;

    private ProgressDialog resetPasswordDialog = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        forgotPasswordEmailEditText = (EditText) findViewById(R.id.forgot_password_email_editText);

        forgotPasswordEmailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ResetPasswordActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        //Set focus on email EditText
        forgotPasswordEmailEditText.requestFocus();
    }

    private void validateEmail() {
        boolean errors = false;

        String email = forgotPasswordEmailEditText.getText().toString();

        forgotPasswordEmailEditText.setError(null);

        if (!GeneralUtils.isValidEmail(email)) {
            forgotPasswordEmailEditText.setError(getString(R.string.invalid_email_error));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            sendResetPasswordInstructions(email);
        }
    }

    private void sendResetPasswordInstructions(String email) {
        resetPasswordDialog = ProgressDialog.show(this, "", getString(R.string.reset_password_processing), false);

        ApiUtils.sendResetPasswordInstructions(GeneralUtils.getAquery(this), email,new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                resetPasswordDialog.cancel();

                if (status.getError() == null && object != null && status.getCode() != 222) {
                    Toast toast = Toast.makeText(ResetPasswordActivity.this, getString(R.string.reset_password_success), Toast.LENGTH_SHORT);
                    toast.show();

                     finish();
                } else if (status.getError() == null && status.getCode() == 222) {
                    Toast toast = Toast.makeText(ResetPasswordActivity.this, getString(R.string.email_not_found), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(ResetPasswordActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reset_menu, menu);

        MenuItem item = menu.findItem(R.id.action_reset);
        item.setTitle("Reset");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_reset) {
            validateEmail();
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }
}