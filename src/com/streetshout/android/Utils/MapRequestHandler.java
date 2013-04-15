package com.streetshout.android.Utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.CameraPosition;
import org.json.JSONObject;

/**
 * Handles the requests made to populate the map with shouts as the user scrolls
 */
public class MapRequestHandler {
    private static final int MIN_QUERY_RADIUS = 15;

    /** Camera position given by the caller to retrieve shouts in a zone */
    private CameraPosition lastRequest = null;

    /** Listener for the MapRequestHandler */
    private RequestResponseListener requestResponseListener;

    /** Implement listener for MapRequestHandler responses */
    public interface RequestResponseListener {
        public void responseReceived(String url, JSONObject object, AjaxStatus status);
    }

    /** Add a request to populate a zone of the map with shout, we're going to handle that */
    public void addMapRequest(Activity ctx, AQuery aq, CameraPosition position) {
        this.lastRequest = position;

        int queryRadius = Math.max(MIN_QUERY_RADIUS, LocationUtils.zoomToKm(position.zoom, ctx.getWindowManager().getDefaultDisplay()));

        //API call to retrieve shouts in the camera zone
        ApiUtils.pullShoutsInZone(aq, queryRadius, position.target.latitude, position.target.longitude, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

                //Notify the MapRequestHandler caller that we received the shouts
                reportResponseReceived(url, object, status);
            }
        });
    }

    /** Method to set RequestResponseLister in the caller class (MainActivity) */
    public void setRequestResponseListener(RequestResponseListener listener) {
        requestResponseListener = listener;
    }

    /** Notify the caller that MapRequestHandler received a response, fire the callback */
    private void reportResponseReceived(String url, JSONObject object, AjaxStatus status) {
        if (requestResponseListener != null) {
            requestResponseListener.responseReceived(url, object, status);
        }
    }
}