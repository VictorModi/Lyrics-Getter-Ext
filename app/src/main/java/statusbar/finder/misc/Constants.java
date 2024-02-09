package statusbar.finder.misc;

public class Constants {
    public static final String SHARED_PREFERENCES_NAME = "preferences";

    public static final String PREFERENCE_KEY_ENABLED = "enabled";
    public static final String PREFERENCE_KEY_CONNECTION_STATUS = "connection_status";
    public static final String PREFERENCE_KEY_TRANSELATE = "translate";
    public static final String PREFERENCE_KEY_ABOUT = "about";
    public static final String PREFERENCE_KEY_TARGET_PACKAGES = "target_packages";

    public static final String NOTIFICATION_CHANNEL_LRC = "lrc";

    public static final int FLAG_ALWAYS_SHOW_TICKER = 0x1000000;
    public static final int FLAG_ONLY_UPDATE_TICKER = 0x2000000;

    public static final String BROADCAST_TARGET_APP_CHANGED = "target_app_changed";

    public static final String SETTINGS_ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    public static boolean isTranslateCheck = false;
}
