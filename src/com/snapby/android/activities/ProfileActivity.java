package com.snapby.android.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.snapby.android.R;
import com.snapby.android.fragments.ProfileFragment;

/**
 * Created by bastien on 3/10/14.
 */
public class ProfileActivity extends FragmentActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile_activity);
    }

    @Override
    public void onResume() {
        ProfileFragment profileFragment = (ProfileFragment) this.getSupportFragmentManager().findFragmentById(R.id.profile_fragment);

        profileFragment.getUserInfo();


        super.onResume();
    }
}