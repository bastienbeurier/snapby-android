package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 1/27/14.
 */
public class SignupActivity extends Activity {

    private Button signupValidateButton = null;

    private EditText usernameEditText = null;

    private EditText emailEditText = null;

    private EditText passwordEditText = null;

    private EditText confirmPasswordEditText = null;

    private ConnectivityManager connectivityManager = null;

    private ProgressDialog signinDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        usernameEditText = (EditText) findViewById(R.id.username_editText);
        emailEditText = (EditText) findViewById(R.id.user_email_editText);
        passwordEditText = (EditText) findViewById(R.id.user_password_editText);
        confirmPasswordEditText = (EditText) findViewById(R.id.user_confirm_password_editText);

        signupValidateButton = (Button) findViewById(R.id.signup_validate_button);

        signupValidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateSignupInfo();
            }
        });


        usernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    SignupActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        //Set focus on username EditText
        usernameEditText.requestFocus();
    }

    public void validateSignupInfo() {
        boolean errors = false;

        usernameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);

        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirm = confirmPasswordEditText.getText().toString();

        if (!GeneralUtils.isValidUsername(username)) {
            usernameEditText.setError(getString(R.string.invalid_username_error));
            errors = true;
        }

        if (username.length() < 6 || username.length() > 20) {
            usernameEditText.setError(getString(R.string.username_length_error));
            errors = true;
        }

        if (!GeneralUtils.isValidEmail(email)) {
            emailEditText.setError(getString(R.string.invalid_email_error));
            errors = true;
        }

        if (password.length() < 6 || password.length() > 128) {
            passwordEditText.setError(getString(R.string.password_length_error));
            errors = true;
        }

        if (!password.equals(confirm)) {
            confirmPasswordEditText.setError(getString(R.string.password_match_error));
            errors = true;
        }

        if (connectivityManager != null && connectivityManager.getActiveNetworkInfo() == null) {
            Toast toast = Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
            toast.show();
        } else if (!errors) {
            emailEditText.setError(null);
            passwordEditText.setError(null);
            signupUser();
        }
    }

    private void signupUser() {
        signinDialog = ProgressDialog.show(this, "", getString(R.string.signup_processing), false);

        ApiUtils.signupWithEmail(this, GeneralUtils.getAquery(this), usernameEditText.getText().toString(), emailEditText.getText().toString(), passwordEditText.getText().toString(), new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                signinDialog.cancel();

                if (status.getError() == null && object != null && status.getCode() != 222) {
                    JSONObject rawUser = null;
                    String token = null;

                    try {
                        JSONObject result = object.getJSONObject("result");

                        rawUser = result.getJSONObject("user");
                        token = result.getString("auth_token");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    SessionUtils.saveCurrentUserToken(SignupActivity.this, token);
                    SessionUtils.updateCurrentUserInfoInPhone(SignupActivity.this, User.rawUserToInstance(rawUser));

                    //TODO: Mixpanel identify and signup

                    Intent nav = new Intent(SignupActivity.this, NavActivity.class);
                    SignupActivity.this.startActivity(nav);
                } else if (status.getError() == null && status.getCode() == 222) {
                    Toast toast = null;

                    try {
                        JSONObject errors = object.getJSONObject("errors");
                        JSONObject userErrors = errors.getJSONObject("user");

                        if (userErrors.has("username")) {
                            toast = Toast.makeText(SignupActivity.this, getString(R.string.username_taken_error), Toast.LENGTH_SHORT);
                        } else if (userErrors.has("email")) {
                            toast = Toast.makeText(SignupActivity.this, getString(R.string.email_taken_error), Toast.LENGTH_SHORT);
                        }

                        toast.show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast toast = Toast.makeText(SignupActivity.this, getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }
}