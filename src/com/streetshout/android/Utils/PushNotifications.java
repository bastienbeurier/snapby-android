package com.streetshout.android.utils;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import com.androidquery.AQuery;
import com.streetshout.android.R;
import com.streetshout.android.receivers.PushNotificationReceiver;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;
import com.urbanairship.push.BasicPushNotificationBuilder;
import com.urbanairship.push.PushManager;
import com.urbanairship.push.PushPreferences;

import java.util.HashMap;
import java.util.Map;

public class PushNotifications {
    public static void initialize(final Application appCtx) {
        //Start Urban Airship push notifications
        AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(appCtx);
        if (Constants.PRODUCTION) {
            options.inProduction = true;
        } else {
            options.inProduction = false;
        }
        UAirship.takeOff(appCtx, options);

        final AQuery aq = new AQuery(appCtx);

        // Status bar Icon
        BasicPushNotificationBuilder nb = new BasicPushNotificationBuilder() {
            @Override
            public Notification buildNotification(String alert, Map<String, String> extras) {
                Notification notification = super.buildNotification(alert,
                        extras);
                // The icon displayed in the status bar
                notification.icon = R.drawable.ic_stat_notify_shout;
                // The icon displayed within the notification content
                notification.contentView.setImageViewResource(android.R.id.icon, R.drawable.ic_stat_notify_shout);

                //Send recent location when user receives a notification
                LocationManager locationManager = (LocationManager) appCtx.getSystemService(appCtx.LOCATION_SERVICE);
                if (locationManager != null) {
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    String provider = locationManager.getBestProvider(criteria, true);
                    if (provider != null) {
                        Location location = locationManager.getLastKnownLocation(provider);
                        if (location != null) {
                            //TODO: send user info
//                            ApiUtils.sendDeviceInfo(appCtx, aq, location);
                        }
                    }
                }

                return notification;
            }

            @Override
            public int getNextId(String alert, Map<String, String> extras) {
                return 1001; // Always update the single notification
            }
        };

        nb.appName = appCtx.getString(R.string.status_bar_notification_title);

        // Set the custom notification builder
        PushManager.shared().setNotificationBuilder(nb);
        PushPreferences prefs = PushManager.shared().getPreferences();


        // enable vibration etc.
        prefs.setSoundEnabled(true);
        prefs.setVibrateEnabled(true);

        // handle when a notification actually comes in

        PushManager.shared().setNotificationBuilder(nb);
        PushManager.shared().setIntentReceiver(PushNotificationReceiver.class);

        // enable or disable
        PushManager.enablePush();
    }

    public static String getPushToken() {
        return PushManager.shared().getAPID();
    }

    public static HashMap<String, Object> getDeviceInfo(Context ctx) {
        HashMap<String, Object> deviceInfo = new HashMap<String, Object>();

        deviceInfo.put("device_id", GeneralUtils.getDeviceId(ctx));
        deviceInfo.put("push_token", PushNotifications.getPushToken());
        deviceInfo.put("device_model", Build.BRAND + " " + Build.PRODUCT);
        deviceInfo.put("os_version", Build.VERSION.RELEASE);
        deviceInfo.put("os_type", "android");
        deviceInfo.put("app_version", GeneralUtils.getAppVersion(ctx));
        deviceInfo.put("api_version", Constants.API);

        return deviceInfo;
    }
}
