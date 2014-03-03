package com.streetshout.android.adapters;

import android.content.Context;
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
    private Context context = null;

    private ArrayList<Shout> items = null;

    public ShoutSlidePagerAdapter(FragmentManager fm, Context context, ArrayList<Shout> shouts) {
        super(fm);

        this.context = context;
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
}
