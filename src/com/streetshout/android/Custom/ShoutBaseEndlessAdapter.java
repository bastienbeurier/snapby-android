package com.streetshout.android.Custom;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.streetshout.android.R;

public class ShoutBaseEndlessAdapter extends EndlessAdapter {
    public ShoutBaseEndlessAdapter(ShoutBaseAdapter wrapped) {
        super(wrapped);
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        View row	= LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, null);

        return(row);
    }

    @Override
    protected boolean cacheInBackground() {
        ShoutBaseAdapter adapter	= (ShoutBaseAdapter)getWrappedAdapter();

        return adapter.load();
    }

    @Override
    protected void appendCachedData() {
        ShoutBaseAdapter adapter	= (ShoutBaseAdapter)getWrappedAdapter();

        adapter.processLatestJSON();
    }

    public void refresh()
    {
        ShoutBaseAdapter adapter	= (ShoutBaseAdapter)getWrappedAdapter();
        adapter.refresh();

        this.restartAppending();
    }
}
