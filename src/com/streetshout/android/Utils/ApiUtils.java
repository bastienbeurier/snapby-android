package com.streetshout.android.Utils;

import android.util.Log;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Tools relative to API calls to street-shout-web.
 */
public class ApiUtils {

    /** street-shout-web URL for API calls */
    public static String SITEURL = "http://street-shout.herokuapp.com";

    /** API call to create a new shout */
    public static void createShout(AQuery aq, double lat, double lng, String description, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/shouts.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    /** API call to retrieve shouts in a zone of the map */
    public static void pullShoutsInZone(AQuery aq, int radius, double lat, double lng, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/zone_shouts.json?lat=" + String.valueOf(lat) + "&lng=" + String.valueOf(lng)
                                                                                  + "&radius=" + String.valueOf(radius);

        aq.ajax(url, JSONObject.class, cb);
    }
}
