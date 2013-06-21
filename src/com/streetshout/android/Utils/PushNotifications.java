package com.streetshout.android.Utils;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

import java.util.HashMap;

public class PushNotifications {
    public static void initialize(Application appCtx) {
        //Start Urban Airship push notifications
        AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(appCtx);
        if (Constants.PRODUCTION) {
            options.inProduction = true;
        } else {
            options.inProduction = false;
        }
        UAirship.takeOff(appCtx, options);

//        // Status bar Icon
//        BasicPushNotificationBuilder nb = new BasicPushNotificationBuilder() {
//            @Override
//            public Notification buildNotification(String alert,
//                                                  Map<String, String> extras) {
//                Notification notification = super.buildNotification(alert,
//                        extras);
//                // The icon displayed in the status bar
//                notification.icon = R.drawable.status_bar_ic;
//                // The icon displayed within the notification content
//                notification.contentView.setImageViewResource(
//                        android.R.id.icon, R.drawable.status_bar_ic);
//                return notification;
//            }
//        };
//        // Set the custom notification builder
//        PushManager.shared().setNotificationBuilder(nb);
//        PushPreferences prefs = PushManager.shared().getPreferences();
//
//
//        // enable vibration etc.
//        prefs.setSoundEnabled(true);
//        prefs.setVibrateEnabled(true);

//        // handle when a notification actually comes in
//
//        PushManager.shared().setNotificationBuilder(nb);
//        PushManager.shared().setIntentReceiver(PushNotificationsReceiver.class);

        // enable or disable
        PushManager.enablePush();
        //TODO: DELETE
        Log.d("BAB", "My Application onCreate - App APID: " + getPushToken());
    }

    public static String getPushToken() {
        return PushManager.shared().getAPID();
    }

    public static HashMap<String, Object> getDeviceInfo(Context ctx) {
        HashMap<String, Object> deviceInfo = new HashMap<String, Object>();

        deviceInfo.put("api_version", Constants.API);
        deviceInfo.put("app_version", GeneralUtils.getAppVersion(ctx));
        deviceInfo.put("device_id", Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID));
        deviceInfo.put("device_model", Build.BRAND + " " + Build.PRODUCT);
        deviceInfo.put("os_version", Build.VERSION.RELEASE);
        deviceInfo.put("os_type", "android");
        deviceInfo.put("push_token", PushNotifications.getPushToken());

        return deviceInfo;
    }
}
