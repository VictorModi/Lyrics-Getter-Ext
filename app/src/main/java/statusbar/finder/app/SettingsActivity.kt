package statusbar.finder.app

/**
 * LyricGetterExt - statusbar.finder.app
 * @description
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/18 20:06
 */

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.*
import cn.lyric.getter.api.API
import statusbar.finder.BuildConfig
import statusbar.finder.R
import statusbar.finder.config.Config
import statusbar.finder.hook.tool.Tool
import statusbar.finder.hook.tool.Tool.xpActivation
import statusbar.finder.misc.Constants

class SettingsActivity : FragmentActivity() {

    companion object {
        private val mUrlMap: MutableMap<String, String> = mutableMapOf(
            "app" to "https://github.com/VictorModi/Lyrics-Getter-Ext",
            "statusbarlyricext" to "https://github.com/KaguraRinko/StatusBarLyricExt",
            "lyricview" to "https://github.com/markzhai/LyricView"
        )
        val lyricsGetterApiHasEnable: Boolean = API().hasEnable

        private var config: Config = Config()

        fun enableNotification(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                    putExtra("app_package", context.packageName)
                    putExtra("app_uid", context.applicationInfo.uid)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }

        private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }

        @SuppressLint("BatteryLife")
        fun requestIgnoreBatteryOptimizations(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        private fun isNotificationListenerEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, Constants.SETTINGS_ENABLED_NOTIFICATION_LISTENERS)
            return flat?.split(":")?.any { ComponentName.unflattenFromString(it)?.packageName == pkgName } ?: false
        }

        private fun getAppVersionName(context: Context): String? {
            return try {
                val pm = context.packageManager
                pm.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.collapsing_toolbar_base_layout)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commit()
        }

        val collapsingToolbar: Toolbar = findViewById(R.id.action_bar)
        setActionBar(collapsingToolbar)

        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_LRC, "LRC", NotificationManager.IMPORTANCE_MIN
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.createNotificationChannel(channel)
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
        private lateinit var mEnabledPreference: SwitchPreference
        private lateinit var mConnectionStatusPreference: SwitchPreference
        private lateinit var mForceRepeatPreference: SwitchPreference
        private lateinit var mTranslateListPreference: ListPreference

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val context = requireContext()
            val manager = NotificationManagerCompat.from(context)
            if (!xpActivation) {
                if (!manager.areNotificationsEnabled()) {
                    Toast.makeText(context, R.string.toast_get_notification_permission, Toast.LENGTH_LONG).show()
                    enableNotification(context)
                }

                if (!isIgnoringBatteryOptimizations(context)) {
                    requestIgnoreBatteryOptimizations(context)
                }
            }

            manager.cancelAll()

            mEnabledPreference = findPreference(Constants.PREFERENCE_KEY_ENABLED)!!
            mConnectionStatusPreference = findPreference(Constants.PREFERENCE_KEY_CONNECTION_STATUS)!!
            mTranslateListPreference = findPreference(Constants.PREFERENCE_KEY_TRANSLATE_TYPE)!!
            mForceRepeatPreference = findPreference(Constants.PREFERENCE_KEY_FORCE_REPEAT)!!
            mConnectionStatusPreference.notifyDependencyChange(false)

            mConnectionStatusPreference.apply {
                summary = lyricsGetterApiHasEnable.toString()
                isChecked = lyricsGetterApiHasEnable
                onPreferenceClickListener = this@SettingsFragment
            }

            mEnabledPreference.apply {
                isChecked = isNotificationListenerEnabled(context)
                notifyDependencyChange(false)
                isEnabled = true
                onPreferenceClickListener = this@SettingsFragment
                if (xpActivation) {
                    isEnabled = true
                    isChecked = false
                    summary = "由于 Xposed 的启用被禁用"
                }
            }

            mTranslateListPreference.apply {
                onPreferenceClickListener = this@SettingsFragment
                setOnPreferenceChangeListener { _, newValue ->
                    if (!xpActivation) return@setOnPreferenceChangeListener true
                    Log.i("TranslateList", newValue as String)
                    Log.i("xTranslateList", config.translateDisplayType)
                    config.translateDisplayType = newValue
                    true
                }
            }

            mForceRepeatPreference.apply {
                onPreferenceClickListener = this@SettingsFragment
                setOnPreferenceChangeListener { _, newValue ->
                    if (!xpActivation) return@setOnPreferenceChangeListener true
                    config.forceRepeat = newValue as Boolean
                    true
                }
            }

            findPreference<Preference>("app")?.apply {
                summary = getAppVersionName(context)
                isEnabled = true
            }

            findPreference<PreferenceCategory>(Constants.PREFERENCE_KEY_ABOUT)?.let { category ->
                for (i in 0 until category.preferenceCount) {
                    category.getPreference(i).onPreferenceClickListener = this@SettingsFragment
                }
            }
        }

        override fun onResume() {
            super.onResume()
            context?.let {
                mEnabledPreference.isChecked = xpActivation || isNotificationListenerEnabled(it)
                mEnabledPreference.isEnabled = !xpActivation
            }
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            val context = requireContext()
            when (preference) {
                mEnabledPreference -> startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                mConnectionStatusPreference -> {
                    mConnectionStatusPreference.isChecked = lyricsGetterApiHasEnable
                    try {
                        startActivity(Intent().setComponent(ComponentName("cn.lyric.getter", "cn.lyric.getter.ui.activity.MainActivity")))
                    } catch (e: Exception) {
                        Toast.makeText(context, R.string.toast_cannot_start_lyricsgetter, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {
                    mUrlMap[preference.key]?.let {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                }
            }
            return true
        }
    }
}
