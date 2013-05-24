package com.streetshout.android.Adapters;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.streetshout.android.R;
import com.streetshout.android.Utils.GeneralUtils;

/**
 * Created with IntelliJ IDEA.
 * User: bastienbeurier
 * Date: 5/23/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class MapWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private Context context = null;

    public MapWindowAdapter(Context context) {
        this.context = context;
    }

    // Use default InfoWindow frame
    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    // Defines the contents of the InfoWindow
    @Override
    public View getInfoContents(Marker marker) {

        if (marker.getTitle() == null && marker.getSnippet() == null) {
            return null;
        }

        // Getting view from the layout file info_window_layout
        View v = ((Activity) context).getLayoutInflater().inflate(R.layout.map_info_window, null);

        // Getting reference to the TextView to set title
        TextView userNameView = (TextView) v.findViewById(R.id.map_info_window_title);
        TextView descriptionView = (TextView) v.findViewById(R.id.map_info_window_body);
        TextView timeStampView = (TextView) v.findViewById(R.id.map_info_window_stamp);

        userNameView.setText(marker.getTitle());

        String[] descriptionAndStamp = TextUtils.split(marker.getSnippet(), GeneralUtils.STAMP_DIVIDER);

        descriptionView.setText(descriptionAndStamp[0]);
        timeStampView.setText(descriptionAndStamp[1]);

        // Returning the view containing InfoWindow contents
        return v;
    }
}
