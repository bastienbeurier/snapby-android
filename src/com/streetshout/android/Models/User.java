package com.streetshout.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

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
    }

    /**
     * To implement Parcelable interface
     */
    public void readFromParcel(Parcel in) {
        id = in.readInt();
        email = in.readString();
        username = in.readString();
        isBlackListed = in.readByte() != 0;
    }
}