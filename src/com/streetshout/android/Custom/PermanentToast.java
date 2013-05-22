package com.streetshout.android.Custom;

import android.widget.Toast;

public class PermanentToast {
    private Thread thread = null;
    private Toast mToast = null;

    public PermanentToast(Toast toast) {
        mToast = toast;

        thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    while (true) {
                        mToast.show();
                        sleep(3600);
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
        mToast.cancel();
        thread.interrupt();
    }
}