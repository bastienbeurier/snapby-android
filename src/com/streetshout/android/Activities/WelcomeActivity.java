package com.streetshout.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.streetshout.android.R;
import com.streetshout.android.models.User;
import com.streetshout.android.utils.ApiUtils;
import com.streetshout.android.utils.GeneralUtils;
import com.streetshout.android.utils.SessionUtils;
import com.streetshout.android.utils.TrackingUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by bastien on 1/27/14.
 */
public class WelcomeActivity extends Activity {

    private Button signinButton = null;

    private Button signupButton = null;

    private Button connectFBButton = null;

    private ProgressDialog connectFBDialog;

    private Session.StatusCallback statusCallback = new SessionStatusCallback();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        //Mixpanel
        TrackingUtils.trackAppOpened(this);

        signinButton = (Button) findViewById(R.id.welcome_signin_button);
        signupButton = (Button) findViewById(R.id.welcome_signup_button);
        connectFBButton = (Button) findViewById(R.id.welcome_connect_fb_button);

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signin = new Intent(WelcomeActivity.this, SigninActivity.class);
                startActivity(signin);
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signup = new Intent(WelcomeActivity.this, SignupActivity.class);
                startActivity(signup);
            }
        });

        connectFBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonsVisibility(View.GONE);
                connectToFB();
            }
        });

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        //Retrieve FB session
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, statusCallback, savedInstanceState);
            }

            if (session == null) {
                session = new Session(this);
            }

            Session.setActiveSession(session);

            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                if (SessionUtils.isSignIn(this)) {
                    fbSessionRequest(session);
                    return;
                }
            }
        }

        if (SessionUtils.isSignIn(this)) {
            goToNavActivity();
        }

        setButtonsVisibility(View.VISIBLE);
    }

    private void setButtonsVisibility(int visibility) {
        signinButton.setVisibility(visibility);
        signupButton.setVisibility(visibility);
        connectFBButton.setVisibility(visibility);
    }

    @Override
    public void onStart() {
        super.onStart();
        Session.getActiveSession().addCallback(statusCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        Session.getActiveSession().removeCallback(statusCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    private void connectToFB() {
        Session session = Session.getActiveSession();

        if (session == null) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        if (!session.isOpened() && !session.isClosed()) {
            fbSessionRequest(session);
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
    }

    private void fbSessionRequest(Session session) {
        connectFBDialog = ProgressDialog.show(this, "", getString(R.string.signin_processing), false);
        Session.OpenRequest openRequest = new Session.OpenRequest(this).setCallback(statusCallback).setPermissions(Arrays.asList("basic_info","email"));
        session.openForRead(openRequest);
    }

    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {

            //User cancel FB connect
            if (exception instanceof FacebookOperationCanceledException || exception instanceof FacebookAuthorizationException) {

                if (connectFBDialog != null) {
                    connectFBDialog.cancel();
                }
                setButtonsVisibility(View.VISIBLE);

                Session.setActiveSession(null);

                return;
            }

            if (session.isOpened()) {
                if (SessionUtils.isSignIn(WelcomeActivity.this)) {
                    if (connectFBDialog != null) {
                        connectFBDialog.cancel();
                    }

                    goToNavActivity();
                } else {
                        Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {

                            // callback after Graph API response with user object
                            @Override
                            public void onCompleted(GraphUser user, Response response) {
                                if (user != null) {
                                    if (user.getUsername() == null || user.asMap().get("email") == null || user.getId() == null || user.getName() == null) {
                                        if (connectFBDialog != null) {
                                            connectFBDialog.cancel();
                                        }
                                        setButtonsVisibility(View.VISIBLE);
                                        Toast toast = Toast.makeText(WelcomeActivity.this, getString(R.string.facebook_connect_failed), Toast.LENGTH_SHORT);
                                        toast.show();

                                        return;
                                    }

                                    ApiUtils.connectFacebook(WelcomeActivity.this, GeneralUtils.getAquery(WelcomeActivity.this), user.getUsername(), user.asMap().get("email").toString(), user.getId(), user.getName(), new AjaxCallback<JSONObject>() {
                                        @Override
                                        public void callback(String url, JSONObject object, AjaxStatus status) {
                                            super.callback(url, object, status);

                                            if (status.getError() == null && object != null) {
                                                JSONObject rawUser = null;
                                                String token = null;
                                                Boolean isSigningUp = null;

                                                try {
                                                    result = object.getJSONObject("result");

                                                    rawUser = result.getJSONObject("user");
                                                    token = result.getString("auth_token");

                                                    isSigningUp = result.getBoolean("is_signup");

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                SessionUtils.saveCurrentUserToken(WelcomeActivity.this, token);

                                                User currentUser = User.rawUserToInstance(rawUser);
                                                SessionUtils.updateCurrentUserInfoInPhone(WelcomeActivity.this, currentUser);

                                                TrackingUtils.identify(WelcomeActivity.this, currentUser);

                                                if (isSigningUp != null && isSigningUp) {
                                                    TrackingUtils.trackSignup(WelcomeActivity.this, "Facebook");
                                                    SessionUtils.autofollowFacebookFriends(WelcomeActivity.this);
                                                }

                                                if (connectFBDialog != null) {
                                                    connectFBDialog.cancel();
                                                }
                                                goToNavActivity();
                                            } else {
                                                if (connectFBDialog != null) {
                                                    connectFBDialog.cancel();
                                                }
                                                setButtonsVisibility(View.VISIBLE);
                                                Toast toast = Toast.makeText(WelcomeActivity.this, getString(R.string.facebook_connect_failed), Toast.LENGTH_SHORT);
                                                toast.show();
                                            }
                                        }
                                    });
                                } else {
                                    if (connectFBDialog != null) {
                                        connectFBDialog.cancel();
                                    }
                                    setButtonsVisibility(View.VISIBLE);
                                    Toast toast = Toast.makeText(WelcomeActivity.this, getString(R.string.facebook_connect_failed), Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });
                        Request.executeBatchAsync(request);
                }
            }
        }
    }

    private void goToNavActivity() {
        TrackingUtils.identify(WelcomeActivity.this, SessionUtils.getCurrentUser(this));

        Intent main = new Intent(WelcomeActivity.this, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        WelcomeActivity.this.startActivity(main);

        finish();
    }
}