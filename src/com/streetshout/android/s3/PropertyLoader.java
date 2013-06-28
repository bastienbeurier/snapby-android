package com.streetshout.android.s3;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.util.Properties;

public class PropertyLoader {
    private boolean hasCredentials = false;
    private String tokenVendingMachineURL = null;
    private boolean useSSL = false;

    private static PropertyLoader instance = null;
    private Context context = null;

    public static PropertyLoader getInstance(Context ctx) {
        if ( instance == null ) {
            instance = new PropertyLoader(ctx);
        }

        return instance;
    }

    public PropertyLoader(Context ctx) {
        try {
            Properties properties = new Properties();
            Resources resources = ctx.getResources();
            AssetManager assetManager = resources.getAssets();
            properties.load( assetManager.open( "AwsCredentials.properties" ) );

            this.tokenVendingMachineURL = properties.getProperty( "tokenVendingMachineURL" );
            this.useSSL = Boolean.parseBoolean( properties.getProperty( "useSSL" ) );

            if ( this.tokenVendingMachineURL == null || this.tokenVendingMachineURL.equals( "" ) || this.tokenVendingMachineURL.equals( "CHANGEME" ) ) {
                this.tokenVendingMachineURL = null;
                this.useSSL = false;
                this.hasCredentials = false;
            }
            else {
                this.hasCredentials = true;
            }
        }
        catch ( Exception exception ) {
            Log.e("PropertyLoader", "Unable to read property file.");
        }
    }

    public boolean hasCredentials() {
        return this.hasCredentials;
    }

    public String getTokenVendingMachineURL() {
        return this.tokenVendingMachineURL;
    }

    public boolean useSSL() {
        return this.useSSL;
    }
}
