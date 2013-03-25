package com.streetshout.android.Utils;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.streetshout.android.Utils.ApiUtils;
import org.json.JSONObject;

/**
 * Tools relative to shouts, a class might be needed in the future.
 */
public class ShoutUtils {

    /** User creates a new shout */
    public static void createShout(AQuery aq, double lat, double lng, String description, AjaxCallback<JSONObject> cb) {
        ApiUtils.createShout(aq, lat, lng, description, cb);
    }
}
