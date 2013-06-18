package com.streetshout.android.Fragments;


import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.androidquery.AQuery;
import com.streetshout.android.Adapters.ShoutFeedEndlessAdapter;
import com.streetshout.android.R;

public class FeedFragment extends ListFragment {
    private ListView feedListView = null;

    private Activity activity = null;

    private AQuery aq = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        activity = getActivity();
        aq = new AQuery(activity);

        setListAdapter(new ShoutFeedEndlessAdapter(activity, aq));

        return inflater.inflate(R.layout.feed_fragment, container, false);
    }
}
