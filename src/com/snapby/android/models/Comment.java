package com.snapby.android.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 1/29/14.
 */
public class Comment {
    public int snapbyId = 0;

    public int snapbyerId = 0;

    public int commenterId = 0;

    public String commenterUsername = "";

    public String description = "";

    public double lat = 0;

    public double lng = 0;

    public String created = "";

    public int commenterScore = 0;

    public static ArrayList<Comment> rawCommentsToInstances(JSONArray rawComments) {
        ArrayList<Comment> comments = new ArrayList<Comment>();

        int len = rawComments.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawComment = rawComments.getJSONObject(i);
                if (rawComment != null) {
                    Comment comment = rawCommentToInstance(rawComment);
                    comments.add(comment);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return comments;
    }

    public static Comment rawCommentToInstance(JSONObject rawComment) {
        Comment comment = new Comment();

        try {
            if (rawComment != null) {
                comment.snapbyId = Integer.parseInt(rawComment.getString("snapby_id"));
                comment.snapbyerId = Integer.parseInt(rawComment.getString("snapbyer_id"));
                comment.commenterId = Integer.parseInt(rawComment.getString("commenter_id"));
                comment.created = rawComment.getString("created_at");
                comment.commenterUsername = rawComment.getString("commenter_username");
                comment.description = rawComment.getString("description");

                String lat = rawComment.getString("lat");
                String lng = rawComment.getString("lng");

                comment.lat = lat.equals("null") ? 0 : Double.parseDouble(lat);
                comment.lng = lng.equals("null") ? 0 : Double.parseDouble(lng);

                if (rawComment.has("commenter_score") && !rawComment.getString("commenter_score").equals("null")) {
                    comment.commenterScore = Integer.parseInt(rawComment.getString("commenter_score"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return comment;
    }
}
