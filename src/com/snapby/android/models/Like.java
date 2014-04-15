package com.snapby.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class Like {

    public int shoutId = 0;

    public int likerId = 0;

    public String likerUsername = "";

    public double lat = 0;

    public double lng = 0;

    public String created = "";

    public static ArrayList<Like> rawLikesToInstances(JSONArray rawLikes) {
        ArrayList<Like> likes = new ArrayList<Like>();

        int len = rawLikes.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawLike = rawLikes.getJSONObject(i);
                if (rawLike != null) {
                    Like like = rawLikeToInstance(rawLike);
                    likes.add(like);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return likes;
    }

    public static Like rawLikeToInstance(JSONObject rawLike) {
        Like like = new Like();

        try {
            if (rawLike != null) {
                like.shoutId = Integer.parseInt(rawLike.getString("shout_id"));
                like.likerId = Integer.parseInt(rawLike.getString("liker_id"));
                like.created = rawLike.getString("created_at");
                like.likerUsername = rawLike.getString("liker_username");

                String lat = rawLike.getString("lat");
                String lng = rawLike.getString("lng");

                like.lat = lat.equals("null") ? 0 : Double.parseDouble(lat);
                like.lng = lng.equals("null") ? 0 : Double.parseDouble(lng);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return like;
    }

    public static ArrayList<Integer> rawLikerIdsToIntegers(JSONArray rawLikerIds) {
        ArrayList<Integer> likerIds = new ArrayList<Integer>();

        int len = rawLikerIds.length();
        for(int i = 0; i < len; i++) {
            try {
                Integer likerId = Integer.parseInt(rawLikerIds.getString(i));
                if (likerId != null) {
                    likerIds.add(likerId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return likerIds;
    }
}
