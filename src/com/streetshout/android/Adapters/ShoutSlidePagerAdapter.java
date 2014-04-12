package com.streetshout.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.streetshout.android.fragments.ShoutSlidePageFragment;
import com.streetshout.android.models.Shout;

import java.util.ArrayList;

/**
 * Created by bastien on 3/3/14.
 */
public class ShoutSlidePagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<Shout> items = null;

    public ShoutSlidePagerAdapter(FragmentManager fm, ArrayList<Shout> shouts) {
        super(fm);

        this.items = shouts;
    }

    @Override
    public Fragment getItem(int position) {
        return ShoutSlidePageFragment.newInstance(items.get(position));
    }

    @Override
    public int getCount() {
        return items.size();
    }

//    @Override
//    public float getPageWidth(int position) {
//        return 0.3f;
//    }
}
