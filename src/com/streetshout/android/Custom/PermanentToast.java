package com.streetshout.android.Custom;

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