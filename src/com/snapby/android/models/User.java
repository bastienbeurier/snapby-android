package com.snapby.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/27/14.
 */
public class User implements Parcelable {

    /** User id */
    public int id = 0;

    /** User email */
    public String email = "";

    /** Username */
    public String username = "";

    /** User blacklisted */
    public Boolean isBlackListed = false;

    /** User latitude */
    public double lat = 0;

    /** User longitude */
    public double lng = 0;

    public int snapbyCount = 0;

    public int likedSnapbies = 0;

    public static ArrayList<User> rawUsersToInstances(JSONArray rawUsers) {
        ArrayList<User> users = new ArrayList<User>();

        int len = rawUsers.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawUser = rawUsers.getJSONObject(i);
                if (rawUser != null) {
                    User user = rawUserToInstance(rawUser);
                    users.add(user);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return users;
    }

    public static User rawUserToInstance(JSONObject rawUser) {
        if (rawUser == null) {
            return null;
        }

        User user = new User();

        try {
            user.id = Integer.parseInt(rawUser.getString("id"));
            user.email = rawUser.getString("email");
            user.username = rawUser.getString("username");
            user.isBlackListed = Boolean.parseBoolean(rawUser.getString("black_listed"));

            String rawLat = rawUser.getString("lat");
            String rawLng = rawUser.getString("lng");

            if (!rawLat.equals("null") && !rawLng.equals("null")) {
                user.lat = Double.parseDouble(rawLat);
                user.lng = Double.parseDouble(rawLng);
            }

            user.snapbyCount = Integer.parseInt(rawUser.getString("snapby_count"));
            user.likedSnapbies = Integer.parseInt(rawUser.getString("liked_snapbies"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return user;
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
    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    /**
     * To implement Parcelable interface
     */
    private User(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Default constructor
     */
    public User() {
    }

    /**
     * To implement Parcelable interface
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(email);
        out.writeString(username);
        out.writeByte((byte) (isBlackListed ? 1 : 0));
        out.writeDouble(lat);
        out.writeDouble(lng);
        out.writeInt(snapbyCount);
        out.writeInt(likedSnapbies);
    }

    /**
     * To implement Parcelable interface
     */
    public void readFromParcel(Parcel in) {
        id = in.readInt();
        email = in.readString();
        username = in.readString();
        isBlackListed = in.readByte() != 0;
        lat = in.readDouble();
        lng = in.readDouble();
        snapbyCount = in.readInt();
        likedSnapbies = in.readInt();
    }
}