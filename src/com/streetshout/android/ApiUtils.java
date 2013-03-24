package com.streetshout.android;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiUtils {

    public static String SITEURL = "http://street-shout.herokuapp.com";

    public static void createShout(AQuery aq, double lat, double lng, String description, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/shouts.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);

        aq.ajax(url, params, JSONObject.class, cb);
    }
}
