package com.streetshout.android.Utils;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.LatLngBounds;
import org.json.JSONObject;

/**
 * Handles the requests made to populate the map with shouts as the user scrolls
 */
public class MapRequestHandler {
    /** Listener for the MapRequestHandler */
    private RequestResponseListener requestResponseListener;

    /** Implement listener for MapRequestHandler responses */
    public interface RequestResponseListener {
        public void responseReceived(String url, JSONObject object, AjaxStatus status);
    }

    /** Add a request to populate a zone of the map with shout, we're going to handle that */
    public void addMapRequest(AQuery aq, LatLngBounds latLngBounds) {
        //API call to retrieve shouts in the screen
        ApiUtils.pullShoutsInZone(aq, latLngBounds.northeast.latitude, latLngBounds.northeast.longitude, latLngBounds.southwest.latitude, latLngBounds.southwest.longitude, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);

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
