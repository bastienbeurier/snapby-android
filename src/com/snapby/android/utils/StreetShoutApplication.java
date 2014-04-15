package com.snapby.android.utils;


import android.app.Application;
import com.androidquery.AQuery;
import com.androidquery.util.AQUtility;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class StreetShoutApplication extends Application {

    private AppPreferences appPrefs = null;

    private AQuery aq = null;

    private MixpanelAPI mixpanel = null;

    @Override
    public void onCreate() {
        appPrefs = new AppPreferences(getApplicationContext());

        PushNotifications.initialize(this);

        if (Constants.PRODUCTION) {
            AQUtility.setDebug(false);
        } else {
            AQUtility.setDebug(true);
        }

        if (Constants.PRODUCTION) {
            mixpanel = MixpanelAPI.getInstance(this, Constants.PROD_MIXPANEL_TOKEN);
        } else {
            mixpanel = MixpanelAPI.getInstance(this, Constants.DEV_MIXPANEL_TOKEN);
        }
    }

    public AppPreferences getAppPrefs() {
        return appPrefs;
    }

    public AQuery getAQuery() {
        if (aq == null) {
            aq	= new AQuery(this);
        }

        return aq;
    }

    public MixpanelAPI getMixpanel() {
        return mixpanel;
    }
}
