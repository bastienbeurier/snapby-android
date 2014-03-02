package com.streetshout.android.utils;

public class Constants {
    public static boolean PRODUCTION = false;

    public static final boolean ADMIN = false;

    public static final String API = "2";

    public static final int SHOUT_DURATION = 4 * 60 * 60 * 1000;
    public static final int SHOUT_DURATION_HOURS = 4;

    public static final int MAX_DESCRIPTION_LENGTH = 140;

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_USERNAME_LENGTH = 1;

    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** Zoom level set when a user clicks on a shout**/
    public static final int REDIRECTION_FROM_CREATE_SHOUT = 16;
    public static final int CLICK_ON_MY_LOCATION_BUTTON = 13;

    /** Zoom for the initial camera position when we have the user location */
    public static final int INITIAL_ZOOM = 12;

    /** Minimum radius around the user's location where he can create shout **/
    public static final int SHOUT_RADIUS = 300;

    /** StartActivityForResult codes **/
    public static final int REFINE_LOCATION_ACTIVITY_REQUEST = 13450;
    public static final int CREATE_SHOUT_REQUEST = 11101;
    public static final int EXPLORE_REQUEST = 12376;
    public static final int SETTINGS_REQUEST = 14760;
    public static final int RESET_PASSWORD_REQUEST = 64783;

    /** AWS S3 **/
    public static final String PICTURE_BUCKET = "street-shout1";
    public static final String S3_URL = "street-shout1.s3.amazonaws.com/";

    /** Shout image res **/
    public static final int SHOUT_BIG_RES_SUFFIX = 400;
    public static final int SHOUT_BIG_RES = 800;

    /** Mixpanel tokens **/
    public static final String PROD_MIXPANEL_TOKEN = "24dc482a232028564063bd3dd7e84e93";
    public static final String DEV_MIXPANEL_TOKEN = "468e53159f354365149b1a46a7ecdec3";
}
