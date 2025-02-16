package statusbar.finder.hook.app

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import statusbar.finder.hook.BaseHook
import statusbar.finder.hook.tool.EventTool
import statusbar.finder.hook.tool.HookTool.getApplication

/**
 * LyricGetterExt - statusbar.finder.hook.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/17 00:08
 */
object LyricsGetter : BaseHook() {
    override fun init() {
        super.init()
        getApplication {
            EventTool.setContext(it.baseContext)
            EventTool.sendLyric("测试")
        }
    }
}
