package com.snapby.android.utils;

public class Constants {
    public static boolean PRODUCTION = true;

    public static final boolean ADMIN = false;

    public static final String API = "1";

    public static final int SHOUT_DURATION = 4 * 60 * 60 * 1000;

    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_USERNAME_LENGTH = 1;

    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MIN_PASSWORD_LENGTH = 6;

    /** Zoom for the initial camera position when we have the user location */
    public static final int EXPLORE_ZOOM = 15;
    public static final int INITIAL_PROFILE_ZOOM = 13;

    /** Minimum radius around the user's location where he can create snapby **/
    public static final int SHOUT_RADIUS = 300;

    /** StartActivityForResult codes **/
    public static final int REFINE_LOCATION_ACTIVITY_REQUEST = 13450;
    public static final int SETTINGS_REQUEST = 14760;
    public static final int RESET_PASSWORD_REQUEST = 64783;
    public static final int DISPLAY_SHOUT_REQUEST = 48392;
    public static final int CHOOSE_PROFILE_PICTURE_REQUEST = 37489;
    public static final int COMMENTS_REQUEST = 32348;

    /** AWS S3 **/
    public static final String SMALL_SHOUT_IMAGE_URL_PREFIX_DEV = "http://s3.amazonaws.com/snapby_development/small/image_";
    public static final String BIG_SHOUT_IMAGE_URL_PREFIX_DEV = "http://s3.amazonaws.com/snapby_development/original/image_";
    public static final String SMALL_SHOUT_IMAGE_URL_PREFIX_PROD = "http://s3.amazonaws.com/snapby_production/small/image_";
    public static final String BIG_SHOUT_IMAGE_URL_PREFIX_PROD = "http://s3.amazonaws.com/snapby_production/original/image_";
    public static final String THUMB_PROFILE_PICS_URL_PREFIX_PROD = "http://s3.amazonaws.com/snapby_profile_pics/thumb/profile_";
    public static final String THUMB_PROFILE_PICS_URL_PREFIX_DEV = "http://s3.amazonaws.com/snapby_profile_pics_dev/thumb/profile_";
    public static final String BIG_PROFILE_PICS_URL_PREFIX_PROD = "http://s3.amazonaws.com/snapby_profile_pics/original/profile_";
    public static final String BIG_PROFILE_PICS_URL_PREFIX_DEV = "http://s3.amazonaws.com/snapby_profile_pics_dev/original/profile_";

    /** Snapby image res **/
    public static final int SHOUT_BIG_RES = 800;
    public static final int PROFILE_PICTURE_RES = 300;

    /** Mixpanel tokens **/
    public static final String PROD_MIXPANEL_TOKEN = "781f8a3090780f2afbb8a260a58911c4";
    public static final String DEV_MIXPANEL_TOKEN = "293023eb15e4681ca1aa4c81d3a6ce19";
}
