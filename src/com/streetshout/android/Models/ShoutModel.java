package com.streetshout.android.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for shouts
 */
public class ShoutModel {

    public int id = 0;
    public double lat = 0;
    public double lng = 0;
    public String description = "";
    public String created = "";


    public static List<ShoutModel> rawShoutsToInstances(JSONArray rawShouts) {
        List<ShoutModel> shouts = new ArrayList<ShoutModel>();

        int len = rawShouts.length();
        for (int i = 0; i < len; i++) {
            JSONObject rawShout = null;

            try {
                rawShout = rawShouts.getJSONObject(i);
            } catch (JSONException e) {
                rawShout = null;
                e.printStackTrace();
            }

            ShoutModel shout = new ShoutModel();
            if (rawShout != null) {
                try {
                    shout.id = Integer.parseInt(rawShout.getString("id"));
                    shout.lat = Double.parseDouble(rawShout.getString("lat"));
                    shout.lng = Double.parseDouble(rawShout.getString("lng"));
                    shout.description = rawShout.getString("description");
                    shout.created = rawShout.getString("created_at");
                    shouts.add(shout);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return shouts;
    }
}
