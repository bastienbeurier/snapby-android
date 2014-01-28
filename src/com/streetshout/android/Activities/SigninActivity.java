package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 1/27/14.
 */
public class SigninActivity extends Activity {

    private Button signinValidateButton = null;

    private Button resetPasswordButton = null;

    private EditText emailEditText = null;

    private EditText passwordEditText = null;

    private ConnectivityManager connectivityManager = null;

    private ProgressDialog signinDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        emailEditText = (EditText) findViewById(R.id.user_email_editText);
        passwordEditText = (EditText) findViewById(R.id.user_password_editText);

        signinValidateButton = (Button) findViewById(R.id.signin_validate_button);
        resetPasswordButton = (Button) findViewById(R.id.reset_password_button);

        signinValidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateSigninInfo();
            }
        });


        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resetPassword = new Intent(SigninActivity.this, ResetPasswordActivity.class);
                startActivityForResult(resetPassword, Constants.RESET_PASSWORD_REQUEST);
            }
        });

        //Set focus on email EditText
        emailEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    private void validateSigninInfo() {
        boolean errors = false;

        emailEditText.setError(null);
        passwordEditText.setError(null);

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (!GeneralUtils.isValidEmail(email)) {
            emailEditText.setError(getString(R.string.invalid_email_error));
            errors = true;
        }

        if (password.length() < 6 || password.length() > 128) {
            passwordEditText.setError(getString(R.string.password_length_error));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            emailEditText.setError(null);
            passwordEditText.setError(null);
            signinUser();
        }
    }

    private void signinUser() {
        signinDialog = ProgressDialog.show(this, "", getString(R.string.signin_processing), false);

        ApiUtils.signinWithEmail(GeneralUtils.getAquery(this), emailEditText.getText().toString(), passwordEditText.getText().toString(), new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                signinDialog.cancel();

                if (status.getError() == null && object != null && status.getCode() != 401) {
                    JSONObject rawUser = null;
                    String token = null;

                    try {
                        result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");
                        token = result.getString("auth_token");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    SessionUtils.saveCurrentUserToken(SigninActivity.this, token);
                    SessionUtils.updateCurrentUserInfoInPhone(SigninActivity.this, User.rawUserToInstance(rawUser));

                    //TODO: Mixpanel identify

                    Intent nav = new Intent(SigninActivity.this, NavActivity.class);
                    SigninActivity.this.startActivity(nav);
                } else if (status.getCode() == 401) {
                    Toast toast = Toast.makeText(SigninActivity.this, getString(R.string.invalid_signin_message), Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(SigninActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }
}