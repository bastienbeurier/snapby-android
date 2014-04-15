package com.streetshout.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.streetshout.android.fragments.SnapbyPageFragment;
import com.streetshout.android.models.Shout;

import java.util.ArrayList;

/**
 * Created by bastien on 3/3/14.
 */
public class ShoutsPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<Shout> items = null;

    private String type = null;

    public ShoutsPagerAdapter(FragmentManager fm, ArrayList<Shout> shouts) {
        super(fm);

        this.items = shouts;
    }

    public ShoutsPagerAdapter(FragmentManager fm, ArrayList<Shout> shouts, String type) {
        super(fm);

        this.items = shouts;

        this.type = type;
    }

    @Override
    public Fragment getItem(int position) {
        if (type != null && type.equals("profile")) {
            return SnapbyPageFragment.newInstance(items.get(position), type);
        } else {
            return SnapbyPageFragment.newInstance(items.get(position));
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }
}
