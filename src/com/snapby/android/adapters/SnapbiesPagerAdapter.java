package com.snapby.android.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.snapby.android.fragments.SnapbyPageFragment;
import com.snapby.android.models.Snapby;

import java.util.ArrayList;

/**
 * Created by bastien on 3/3/14.
 */
public class SnapbiesPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<Snapby> items = null;

    private String type = null;

    public SnapbyPageFragment firstFragment = null;

    public SnapbiesPagerAdapter(FragmentManager fm, ArrayList<Snapby> snapbies) {
        super(fm);

        this.items = snapbies;
    }

    public SnapbiesPagerAdapter(FragmentManager fm, ArrayList<Snapby> snapbies, String type) {
        super(fm);

        this.items = snapbies;

        this.type = type;
    }

    @Override
    public Fragment getItem(int position) {
        SnapbyPageFragment fragment = null;

        if (type != null && type.equals("profile")) {
            fragment = SnapbyPageFragment.newInstance(items.get(position), type, position);
        } else {
            fragment = SnapbyPageFragment.newInstance(items.get(position), position);
        }

        if (position == 0) {
            firstFragment = fragment;
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return items.size();
    }
}
