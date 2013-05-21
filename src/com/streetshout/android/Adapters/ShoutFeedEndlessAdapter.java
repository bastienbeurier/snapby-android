package com.streetshout.android.Adapters;

import android.content.Context;
import com.androidquery.AQuery;
import com.google.android.gms.maps.GoogleMap;
import com.streetshout.android.Custom.ShoutBaseEndlessAdapter;

public class ShoutFeedEndlessAdapter extends ShoutBaseEndlessAdapter{

    public ShoutFeedEndlessAdapter(Context context, AQuery aq, GoogleMap map) {
        super(new ShoutFeedAdapter(context, aq, map));
    }
}
