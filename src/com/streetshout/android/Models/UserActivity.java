package com.streetshout.android.models;

import com.streetshout.android.utils.Constants;
import com.streetshout.android.utils.GeneralUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bastien on 4/2/14.
 */
public class UserActivity {
    /** Activity id */
    public int id = 0;

    /** Activity type */
    public String message = null;

    /** Activity image */
    public String image = null;

    /** Activity creation date/time */
    public String created = "";

    /** Activity extra */
    public JSONObject extra = null;

    /** Activity redirect object type */
    public String redirectType = "";

    /** Activity redirect id */
    public Integer redirectId = 0;

    /** Turns a JSONArray received from the API to an ArrayList of UserModel instances */
    public static ArrayList<UserActivity> rawUserActivitiesToInstances(JSONArray rawUserActivities) {
        ArrayList<UserActivity> userActivities = new ArrayList<UserActivity>();

        int len = rawUserActivities.length();
        for (int i = 0; i < len; i++) {
            try {
                JSONObject rawUserActivity = rawUserActivities.getJSONObject(i);
                if (rawUserActivity != null) {
                    UserActivity userActivity = rawUserActivityToInstance(rawUserActivity);

                    if (userActivities != null) {
                        userActivities.add(userActivity);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return userActivities;
    }

    public static UserActivity rawUserActivityToInstance(JSONObject rawUserActivity) {
        UserActivity userActivity = new UserActivity();

        try {
            if (rawUserActivity != null) {
                userActivity.id = Integer.parseInt(rawUserActivity.getString("id"));
                userActivity.created = rawUserActivity.getString("created_at");
                String type = rawUserActivity.getString("activity_type");

                try {
                    userActivity.extra = rawUserActivity.getJSONObject("extra");
                } catch (JSONException e) {
                }

                if (type.equals("nearby_shout")) {
                    String shoutId = userActivity.extra.getString("shout_id");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "New shout in your area.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("shout_by_followed")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    String shoutUsername = userActivity.extra.getString("shouter_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "New shout by @" + shoutUsername + " in your area.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("nearby_shout_trending")) {
                    String shoutId = userActivity.extra.getString("shout_id");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "A shout is now trending in your area!";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("shout_by_followed_trending")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    String shoutUsername = userActivity.extra.getString("shouter_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "@" + shoutUsername + "'s shout is now trending.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("my_shout_trending")) {
                    String shoutId = userActivity.extra.getString("shout_id");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "Your shout is now trending!";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("commenter_shout_commented")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    String commenterUsername = userActivity.extra.getString("commenter_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "New comment from " + commenterUsername + " on the shout you commented.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("liker_shout_commented")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    String commenterUsername = userActivity.extra.getString("commenter_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "New comment from " + commenterUsername + " on the shout you liked.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("my_shout_commented")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    String commenterUsername = userActivity.extra.getString("commenter_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";
                    userActivity.message = "New comment from " + commenterUsername + " on your shout.";

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("my_shout_liked")) {
                    String shoutId = userActivity.extra.getString("shout_id");
                    Integer likeCount = Integer.parseInt(userActivity.extra.getString("like_count"));
                    String likerUsername = userActivity.extra.getString("liker_username");

                    userActivity.image = GeneralUtils.getShoutSmallPicturePrefix() + shoutId + "--400";

                    if (likeCount <= 1) {
                        userActivity.message = likerUsername + " likes" + " your shout.";
                    } else {
                        userActivity.message = likerUsername + " and"  + (likeCount - 1) + " others like your shout.";
                    }

                    userActivity.redirectType = "Shout";
                    userActivity.redirectId = Integer.parseInt(shoutId);
                } else if (type.equals("new_facebook_friend")) {
                    String userId = userActivity.extra.getString("user_id");
                    String username = userActivity.extra.getString("username");
                    String facebookName = userActivity.extra.getString("facebook_name");

                    userActivity.image = GeneralUtils.getProfileThumbPicturePrefix() + userId;
                    userActivity.message = facebookName + " joined Shout as @" + username + ".";

                    userActivity.redirectType = "User";
                    userActivity.redirectId = Integer.parseInt(userId);
                } else if (type.equals("new_follower")) {
                    String userId = userActivity.extra.getString("user_id");
                    String username = userActivity.extra.getString("username");

                    userActivity.image = GeneralUtils.getProfileThumbPicturePrefix() + userId;
                    userActivity.message = username + " is now following you.";

                    userActivity.redirectType = "User";
                    userActivity.redirectId = Integer.parseInt(userId);
                } else if (type.equals("welcome")) {
                    userActivity.redirectType = "Welcome";

                    userActivity.image = Constants.SHOUT_ICON;
                    userActivity.message = "Welcome to the Shout planet, where everything happens here and now!";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return userActivity;
    }
}
