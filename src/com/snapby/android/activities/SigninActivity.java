package com.snapby.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.snapby.android.R;
import com.snapby.android.models.User;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.SessionUtils;
import com.snapby.android.utils.TrackingUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by bastien on 1/27/14.
 */
public class SigninActivity extends Activity {

    private Button resetPasswordButton = null;

    private EditText emailEditText = null;

    private EditText passwordEditText = null;

    private ConnectivityManager connectivityManager = null;

    private ProgressDialog signinDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        emailEditText = (EditText) findViewById(R.id.user_email_editText);
        passwordEditText = (EditText) findViewById(R.id.user_password_editText);

        resetPasswordButton = (Button) findViewById(R.id.reset_password_button);


        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resetPassword = new Intent(SigninActivity.this, ResetPasswordActivity.class);
                startActivityForResult(resetPassword, Constants.RESET_PASSWORD_REQUEST);
            }
        });

        emailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    SigninActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        //Set focus on email EditText
        emailEditText.requestFocus();
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

        if (password.length() < Constants.MIN_PASSWORD_LENGTH || password.length() > Constants.MAX_PASSWORD_LENGTH) {
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

                    User currentUser = User.rawUserToInstance(rawUser);
                    SessionUtils.updateCurrentUserInfoInPhone(SigninActivity.this, currentUser);

                    TrackingUtils.identify(SigninActivity.this, currentUser);

                    Intent main = new Intent(SigninActivity.this, MainActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    SigninActivity.this.startActivity(main);
                    finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.signin_menu, menu);

        MenuItem item = menu.findItem(R.id.action_signin);
        item.setTitle("Sign In");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_signin) {
            validateSigninInfo();
            return false;
        } else {
            Intent returnIntent = new Intent();
            setResult(RESULT_CANCELED, returnIntent);
            finish();
            return true;
        }
    }
}