package com.streetshout.android;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.CameraPosition;
import org.json.JSONObject;

public class MapRequestHandler {
    private CameraPosition lastRequest = null;

    private RequestResponseListener requestResponseListener;

    public interface RequestResponseListener {
        public void responseReceived(JSONObject object, AjaxStatus status);
    }

    public void addMapRequest(AQuery aq, CameraPosition position) {
        this.lastRequest = position;
        ApiUtils.pullShoutsInZone(aq, 20, position.target.latitude, position.target.longitude, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                super.callback(url, object, status);
                reportResponseReceived(object, status);
            }
        });
    }

    public void setRequestResponseListener(RequestResponseListener listener) {
        requestResponseListener = listener;
    }

    private void reportResponseReceived(JSONObject object, AjaxStatus status) {
        if (requestResponseListener != null) {
            requestResponseListener.responseReceived(object, status);
        }
    }
}
