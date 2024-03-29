package com.snapby.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.snapby.android.fragments.CameraFragment;
import com.snapby.android.fragments.ExploreFragment;
import com.snapby.android.fragments.ProfileFragment;
import com.snapby.android.fragments.SettingsFragment;

/**
 * Created by bastien on 4/11/14.
 */
public class MainSlidePagerAdapter extends FragmentStatePagerAdapter {

    private ExploreFragment exploreFragment = null;
    private CameraFragment cameraFragment = null;
    private ProfileFragment profileFragment = null;
    private SettingsFragment settingsFragment = null;

    public MainSlidePagerAdapter(FragmentManager fm, ExploreFragment exploreFragment, CameraFragment cameraFragment, ProfileFragment profileFragment, SettingsFragment settingsFragment) {
        super(fm);

        this.exploreFragment = exploreFragment;
        this.cameraFragment = cameraFragment;
        this.profileFragment = profileFragment;
        this.settingsFragment = settingsFragment;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:  return exploreFragment;
            case 1:  return cameraFragment;
            case 2:  return profileFragment;
            case 3:  return settingsFragment;
        }

        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }
}
