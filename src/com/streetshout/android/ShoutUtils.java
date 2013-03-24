package com.streetshout.android;

import android.appwidget.AppWidgetProviderInfo;
import android.util.Log;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: sorenzi
 * Date: 3/21/13
 * Time: 10:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShoutUtils {

    public static void createShout(AQuery aq, double lat, double lng, String description, AjaxCallback<JSONObject> cb) {
        ApiUtils.createShout(aq, lat, lng, description, cb);
    }
}
