package statusbar.finder.hook.app

import android.media.session.MediaController
import cn.lyric.getter.api.API
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.hook.BaseHook
import statusbar.finder.hook.observe.MediaSessionObserve
import statusbar.finder.hook.tool.EventTool
import statusbar.finder.hook.tool.HookTool.getApplication

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
        getApplication {
            DatabaseHelper.init(it.baseContext)
            EventTool.setContext(it.baseContext)
            MediaSessionObserve(it.baseContext)
        }
    }
}
