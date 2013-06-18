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
import org.json.JSONObject;

public class FeedFragment extends ListFragment {
    private ListView feedListView = null;

    private Activity activity = null;

    private AQuery aq = null;

    private OnFeedShoutSelectedListener shoutSelectedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        activity = getActivity();
        aq = new AQuery(activity);

        setListAdapter(new ShoutFeedEndlessAdapter(activity, aq));

        return inflater.inflate(R.layout.feed_fragment, container, false);
    }

    public interface OnFeedShoutSelectedListener {
        public void onFeedShoutSelected(JSONObject rawShout);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            shoutSelectedListener = (OnFeedShoutSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFeedShoutSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        shoutSelectedListener.onFeedShoutSelected((JSONObject) getListAdapter().getItem(position));
    }
}
