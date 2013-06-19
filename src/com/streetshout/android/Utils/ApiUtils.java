package com.streetshout.android.Utils;

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
    public static void createShout(AQuery aq, double lat, double lng, String userName, String description, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/shouts.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_name", userName);
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);

        cb.timeout(10000);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    /** API call to retrieve shouts in a zone of the map */
    public static void pullShoutsInZone(AQuery aq, double neLat, double neLng, double swLat, double swLng, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/bound_box_shouts.json?neLat=" + String.valueOf(neLat) + "&neLng=" + String.valueOf(neLng)
                                                   + "&swLat=" + String.valueOf(swLat) + "&swLng=" + String.valueOf(swLng);

        aq.ajax(url, JSONObject.class, cb);
    }

    public static void retrieveFeedShouts(AQuery aq, int page, int perPage, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/global_feed_shouts.json?page=" + String.valueOf(page) + "&per_page=" + String.valueOf(perPage);

        cb.url(url).type(JSONObject.class);

        aq.sync(cb);
    }
}
