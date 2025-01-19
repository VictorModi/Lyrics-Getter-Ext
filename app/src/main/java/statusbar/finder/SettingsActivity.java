package statusbar.finder;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;
import android.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.*;
import cn.lyric.getter.api.API;
import org.jetbrains.annotations.NotNull;
import statusbar.finder.misc.Constants;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends FragmentActivity {

    private final static Map<String, String> mUrlMap = new HashMap<>();
    public final static boolean lyricsGetterApiHasEnable = new API().getHasEnable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collapsing_toolbar_base_layout);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new SettingsFragment())
                    .commit();
        }
        Toolbar collapsingToolbar = findViewById(R.id.action_bar);
        setActionBar(collapsingToolbar);


        // add urls
        mUrlMap.put("app", "https://github.com/VictorModi/Lyrics-Getter-Ext");
        mUrlMap.put("statusbarlyricext", "https://github.com/KaguraRinko/StatusBarLyricExt");
        mUrlMap.put("lyricview", "https://github.com/markzhai/LyricView");

        NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_LRC, "LRC", NotificationManager.IMPORTANCE_MIN);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void enableNotification(Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE,context. getPackageName());
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
            context.startActivity(intent);
        } catch (Exception e) {
            e.fillInStackTrace();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package",context. getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }

    private static boolean isIgnoringBatteryOptimizations(Context context) {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return isIgnoring;
    }

    public static void requestIgnoreBatteryOptimizations(Context context) {
        try {
            @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    private static boolean isNotificationListenerEnabled(Context context) {
        if (context == null) return false;
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(), Constants.SETTINGS_ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String getAppVersionName(Context context) {
        String versionName=null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return versionName;
    }

    public static class SettingsFragment
            extends PreferenceFragmentCompat
            implements Preference.OnPreferenceClickListener {

        private SwitchPreference mEnabledPreference;
        private SwitchPreference mConnectionStatusPreference;
        private ListPreference mTranslateListPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            NotificationManagerCompat manager = NotificationManagerCompat.from(requireContext());
            if (!manager.areNotificationsEnabled()) {
                Toast.makeText(requireContext(), R.string.toast_get_notification_permission, Toast.LENGTH_LONG).show();
                enableNotification(requireContext());
            }
            if (!isIgnoringBatteryOptimizations(requireContext())){
                requestIgnoreBatteryOptimizations(requireContext());
            }
            manager.cancelAll();
            mEnabledPreference = findPreference(Constants.PREFERENCE_KEY_ENABLED);
            mConnectionStatusPreference = findPreference(Constants.PREFERENCE_KEY_CONNECTION_STATUS);
            mTranslateListPreference = findPreference(Constants.PREFERENCE_KEY_TRANSLATE_TYPE);
//            try {
//                mNotificationFields[0] =
//                        Notification.class.getDeclaredField("FLAG_ALWAYS_SHOW_TICKER").getInt(null);
//                mNotificationFields[1] =
//                        Notification.class.getDeclaredField("FLAG_ONLY_UPDATE_TICKER").getInt(null);
//            } catch (NoSuchFieldException | IllegalAccessException e) {
//                mEnabledPreference.setEnabled(false);
//                mEnabledPreference.setTitle(R.string.unsupport_rom_title);
//                mEnabledPreference.setSummary(R.string.unsupport_rom_summary);
//            }
//            );
            // boolean lyricsGetterApiHasEnable = true; For Debug

            if (mConnectionStatusPreference != null){
                mConnectionStatusPreference.setSummary(String.valueOf(lyricsGetterApiHasEnable));
                mConnectionStatusPreference.setOnPreferenceClickListener(this);
            }
            if (mEnabledPreference != null) {
                mEnabledPreference.setChecked(isNotificationListenerEnabled(getContext()));
                // mEnabledPreference.setEnabled(lyricsGetterApiHasEnable);
                mEnabledPreference.setEnabled(true);
                mEnabledPreference.setOnPreferenceClickListener(this);
            }
            if (mTranslateListPreference != null) {
                mTranslateListPreference.setOnPreferenceClickListener(this);
            }
            Preference appInfoPreference = findPreference("app");
            if (appInfoPreference != null) {
                appInfoPreference.setSummary(getAppVersionName(getContext()));
            }
            PreferenceCategory aboutCategory = findPreference(Constants.PREFERENCE_KEY_ABOUT);
            if (aboutCategory != null) {
                for (int i = 0; i < aboutCategory.getPreferenceCount(); i++) {
                    aboutCategory.getPreference(i).setOnPreferenceClickListener(this);
                }
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            if (mEnabledPreference != null) {
                mEnabledPreference.setChecked(isNotificationListenerEnabled(getContext()));
            }
        }

        @Override
        public boolean onPreferenceClick(@NotNull Preference preference) {
            if (preference == mEnabledPreference) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            } else if (preference == mConnectionStatusPreference) {
                mConnectionStatusPreference.setChecked(lyricsGetterApiHasEnable);
                Intent intent = new Intent();
                // Open LyricsGetter
                try {
                    intent.setComponent(new ComponentName("cn.lyric.getter", "cn.lyric.getter.ui.activity.MainActivity"));
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.toast_cannot_start_lyricsgetter, Toast.LENGTH_SHORT).show();
                }
                // 启动活动
                return true;
            } else if (preference == mTranslateListPreference) {
//                Toast.makeText(requireContext(), "TranslateLyrics: " + (mTranslateSwitch.isChecked() ? "Enable" : "Disable"), Toast.LENGTH_SHORT).show();
                Toast.makeText(requireContext(), "TranslateLyrics: " + mTranslateListPreference.getValue(), Toast.LENGTH_SHORT).show();
            } else {
                String url = mUrlMap.get(preference.getKey());
                if (TextUtils.isEmpty(url)) return false;
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
            return true;
        }
    }
 }
