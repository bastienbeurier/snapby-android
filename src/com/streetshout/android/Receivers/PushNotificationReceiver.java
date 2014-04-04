package com.streetshout.android.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.streetshout.android.activities.CameraActivity;
import com.streetshout.android.activities.ExploreActivity;
import com.streetshout.android.activities.WelcomeActivity;
import com.streetshout.android.models.Shout;
import com.streetshout.android.utils.SessionUtils;
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
        Log.d("BAB", "HANDLE OPEN: " + intent);

        if (!SessionUtils.isSignIn(context) || !intent.hasExtra("notif_type")) {
            Log.d("BAB", "WELCOME");
            redirectToWelcome();
            return;
        }

        String type = intent.getStringExtra("notif_type");

        if (type.equals("new_shout")) {
            Log.d("BAB", "NEW SHOUT");
            redirectToShout(intent.getStringExtra("shout"));
        } else if (type.equals("new_like")) {
            Log.d("BAB", "NEW LIKE");
            redirectToShout(intent.getStringExtra("shout"));
        } else if (type.equals("new_comment")) {
            Log.d("BAB", "NEW COMMENT");
            redirectToShout(intent.getStringExtra("shout"));
        } else if (type.equals("new_friend")) {
            Log.d("BAB", "NEW FRIEND");
            redirectToUser(intent.getStringExtra("user_id"));
        } else if (type.equals("trending")) {
            Log.d("BAB", "NEW USER ID");
            redirectToShout(intent.getStringExtra("shout"));
        } else {
            redirectToWelcome();
        }


    }

    private void redirectToWelcome() {
        Application app	= (Application) UAirship.shared().getApplicationContext();
        Intent start = new Intent(app, WelcomeActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(start);
    }

    private void redirectToShout(String rawShout) {
        Application app	= (Application) UAirship.shared().getApplicationContext();
        Intent start = new Intent(app, CameraActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        start.putExtra("notificationShout", rawShout);
        app.startActivity(start);
    }

    private void redirectToUser(String userId) {
        Application app	= (Application) UAirship.shared().getApplicationContext();
        Intent start = new Intent(app, CameraActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        start.putExtra("notificationUser", userId);
        app.startActivity(start);
    }
}
