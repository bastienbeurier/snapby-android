package com.streetshout.android.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.streetshout.android.Custom.ShoutBaseAdapter;
import com.streetshout.android.Models.ShoutModel;
import com.streetshout.android.R;
import com.streetshout.android.Utils.ApiUtils;
import com.streetshout.android.Utils.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ShoutFeedAdapter extends ShoutBaseAdapter{
    static final int DEFAULT_PAGE_SIZE = 20;

    private Context context = null;

    private AQuery aq = null;

    private JSONArray items = null;

    int page = 1;

    public ShoutFeedAdapter(Context context, AQuery aq) {
        this.context = context;
        this.aq = aq;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FrameLayout shoutView;

        if (convertView != null) {
            shoutView = (FrameLayout) convertView;
        } else {
            shoutView = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.feed_shout_view, null);
        }

        ShoutModel shout = null;

        try {
            shout = ShoutModel.rawShoutToInstance(items.getJSONObject(position));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (shout != null) {
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_title)).setText(shout.displayName);
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_body)).setText(shout.description);
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_stamp)).setText(TimeUtils.shoutAgeToString(TimeUtils.getShoutAge(shout.created)));
        }

        return shoutView;
    }

    @Override
    public void refresh() {
        this.page = 1;
        this.items = null;

        this.notifyDataSetChanged();
    }

    @Override
    public void processLatestJSON() {
        this.notifyDataSetChanged();

        this.page++;
    }

    @Override
    protected boolean load() {
        AjaxCallback<JSONObject> cb	= new AjaxCallback<JSONObject>();
        cb.type(JSONObject.class);

        ApiUtils.retrieveFeedShouts(aq, page, DEFAULT_PAGE_SIZE, cb);

        JSONObject latestJSON = cb.getResult();

        if (latestJSON == null) {
            return false;
        }

        JSONArray rawShouts = null;

        try {
            rawShouts = latestJSON.getJSONArray("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (rawShouts == null || rawShouts.length() == 0) {
            return false;
        }

        int newShoutsCount = rawShouts.length();
        if (items == null) {
            items = rawShouts;
        } else {
            for (int i = 0; i < newShoutsCount; i++) {
                try {
                    items.put(rawShouts.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (newShoutsCount < DEFAULT_PAGE_SIZE) {
            return false;
        }
        return true;
    }

    @Override
    public int getCount() {
        if (items != null)
            return items.length();
        else
            return 0;
    }

    @Override
    public boolean isEmpty() {
        if (this.getCount() == 0)
            return true;

        return false;
    }

    @Override
    public JSONObject getItem(int position) {

        JSONObject	object	= null;

        try {
            object	= items.getJSONObject(position);
        } catch (JSONException e) {
        }

        return object;
    }
}
