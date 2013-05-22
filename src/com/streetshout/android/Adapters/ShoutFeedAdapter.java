package com.streetshout.android.Adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.streetshout.android.Activities.MainActivity;
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

    private CameraPosition.Builder builder = null;

    private GoogleMap map = null;

    int page = 1;

    public ShoutFeedAdapter(Context context, AQuery aq, GoogleMap map) {
        this.context = context;
        this.aq = aq;
        this.map = map;

        builder = new CameraPosition.Builder();
        builder.zoom(MainActivity.CLICK_ON_SHOUT_ZOOM);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FrameLayout shoutView;

        if (convertView != null) {
            shoutView = (FrameLayout) convertView;
        } else {
            shoutView = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.feed_shout_view, null);
        }


        JSONObject rawShout = null;

        try {
            rawShout = items.getJSONObject(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (rawShout == null) {
            return null;
        }

        final ShoutModel shout = ShoutModel.rawShoutToInstance(rawShout);

        if (shout != null) {
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_title)).setText(shout.displayName);
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_body)).setText(shout.description);
            ((TextView) shoutView.findViewById(R.id.feed_shout_item_stamp)).setText(TimeUtils.shoutAgeToString((Activity) context, TimeUtils.getShoutAge(shout.created)));
        }

        shoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraUpdate update = CameraUpdateFactory.newCameraPosition(builder.target(new LatLng(shout.lat, shout.lng)).build());
                map.moveCamera(update);
                ((MainActivity) context).toggle();
            }
        });

        return shoutView;
    }

    @Override
    public void refresh() {
        this.page = 1;
        this.items = null;

        this.notifyDataSetInvalidated();
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
