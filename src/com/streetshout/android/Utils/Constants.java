package com.streetshout.android.Utils;

public class Constants {
    public static final boolean PRODUCTION = false;

    public static final boolean ADMIN = false;

    public static final String API = "1.0";

    public static final int MAX_USER_NAME_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    /** Zoom level set when a user clicks on a shout**/
    public static final int CLICK_ON_SHOUT_IN_SHOUT = 16;
    public static final int CLICK_ON_SHOUT_IN_MAP_OR_FEED = 12;
    public static final int CLICK_ON_SHOUT_IN_FEED = 12;
    public static final int REDIRECTION_FROM_CREATE_SHOUT = 16;
    public static final int REDIRECTION_FROM_NOTIFICATION = 12;

    /** Zoom for the initial camera position when we have the user location */
    public static final int INITIAL_ZOOM = 0;

    /** Minimum radius around the user's location where he can create shout **/
    public static final int SHOUT_RADIUS = 300;
}
