package com.streetshout.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Model for shouts
 */
public class Shout implements Parcelable {

    /** Shout id */
    public int id = 0;

    /** Shout user id */
    public int userId = 0;

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

    public String username = "";

    public String image = "";

    public Boolean removed = false;

    /** Turns a JSONArray received from the API to an ArrayList of UserModel instances */
    public static ArrayList<Shout> rawShoutsToInstances(JSONArray rawShouts) {
        ArrayList<Shout> shouts = new ArrayList<Shout>();

        int len = rawShouts.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawShout = rawShouts.getJSONObject(i);
                if (rawShout != null) {
                    Shout shout = rawShoutToInstance(rawShout);
                    shouts.add(shout);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return shouts;
    }

    public static Shout rawShoutToInstance(JSONObject rawShout) {
        Shout shout = new Shout();

        try {
            if (rawShout != null) {
                shout.id = Integer.parseInt(rawShout.getString("id"));
                shout.userId = Integer.parseInt(rawShout.getString("user_id"));
                shout.lat = Double.parseDouble(rawShout.getString("lat"));
                shout.lng = Double.parseDouble(rawShout.getString("lng"));
                shout.description = rawShout.getString("description");
                shout.created = rawShout.getString("created_at");
                shout.source = rawShout.getString("source");
                shout.username = rawShout.getString("username");
                shout.image = rawShout.getString("image");
                shout.removed = rawShout.getBoolean("removed");
                shout.image = shout.image.equals("null") ? "" : "http://" + shout.image;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return shout;
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
    public static final Parcelable.Creator<Shout> CREATOR = new Parcelable.Creator<Shout>() {
        public Shout createFromParcel(Parcel in) {
            return new Shout(in);
        }

        public Shout[] newArray(int size) {
            return new Shout[size];
        }
    };

    /**
     * To implement Parcelable interface
     */
    private Shout(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Default constructor
     */
    public Shout() {
    }

    /**
     * To implement Parcelable interface
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(description);
        out.writeString(created);
        out.writeString(source);
        out.writeString(username);
        out.writeInt(id);
        out.writeInt(userId);
        out.writeDouble(lat);
        out.writeDouble(lng);
        out.writeByte((byte) (removed ? 1 : 0));
        out.writeString(image);
    }

    /**
     * To implement Parcelable interface
     */
    public void readFromParcel(Parcel in) {
        description = in.readString();
        created = in.readString();
        source = in.readString();
        username = in.readString();
        id = in.readInt();
        userId = in.readInt();
        lat = in.readDouble();
        lng = in.readDouble();
        removed = in.readByte() != 0;
        image = in.readString();
    }
}
