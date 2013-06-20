package com.streetshout.android.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.urbanairship.push.PushManager;

public class PushNotificationReceiver extends BroadcastReceiver {

    /**
     * Logging tag
     */
    public static String TAG = "PushNotificationsReceiver";

    /**
     * The method which receives broadcasts for receiver com.gogobot.gogodroid.PushNotificationsReceiver
     * @param context the current context of the receiver (may or may not be the app)
     * @param intent the intent which caused the receiver to react
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // look for others off of PushManager, such as notification received
        if(action.equals(PushManager.ACTION_NOTIFICATION_OPENED)) {
            handleOpen(context, intent);
        }
    }

    /**
     * Handle when a notification is opened
     * @param context the current context of the receiver (may or may not be the app)
     * @param intent the intent which caused the receiver to react
     */
    private void handleOpen(Context context, Intent intent)  {

    }
}
