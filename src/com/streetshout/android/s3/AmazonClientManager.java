package com.streetshout.android.s3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.streetshout.android.aws.tvmclient.AmazonSharedPreferencesWrapper;
import com.streetshout.android.aws.tvmclient.AmazonTVMClient;
import com.streetshout.android.aws.tvmclient.Response;

public class AmazonClientManager {
    private static final String LOG_TAG = "AmazonClientManager";

    private AmazonS3Client s3Client = null;
    private SharedPreferences sharedPreferences = null;

    public AmazonClientManager(SharedPreferences settings) {
        this.sharedPreferences = settings;
    }

    public AmazonS3Client s3(Context ctx) {
        validateCredentials(ctx);
        return s3Client;
    }

    public Response validateCredentials(Context ctx) {

        Response ableToGetToken = Response.SUCCESSFUL;

        if (AmazonSharedPreferencesWrapper
                .areCredentialsExpired(this.sharedPreferences)) {

            synchronized (this) {

                if (AmazonSharedPreferencesWrapper
                        .areCredentialsExpired(this.sharedPreferences)) {

                    Log.i(LOG_TAG, "Credentials were expired.");

                    AmazonTVMClient tvm = new AmazonTVMClient(this.sharedPreferences,
                            PropertyLoader.getInstance(ctx).getTokenVendingMachineURL(),
                            PropertyLoader.getInstance(ctx).useSSL());

                    ableToGetToken = tvm.anonymousRegister();

                    if (ableToGetToken.requestWasSuccessful()) {

                        ableToGetToken = tvm.getToken();

                        if (ableToGetToken.requestWasSuccessful()) {
                            Log.i(LOG_TAG, "Creating New Credentials.");
                            initClients();
                        }
                    }
                }
            }

        } else if (s3Client == null) {

            synchronized (this) {

                if (s3Client == null) {

                    Log.i(LOG_TAG, "Creating New Credentials.");
                    initClients();
                }
            }
        }

        return ableToGetToken;
    }

    private void initClients() {
        AWSCredentials credentials = AmazonSharedPreferencesWrapper
                .getCredentialsFromSharedPreferences(this.sharedPreferences);

        Region region = Region.getRegion(Regions.US_WEST_2);

        s3Client = new AmazonS3Client( credentials );
        s3Client.setRegion(region);
    }
}
