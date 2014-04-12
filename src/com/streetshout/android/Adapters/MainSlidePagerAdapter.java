package com.streetshout.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.streetshout.android.Fragments.CameraFragment;
import com.streetshout.android.Fragments.ExploreFragment;
import com.streetshout.android.Fragments.ProfileFragment;
import com.streetshout.android.activities.MainActivity;

/**
 * Created by bastien on 4/11/14.
 */
public class MainSlidePagerAdapter extends FragmentStatePagerAdapter {

    private ExploreFragment exploreFragment = null;
    private CameraFragment cameraFragment = null;
    private ProfileFragment profileFragment = null;

    public MainSlidePagerAdapter(FragmentManager fm, ExploreFragment exploreFragment, CameraFragment cameraFragment, ProfileFragment profileFragment) {
        super(fm);

        this.exploreFragment = exploreFragment;
        this.cameraFragment = cameraFragment;
        this.profileFragment = profileFragment;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:  return exploreFragment;
            case 1:  return cameraFragment;
            case 2:  return profileFragment;
        }

        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
