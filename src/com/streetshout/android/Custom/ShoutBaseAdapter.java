package com.streetshout.android.Custom;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ShoutBaseAdapter extends BaseAdapter {
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }

    protected boolean load()
    {
        return false;
    }

    public void processLatestJSON()
    {
        return;
    }

    public void refresh()
    {
        return;
    }
}
