package statusbar.finder.hook.app

import android.app.Application
import android.content.IntentFilter
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import statusbar.finder.hook.BaseHook
import statusbar.finder.hook.broadcast.LyricRequestBroadcastReceiver
import statusbar.finder.hook.observe.MediaSessionManagerHelper
import statusbar.finder.misc.Constants.*

/**
 * LyricGetterExt - statusbar.finder.hook.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/1/21 下午3:17
 */
object SystemUI : BaseHook() {

    override fun init() {
        super.init()
        Application::class.java.methodFinder().filterByName("attach").first().createHook {
            after {
                val application = it.thisObject as Application
                MediaSessionManagerHelper.init(application)
                application.registerReceiver(
                    LyricRequestBroadcastReceiver(),
                    IntentFilter().apply {
                        addAction(BROADCAST_LYRICS_CHANGED_REQUEST)
                        addAction(BROADCAST_LYRICS_OFFSET_UPDATE_REQUEST)
                        addAction(BROADCAST_LYRICS_ACTIVE_UPDATE_REQUEST)
                    }
                )
            }
        }
    }
}
