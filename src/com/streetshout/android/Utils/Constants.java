package com.streetshout.android.Utils;

public class Constants {
    public static final boolean ADMIN = true;
    public static final int MAX_USER_NAME_LENGTH = 20;
    public static final int MAX_DESCRIPTION_LENGTH = 140;

    public static final int CLICK_ON_SHOUT_ZOOM = 16;

    public static final int REGULAR_MODE = 0;
    public static final int SHOUT_LOCATION_MODE = 1;
    public static final int SHOUT_CONTENT_MODE = 2;

    /** Zoom for the initial camera position when we have the user location */
    public static final int INITIAL_ZOOM = 11;

    /** Minimum radius around the user's location where he can create shout **/
    public static final int SHOUT_RADIUS = 300;
}
