package com.snapby.android.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.snapby.android.activities.WelcomeActivity;
import com.snapby.android.models.Shout;
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
        Log.d("BAB", "HANDLE OPEN: " + intent);

//        if (!SessionUtils.isSignIn(context) || !intent.hasExtra("notif_type")) {
//            Log.d("BAB", "WELCOME");
//            redirectToWelcome();
//            return;
//        }
//
//        String type = intent.getStringExtra("notif_type");
//
//        if (type.equals("new_shout")) {
//            if (intent.hasExtra("shout_id")) {
//                redirectToShout(Integer.parseInt(intent.getStringExtra("shout_id")));
//            } else {
//                redirectToShout(intent.getStringExtra("shout"));
//            }
//        } else if (type.equals("new_like")) {
//            if (intent.hasExtra("shout_id")) {
//                redirectToShout(Integer.parseInt(intent.getStringExtra("shout_id")));
//            } else {
//                redirectToShout(intent.getStringExtra("shout"));
//            }
//        } else if (type.equals("new_comment")) {
//            if (intent.hasExtra("shout_id")) {
//                redirectToShout(Integer.parseInt(intent.getStringExtra("shout_id")));
//            } else {
//                redirectToShout(intent.getStringExtra("shout"));
//            }
//        } else if (type.equals("new_friend")) {
//            redirectToUser(intent.getStringExtra("user_id"));
//        } else if (type.equals("trending")) {
//            if (intent.hasExtra("shout_id")) {
//                redirectToShout(Integer.parseInt(intent.getStringExtra("shout_id")));
//            } else {
//                redirectToShout(intent.getStringExtra("shout"));
//            }
//        } else {
//            redirectToWelcome();
//        }
//

    }

//    private void redirectToWelcome() {
//        Application app	= (Application) UAirship.shared().getApplicationContext();
//        Intent start = new Intent(app, WelcomeActivity.class);
//        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        app.startActivity(start);
//    }
//
//    private void redirectToShout(String rawShout) {
//        Application app	= (Application) UAirship.shared().getApplicationContext();
//        Intent start = new Intent(app, CameraActivity.class);
//        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        start.putExtra("notificationShout", rawShout);
//        app.startActivity(start);
//    }
//
//    private void redirectToShout(int shoutId) {
//        Application app	= (Application) UAirship.shared().getApplicationContext();
//        Intent start = new Intent(app, CameraActivity.class);
//        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        start.putExtra("notificationShoutId", shoutId);
//        app.startActivity(start);
//    }
//
//    private void redirectToUser(String userId) {
//        Application app	= (Application) UAirship.shared().getApplicationContext();
//        Intent start = new Intent(app, CameraActivity.class);
//        start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        start.putExtra("notificationUser", userId);
//        app.startActivity(start);
//    }
}
