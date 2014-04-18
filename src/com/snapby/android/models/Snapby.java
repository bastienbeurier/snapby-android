package com.snapby.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Model for snapbies
 */
public class Snapby implements Parcelable {

    /** Snapby id */
    public int id = 0;

    /** Snapby user id */
    public int userId = 0;

    /** Snapby latitude */
    public double lat = 0;

    /** Snapby longitude */
    public double lng = 0;

    /** Snapby creation date/time */
    public String created = "";

    public String lastActive = "";

    public String username = "";

    public Boolean removed = false;

    public Boolean anonymous = false;

    public int likeCount = 0;

    public int commentCount = 0;

    public int userScore = 0;

    /** Turns a JSONArray received from the API to an ArrayList of UserModel instances */
    public static ArrayList<Snapby> rawSnapbiesToInstances(JSONArray rawSnapbies) {
        ArrayList<Snapby> snapbies = new ArrayList<Snapby>();

        int len = rawSnapbies.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawSnapby = rawSnapbies.getJSONObject(i);
                if (rawSnapby != null) {
                    Snapby snapby = rawSnapbyToInstance(rawSnapby);
                    snapbies.add(snapby);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return snapbies;
    }

    public static Snapby rawSnapbyToInstance(JSONObject rawSnapby) {
        Snapby snapby = new Snapby();

        try {
            if (rawSnapby != null) {
                snapby.id = Integer.parseInt(rawSnapby.getString("id"));
                snapby.userId = Integer.parseInt(rawSnapby.getString("user_id"));
                snapby.lat = Double.parseDouble(rawSnapby.getString("lat"));
                snapby.lng = Double.parseDouble(rawSnapby.getString("lng"));
                snapby.created = rawSnapby.getString("created_at");
                snapby.lastActive = rawSnapby.getString("last_active");
                snapby.username = rawSnapby.getString("username");
                snapby.removed = rawSnapby.getBoolean("removed");
                snapby.anonymous = rawSnapby.getBoolean("anonymous");
                snapby.likeCount = Integer.parseInt(rawSnapby.getString("like_count"));
                snapby.commentCount = Integer.parseInt(rawSnapby.getString("comment_count"));

                if (rawSnapby.has("user_score") && !rawSnapby.getString("user_score").equals("null")) {
                    snapby.userScore = Integer.parseInt(rawSnapby.getString("user_score"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return snapby;
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
    public static final Creator<Snapby> CREATOR = new Creator<Snapby>() {
        public Snapby createFromParcel(Parcel in) {
            return new Snapby(in);
        }

        public Snapby[] newArray(int size) {
            return new Snapby[size];
        }
    };

    /**
     * To implement Parcelable interface
     */
    private Snapby(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Default constructor
     */
    public Snapby() {
    }

    /**
     * To implement Parcelable interface
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(created);
        out.writeString(lastActive);
        out.writeString(username);
        out.writeInt(id);
        out.writeInt(userId);
        out.writeDouble(lat);
        out.writeDouble(lng);
        out.writeByte((byte) (removed ? 1 : 0));
        out.writeByte((byte) (anonymous ? 1 : 0));
        out.writeInt(likeCount);
        out.writeInt(commentCount);
        out.writeInt(userScore);
    }

    /**
     * To implement Parcelable interface
     */
    public void readFromParcel(Parcel in) {
        created = in.readString();
        lastActive = in.readString();
        username = in.readString();
        id = in.readInt();
        userId = in.readInt();
        lat = in.readDouble();
        lng = in.readDouble();
        removed = in.readByte() != 0;
        anonymous = in.readByte() != 0;
        likeCount = in.readInt();
        commentCount = in.readInt();
        userScore = in.readInt();
    }
}
