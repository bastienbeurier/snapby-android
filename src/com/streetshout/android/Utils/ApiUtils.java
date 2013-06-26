package com.streetshout.android.Utils;

import android.content.Context;
import android.location.Location;
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
    public static String SITEURL = Constants.PRODUCTION ? "http://street-shout.herokuapp.com" : "http://dev-street-shout.herokuapp.com";

    /** API call to create a new shout */
    public static void createShout(Context ctx, AQuery aq, double lat, double lng, String userName, String description, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/shouts.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_name", userName);
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("device_id", GeneralUtils.getDeviceId(ctx));

        cb.timeout(10000);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    /** API call to retrieve shouts in a zone of the map */
    public static void pullShoutsInZone(AQuery aq, double neLat, double neLng, double swLat, double swLng, AjaxCallback<JSONObject> cb) {
        String url = SITEURL + "/bound_box_shouts.json?neLat=" + String.valueOf(neLat) + "&neLng=" + String.valueOf(neLng)
                                                   + "&swLat=" + String.valueOf(swLat) + "&swLng=" + String.valueOf(swLng);

        aq.ajax(url, JSONObject.class, cb);
    }

    public static void sendDeviceInfo(Context context, AQuery aq, Location lastLocation, Integer notificationRadius) {
        String url = SITEURL + "/update_device_info";

        Map<String, Object> params = PushNotifications.getDeviceInfo(context);
        if (lastLocation != null) {
            params.put("lat", lastLocation.getLatitude());
            params.put("lng", lastLocation.getLongitude());
        }

        if (notificationRadius != null) {
            params.put("notification_radius", Integer.toString(notificationRadius));
        }

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void startDemo(AQuery aq) {
        String url = SITEURL + "/demo";

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

        aq.ajax(url, JSONObject.class, cb);
    }
}
