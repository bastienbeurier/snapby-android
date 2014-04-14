package com.streetshout.android.utils;

public class Constants {
    public static boolean PRODUCTION = true;

    public static final boolean ADMIN = false;

    public static final String API = "2";

    public static final int SHOUT_DURATION = 4 * 60 * 60 * 1000;

    public static final int MAX_DESCRIPTION_LENGTH = 140;

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_USERNAME_LENGTH = 1;

    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** Zoom level set when a user clicks on a shout**/
    public static final int REDIRECTION_FROM_CREATE_SHOUT = 16;

    /** Zoom for the initial camera position when we have the user location */
    public static final int EXPLORE_ZOOM = 15;
    public static final int INITIAL_PROFILE_ZOOM = 13;

    /** Minimum radius around the user's location where he can create shout **/
    public static final int SHOUT_RADIUS = 300;

    /** StartActivityForResult codes **/
    public static final int REFINE_LOCATION_ACTIVITY_REQUEST = 13450;
    public static final int EXPLORE_REQUEST = 12376;
    public static final int SETTINGS_REQUEST = 14760;
    public static final int PROFILE_REQUEST = 14555;
    public static final int RESET_PASSWORD_REQUEST = 64783;
    public static final int DISPLAY_SHOUT_REQUEST = 48392;
    public static final int CHOOSE_PROFILE_PICTURE_REQUEST = 37489;
    public static final int FOLLOWERS_REQUEST = 323489;

    /** AWS S3 **/
    public static final String SMALL_SHOUT_IMAGE_URL_PREFIX_DEV = "http://s3.amazonaws.com/shout_development/small/image_";
    public static final String BIG_SHOUT_IMAGE_URL_PREFIX_DEV = "http://s3.amazonaws.com/shout_development/original/image_";
    public static final String SMALL_SHOUT_IMAGE_URL_PREFIX_PROD = "http://s3.amazonaws.com/shout_production1/small/image_";
    public static final String BIG_SHOUT_IMAGE_URL_PREFIX_PROD = "http://s3.amazonaws.com/shout_production1/original/image_";
    public static final String THUMB_PROFILE_PICS_URL_PREFIX_PROD = "http://s3.amazonaws.com/shout_profile_pics/thumb/profile_";
    public static final String THUMB_PROFILE_PICS_URL_PREFIX_DEV = "http://s3.amazonaws.com/shout_profile_pics_dev/thumb/profile_";
    public static final String BIG_PROFILE_PICS_URL_PREFIX_PROD = "http://s3.amazonaws.com/shout_profile_pics/original/profile_";
    public static final String BIG_PROFILE_PICS_URL_PREFIX_DEV = "http://s3.amazonaws.com/shout_profile_pics_dev/original/profile_";
    public static final String SHOUT_ICON = "http://s3.amazonaws.com/shout_resources/app-icon-120.png";

    /** Shout image res **/
    public static final int SHOUT_BIG_RES = 400;
    public static final int PROFILE_PICTURE_RES = 300;

    /** Mixpanel tokens **/
    public static final String PROD_MIXPANEL_TOKEN = "24dc482a232028564063bd3dd7e84e93";
    public static final String DEV_MIXPANEL_TOKEN = "468e53159f354365149b1a46a7ecdec3";
}
