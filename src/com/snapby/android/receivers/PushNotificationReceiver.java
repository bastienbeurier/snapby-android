package com.snapby.android.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.snapby.android.activities.WelcomeActivity;
import com.snapby.android.models.Snapby;
import com.snapby.android.utils.SessionUtils;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

public class PushNotificationReceiver extends BroadcastReceiver {
    public static String TAG = "PushNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // look for others off of PushManager, such as notification received
        if (action.equals(PushManager.ACTION_NOTIFICATION_OPENED)) {
            handleOpen(context, intent);
        }
    }

    private void handleOpen(Context context, Intent intent)  {
        redirectToWelcome();
    }

    private void redirectToWelcome() {
        Application app	= (Application) UAirship.shared().getApplicationContext();
        Intent start = new Intent(app, WelcomeActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(start);
    }
}
