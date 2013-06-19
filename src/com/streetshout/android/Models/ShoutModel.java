package com.streetshout.android.Models;

import android.os.Parcel;
import android.os.Parcelable;
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
public class ShoutModel implements Parcelable {

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

    /** Shout source (ex: Twitter) */
    public String source = "";

    public String displayName = "";

    /** Turns a JSONArray received from the API to an ArrayList of UserModel instances */
    public static ArrayList<ShoutModel> rawShoutsToInstances(JSONArray rawShouts) {
        ArrayList<ShoutModel> shouts = new ArrayList<ShoutModel>();

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
                    shout.source = rawShout.getString("source");
                    shout.displayName = rawShout.getString("display_name");
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
                shout.displayName = rawShout.getString("display_name");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return shout;
    }

    /** User creates a new shout */
    public static void createShout(AQuery aq, double lat, double lng, String userName, String description, AjaxCallback<JSONObject> cb) {
        ApiUtils.createShout(aq, lat, lng, userName, description, cb);
    }

    /**
     * To implement Parcelable interface
     */
    public int describeContents() {
        return 0;
    }

    /**
     * To implement Parcelable interface
     */
    public static final Parcelable.Creator<ShoutModel> CREATOR = new Parcelable.Creator<ShoutModel>() {
        public ShoutModel createFromParcel(Parcel in) {
            return new ShoutModel(in);
        }

        public ShoutModel[] newArray(int size) {
            return new ShoutModel[size];
        }
    };

    /**
     * To implement Parcelable interface
     */
    private ShoutModel(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Default constructor
     */
    public ShoutModel() {
    }

    /**
     * To implement Parcelable interface
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(description);
        out.writeString(created);
        out.writeString(source);
        out.writeString(displayName);
        out.writeInt(id);
        out.writeDouble(lat);
        out.writeDouble(lng);
    }

    /**
     * To implement Parcelable interface
     */
    public void readFromParcel(Parcel in) {
        description = in.readString();
        created = in.readString();
        source = in.readString();
        displayName = in.readString();
        id = in.readInt();
        lat = in.readDouble();
        lng = in.readDouble();
    }
}
