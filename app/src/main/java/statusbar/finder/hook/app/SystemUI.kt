package statusbar.finder.hook.app

import cn.lyric.getter.api.API
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.hook.BaseHook
import statusbar.finder.hook.tool.HookTool.getApplication

/**
 * LyricGetterExt - statusbar.finder.hook.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/1/21 下午3:17
 */
object SystemUI : BaseHook() {
    private val lyricsGetterApi: API = API()

    override fun init() {
        super.init()
        hook()
    }

    private fun hook(classloader: ClassLoader? = null) {
        getApplication {
            DatabaseHelper.init(it.baseContext)
        }
        assert(lyricsGetterApi.hasEnable)
    }
}
