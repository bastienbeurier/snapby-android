package com.streetshout.android.utils;

public class Constants {
    public static final boolean PRODUCTION = false;

    public static final boolean ADMIN = false;

    public static final String API = "1.0";

    public static final int SHOUT_DURATION = 24 * 60 * 60 * 1000;

    public static final int MAX_USER_NAME_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    /** Zoom level set when a user clicks on a shout**/
    public static final int CLICK_ON_SHOUT_IN_SHOUT = 16;
    public static final int CLICK_ON_SHOUT_IN_MAP_OR_FEED = 12;
    public static final int REDIRECTION_FROM_CREATE_SHOUT = 16;
    public static final int REDIRECTION_FROM_NOTIFICATION = 12;

    /** Zoom for the initial camera position when we have the user location */
    public static final int INITIAL_ZOOM = 0;

    /** Minimum radius around the user's location where he can create shout **/
    public static final int SHOUT_RADIUS = 300;

    /** StartActivityForResult codes **/
    public static final int NEW_SHOUT_CONTENT_ACTIVITY_REQUEST = 13450;
    public static final int CREATE_SHOUT_REQUEST = 11101;
    public static final int SETTINGS_REQUEST = 14760;
    public static final int UPLOAD_PHOTO_REQUEST = 15849;
    public static final int DISPLAY_PHOTO_REQUEST = 47308;
    public static final int IMAGE_EDITOR_REQUEST = 34072;

    /** AWS S3 **/
    public static final String PICTURE_BUCKET = "street-shout1";
    public static final String S3_URL = "street-shout1.s3.amazonaws.com/";

    /** Shout image res **/
    public static final int SHOUT_BIG_RES = 400;
}
