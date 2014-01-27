package com.streetshout.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.streetshout.android.R;

/**
 * Created by bastien on 1/27/14.
 */
public class WelcomeActivity extends Activity {

    private Button signinButton = null;

    private Button signupButton = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        signinButton = (Button) findViewById(R.id.signin_button);
        signupButton = (Button) findViewById(R.id.signup_button);

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
    }
}