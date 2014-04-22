package com.snapby.android.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.snapby.android.models.Snapby;
import com.snapby.android.models.User;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools relative to API calls to snapby-web.
 */
public class ApiUtils {

    public static String getSiteUrl() {
        return Constants.PRODUCTION ? "http://snapby-web.herokuapp.com" : "http://dev-snapby-web.herokuapp.com";
    }

    public static String getUserSiteUrl() {
        return Constants.PRODUCTION ? "http://www.snapby.co" : "http://dev-snapby-web.herokuapp.com";
    }

    private static String getBasePath() {
        return getSiteUrl() + "/api/v" + Constants.API;
    }

    private static String encodeParamsAsUrlParams(Map<String, Object> parameters) {
        if (parameters.size() == 0) {
            return "";
        }

        String result = "?";
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            result += entry.getKey() + "=" + entry.getValue().toString() + "&";
        }

        return result.substring(0, result.length() - 1);
    }

    private static Map<String, Object> enrichParametersWithToken(Activity activity, Map<String, Object> parameters) {
        if (SessionUtils.isSignIn(activity)) {
            parameters.put("auth_token", SessionUtils.getCurrentUserToken(activity));
            return parameters;
        } else {
            SessionUtils.logOut(activity);
            return null;
        }
    }

    private static Map<String, Object> enrichParametersWithToken(Application appCtx, Map<String, Object> parameters) {
        if (SessionUtils.isSignIn(appCtx)) {
            parameters.put("auth_token", SessionUtils.getCurrentUserToken(appCtx));
            return parameters;
        } else {
            return null;
        }
    }

    /** API call to create a new snapby */
    public static void createSnapby(Activity activity, AQuery aq, double lat, double lng, String description, boolean anonymousUser, String image, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/snapbies.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("username", SessionUtils.getCurrentUser(activity).username);
        params.put("user_id", SessionUtils.getCurrentUser(activity).id);
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("anonymous", anonymousUser ? 1 : 0);
        params.put("avatar", image);

        params = enrichParametersWithToken(activity, params);

        if (params == null) return;

        cb.timeout(10000);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    /** API call to retrieve snapbies in a zone of the map */
    public static void pullSnapbiesInZone(AQuery aq, double neLat, double neLng, double swLat, double swLng, int page, int pageSize, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("neLat", String.valueOf(neLat));
        params.put("neLng", String.valueOf(neLng));
        params.put("swLat", String.valueOf(swLat));
        params.put("swLng", String.valueOf(swLng));
        params.put("page", page);
        params.put("page_size", pageSize);

        String url = getBasePath() + "/bound_box_snapbies.json" + encodeParamsAsUrlParams(params);

        cb.timeout(15000);

        aq.ajax(url, JSONObject.class, cb);
    }

    public static void updateUserInfoWithLocation(Activity activity, AQuery aq, Location lastLocation, String image, String username, AjaxCallback<JSONObject> cb) {
        User currentUser = SessionUtils.getCurrentUser(activity);

        if (currentUser == null) {
            return;
        }

        String url = getBasePath() + "/users/" + currentUser.id + ".json";

        Map<String, Object> params = new HashMap<String, Object>();

        if (lastLocation != null) {
            params.put("lat", lastLocation.getLatitude());
            params.put("lng", lastLocation.getLongitude());
        }

        if (image != null) {
            params.put("avatar", image);
        }

        if (username != null) {
            params.put("username", username);
        }

        GeneralUtils.enrichParamsWithWithGeneralUserAndDeviceInfo(activity, params);
        enrichParametersWithToken(activity, params);

        if (params == null) return;

        cb.method(AQuery.METHOD_PUT);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void updateUserInfoWithLocationFromNotif(Application appCtx, AQuery aq, Location lastLocation) {
        User currentUser = SessionUtils.getCurrentUser(appCtx);
        String url = getBasePath() + "/users/" + currentUser.id + ".json";

        Map<String, Object> params = new HashMap<String, Object>();

        if (lastLocation != null) {
            params.put("lat", lastLocation.getLatitude());
            params.put("lng", lastLocation.getLongitude());
        }

        GeneralUtils.enrichParamsWithWithGeneralUserAndDeviceInfo(appCtx, params);
        enrichParametersWithToken(appCtx, params);

        if (params == null) return;

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

        cb.method(AQuery.METHOD_PUT);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void signinWithEmail(AQuery aq, String email, String password, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/users/sign_in.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("email", email);
        params.put("password", password);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void signupWithEmail(Context ctx, AQuery aq, String username, String email, String password, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/users.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("email", email);
        params.put("password", password);
        params.put("username", username);

        GeneralUtils.enrichParamsWithWithGeneralUserAndDeviceInfo(ctx, params);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void connectFacebook(Context context, AQuery aq, String username, String email, String facebookId, String facebookName, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/users/facebook_create_or_update.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("email", email);
        params.put("username", username);
        params.put("facebook_id", facebookId);
        params.put("facebook_name", facebookName);

        GeneralUtils.enrichParamsWithWithGeneralUserAndDeviceInfo(context, params);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void sendResetPasswordInstructions(AQuery aq, String email, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/users/password.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("email", String.valueOf(email));

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void getComments(Activity activity, Snapby snapby, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapby.id);
        params = enrichParametersWithToken(activity, params);

        if (params == null) {
            return;
        }

        //TODO: Remove token from url param
        String url = getBasePath() + "/comments.json" + encodeParamsAsUrlParams(params);

        GeneralUtils.getAquery(activity).ajax(url, JSONObject.class, cb);
    }

    public static void reportSnapby(Activity activity, int snapbyId, String motive, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/flags.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapbyId);
        params.put("motive", motive);
        params.put("flagger_id", SessionUtils.getCurrentUser(activity).id);

        params = enrichParametersWithToken(activity, params);
        if (params == null) {
            return;
        }

        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void createLike(Activity activity, Snapby snapby, double lat, double lng, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/likes.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapby.id);

        if (lat != 0 && lng != 0) {
            params.put("lat", lat);
            params.put("lng", lng);
        }

        params = enrichParametersWithToken(activity, params);
        if (params == null) {
            return;
        }

        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void createComment(Activity activity, String description, Snapby snapby, double lat, double lng, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/comments.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapby.id);
        params.put("snapbyer_id", snapby.userId);
        params.put("description", description);

        if (lat != 0 && lng != 0) {
            params.put("lat", lat);
            params.put("lng", lng);
        }

        params = enrichParametersWithToken(activity, params);
        if (params == null) {
            return;
        }

        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void removeSnapby(Activity activity, int snapbyId, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/snapbies/remove.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapbyId);

        params = enrichParametersWithToken(activity, params);
        if (params == null) {
            return;
        }

        cb.method(AQuery.METHOD_PUT);

        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void removeLike(Activity activity, int snapbyId, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/likes/delete.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("snapby_id", snapbyId);

        params = enrichParametersWithToken(activity, params);

        if (params == null) {
            return;
        }

        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void getUserInfo(Activity activity, int userId, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_id", userId);

        enrichParametersWithToken(activity, params);

        if (params == null) {
            return;
        }

        String url = getBasePath() + "/users/get_user_info.json";

        //Must be a POST request to avoid putting token in url
        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }

    public static void getMyLikes(Activity activity, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();

        enrichParametersWithToken(activity, params);

        if (params == null) {
            return;
        }

        String url = getBasePath() + "/users/my_likes.json";

        //Must be a POST request to avoid putting token in url
        GeneralUtils.getAquery(activity).ajax(url, params, JSONObject.class, cb);
    }


    public static void getSnapbies(Activity activity, int userId, int page, int pageSize, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_id", userId);
        params.put("page", page);
        params.put("page_size", pageSize);

        params = enrichParametersWithToken(activity, params);

        if (params == null) {
            return;
        }

        //TODO: Remove token from url param
        String url = getBasePath() + "/snapbies.json" + encodeParamsAsUrlParams(params);

        GeneralUtils.getAquery(activity).ajax(url, JSONObject.class, cb);
    }

    public static void getLocalSnapbiesCount(Activity activity, double neLat, double neLng, double swLat, double swLng, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("neLat", String.valueOf(neLat));
        params.put("neLng", String.valueOf(neLng));
        params.put("swLat", String.valueOf(swLat));
        params.put("swLng", String.valueOf(swLng));

        //TODO: Remove token from url param
        String url = getBasePath() + "/local_snapbies_count.json" + encodeParamsAsUrlParams(params);

        GeneralUtils.getAquery(activity).ajax(url, JSONObject.class, cb);
    }
}
