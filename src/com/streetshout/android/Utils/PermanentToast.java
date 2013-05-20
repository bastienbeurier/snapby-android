package com.streetshout.android.Utils;

import android.widget.Toast;

public class PermanentToast {
    private Thread thread = null;
    private Toast mToast = null;
    public boolean running = true;

    public PermanentToast(Toast toast) {
        mToast = toast;

        thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (running) {
                            mToast.show();
                            sleep(3600);
                        } else {
                            mToast.cancel();
                            interrupt();
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void start() {
        thread.start();
    }

    public void interrupt() {
        running = false;
    }

}

//import android.util.Log;
//import android.widget.Toast;
//
//public class PermanentToast {
//    private Thread thread = null;
//
//    public PermanentToast(final Toast toast) {
//        thread = new Thread()
//        {
//            @Override
//            public void run() {
//                while (true) {
//                    toast.show();
//                    try {
//                        sleep(3600);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//
//        thread.start();
//    }
//
//    public void stop() {
//        Log.d("BAB", "Thread should stop!");
//        thread.interrupt();
//    }
//}