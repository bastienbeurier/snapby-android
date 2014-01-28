package com.streetshout.android.utils;

import android.content.Context;
import android.location.Location;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.streetshout.android.models.User;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Tools relative to API calls to street-shout-web.
 */
public class ApiUtils {

    private static String getSiteUrl() {
        return Constants.PRODUCTION ? "http://street-shout.herokuapp.com" : "http://dev-street-shout.herokuapp.com";
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

    private static Map<String, Object> enrichParametersWithToken(Context ctx, Map<String, Object> parameters) {
        if (SessionUtils.isSignIn(ctx)) {
            parameters.put("auth_token", SessionUtils.getCurrentUserToken(ctx));
        }

        //TODO else redirect to signin

        return parameters;
    }

    /** API call to create a new shout */
    public static void createShout(Context ctx, AQuery aq, double lat, double lng, String userName, String description, String shoutImageUrl, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/shouts.json";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_name", userName);
        params.put("description", description);
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("device_id", GeneralUtils.getDeviceId(ctx));

        params = enrichParametersWithToken(ctx, params);

        if (shoutImageUrl != null) {
            params.put("image", shoutImageUrl);
        }

        cb.timeout(10000);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    /** API call to retrieve shouts in a zone of the map */
    public static void pullShoutsInZone(AQuery aq, double neLat, double neLng, double swLat, double swLng, AjaxCallback<JSONObject> cb) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("neLat", String.valueOf(neLat));
        params.put("neLng", String.valueOf(neLng));
        params.put("swLat", String.valueOf(swLat));
        params.put("swLng", String.valueOf(swLng));

        String url = getBasePath() + "/bound_box_shouts.json" + encodeParamsAsUrlParams(params);

        cb.timeout(15000);

        aq.ajax(url, JSONObject.class, cb);
    }

    public static void updateUserInfoWithLocation(Context context, AQuery aq, Location lastLocation) {
        User currentUser = SessionUtils.getCurrentUser(context);
        String url = getBasePath() + "/users/" + currentUser.id + ".json";

        Map<String, Object> params = new HashMap<String, Object>();

        if (lastLocation != null) {
            params.put("lat", lastLocation.getLatitude());
            params.put("lng", lastLocation.getLongitude());
        }

        GeneralUtils.enrichParamsWithWithGeneralUserAndDeviceInfo(context, params);
        enrichParametersWithToken(context, params);

        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

        cb.method(AQuery.METHOD_PUT);

        aq.ajax(url, params, JSONObject.class, cb);
    }

    public static void getValidAPIVersion(AQuery aq, AjaxCallback<JSONObject> cb) {
        String url = getBasePath() + "/obsolete_api.json";

        aq.ajax(url, JSONObject.class, cb);
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
}
