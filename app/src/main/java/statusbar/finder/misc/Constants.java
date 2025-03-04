package statusbar.finder.misc;

import statusbar.finder.BuildConfig;

public class Constants {
    public static final int NOTIFICATION_ID_LRC = 1;
    public static final int MSG_LYRIC_UPDATE_DONE = 2;

    public static final String PREFERENCE_KEY_ENABLED = "enabled";
    public static final String PREFERENCE_KEY_CONNECTION_STATUS = "connection_status";
    public static final String PREFERENCE_KEY_TRANSLATE_TYPE = "translate_type";
    public static final String PREFERENCE_KEY_ABOUT = "about";
    public static final String PREFERENCE_KEY_TARGET_PACKAGES = "target_packages";
    public static final String PREFERENCE_KEY_FORCE_REPEAT = "force_repeat";

    public static final String NOTIFICATION_CHANNEL_LRC = "lrc";

    public static final int FLAG_ALWAYS_SHOW_TICKER = 0x1000000;
    public static final int FLAG_ONLY_UPDATE_TICKER = 0x2000000;

    public static final String SETTINGS_ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";


    public static final String BROADCAST_LYRICS_CHANGED = String.format("%s.LYRICS_CHANGED", BuildConfig.APPLICATION_ID);
    public static final String BROADCAST_LYRIC_SENTENCE_UPDATE = String.format("%s.LYRICS_SENTENCE_UPDATED", BuildConfig.APPLICATION_ID);
}
