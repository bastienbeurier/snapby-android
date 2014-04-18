package com.snapby.android.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.snapby.android.R;
import com.snapby.android.activities.MainActivity;
import com.snapby.android.models.User;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.AppPreferences;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import com.snapby.android.utils.ImageUtils;
import com.snapby.android.utils.SessionUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Created by bastien on 4/17/14.
 */
public class SettingsFragment extends Fragment {

    private AppPreferences appPrefs = null;

    private EditText settingsUsernameEditText = null;

    private ImageView profilePictureView = null;

    private FrameLayout profilePictureContainer = null;

    private User currentUser = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.settings, container, false);

        appPrefs = new AppPreferences(getActivity());

        currentUser = SessionUtils.getCurrentUser(getActivity());

        settingsUsernameEditText = (EditText) rootView.findViewById(R.id.settings_username_editText);
        profilePictureContainer = (FrameLayout) rootView.findViewById(R.id.settings_edit_profile_picture_container);
        profilePictureView = (ImageView) rootView.findViewById(R.id.settings_profile_picture);

        updateUI();

        profilePictureContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profilePictureContainer.setEnabled(false);

                letUserChooseProfilePic();
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
                    } else if (!GeneralUtils.isValidUsername(username)) {
                        settingsUsernameEditText.setError(getString(R.string.invalid_username_error));
                        errors = true;
                    }

                    if (errors) {
                        updateUI();
                    } else {
                        settingsUsernameEditText.setError(null);

                        ApiUtils.updateUserInfoWithLocation(getActivity(), GeneralUtils.getAquery(getActivity()), null, null, username, new AjaxCallback<JSONObject>() {
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

                                    SessionUtils.updateCurrentUserInfoInPhone(getActivity(), User.rawUserToInstance(rawUser));

                                    Toast toast = Toast.makeText(getActivity(), getString(R.string.username_successfully_updated), Toast.LENGTH_SHORT);
                                    toast.show();

                                } else if (status.getError() == null && status.getCode() == 222) {
                                    updateUI();
                                    Toast toast = Toast.makeText(getActivity(), getString(R.string.username_taken_error), Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    updateUI();
                                    Toast toast = Toast.makeText(getActivity(), getString(R.string.no_connection), Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });
                    }

                }

                return false;
            }
        });

        ((Button) rootView.findViewById(R.id.rate_me_button)).setText(getString(R.string.rate_me_preference_button));

        rootView.findViewById(R.id.feedback_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("plain/text");

                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { Constants.FEEDBACK_EMAIL });
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feed_back_mail_title, GeneralUtils.getAppVersion(getActivity())));
                startActivity(Intent.createChooser(feedbackIntent, ""));
            }
        });

        rootView.findViewById(R.id.rate_me_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = getActivity();

                Uri uri = Uri.parse("market://details?id=" + ctx.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ctx, getString(R.string.no_google_play_store), Toast.LENGTH_LONG).show();
                }
            }
        });

        rootView.findViewById(R.id.logout_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionUtils.logOut(getActivity());
            }
        });

        ((TextView) rootView.findViewById(R.id.app_name_and_version)).setText(getString(R.string.app_full_name) + " (v." + GeneralUtils.getAppVersion(getActivity()) + ")");

        return rootView;
    }

    public void letUserChooseProfilePic() {
        if (ImageUtils.isSDPresent() == false){
            Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.no_sd_card), Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        ((MainActivity) getActivity()).letUserChooseProfilePic();
    }

    public void updateUI() {
        profilePictureContainer.setEnabled(true);

        settingsUsernameEditText.setText(currentUser.username);

        GeneralUtils.getAquery(getActivity()).id(profilePictureView).image(GeneralUtils.getProfileThumbPicturePrefix() + currentUser.id, false, false);
    }
}