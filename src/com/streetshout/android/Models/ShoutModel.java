package com.streetshout.android.Models;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.streetshout.android.Utils.ApiUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for shouts
 */
public class ShoutModel {

    /** Shout id */
    public int id = 0;

    /** Shout latitude */
    public double lat = 0;

    /** Shout longitude */
    public double lng = 0;

    /** Shout description */
    public String description = "";

    /** Shout creation date/time */
    public String created = "";

    /** Turns a JSONArray received from the API to an ArrayList of UserModel instances */
    public static List<ShoutModel> rawShoutsToInstances(JSONArray rawShouts) {
        List<ShoutModel> shouts = new ArrayList<ShoutModel>();

        int len = rawShouts.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawShout = rawShouts.getJSONObject(i);
                if (rawShout != null) {
                    ShoutModel shout = new ShoutModel();
                    shout.id = Integer.parseInt(rawShout.getString("id"));
                    shout.lat = Double.parseDouble(rawShout.getString("lat"));
                    shout.lng = Double.parseDouble(rawShout.getString("lng"));
                    shout.description = rawShout.getString("description");
                    shout.created = rawShout.getString("created_at");
                    shouts.add(shout);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return shouts;
    }

    public static ShoutModel rawShoutToInstance(JSONObject rawShout) {
        ShoutModel shout = new ShoutModel();

        try {
            if (rawShout != null) {
                shout.id = Integer.parseInt(rawShout.getString("id"));
                shout.lat = Double.parseDouble(rawShout.getString("lat"));
                shout.lng = Double.parseDouble(rawShout.getString("lng"));
                shout.description = rawShout.getString("description");
                shout.created = rawShout.getString("created_at");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return shout;
    }

    /** User creates a new shout */
    public static void createShout(AQuery aq, double lat, double lng, String description, AjaxCallback<JSONObject> cb) {
        ApiUtils.createShout(aq, lat, lng, description, cb);
    }
}
