package statusbar.finder.hook.app

import android.app.Application
import cn.xiaowine.dsp.DSP
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XSharedPreferences
import statusbar.finder.BuildConfig
import statusbar.finder.hook.BaseHook
import statusbar.finder.hook.observe.MediaSessionManagerHelper

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
                MediaSessionManagerHelper.initByContext(it.thisObject as Application)
            }
        }
    }
}
