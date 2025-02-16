package statusbar.finder.hook.app

import cn.lyric.getter.api.API
import cn.xiaowine.xkt.LogTool.log
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import statusbar.finder.DatabaseHelper
import statusbar.finder.hook.BaseHook

/**
 * LyricGetterExt - statusbar.finder.hook.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/1/21 下午3:17
 */
object SystemUI : BaseHook() {
    private val lyricsGetterAPI: API = API()

    override fun init() {
        super.init()
        if (!lyricsGetterAPI.hasEnable) {
            Log.i("Lyrics Getter API is not enabled.")
        }
    }
}
