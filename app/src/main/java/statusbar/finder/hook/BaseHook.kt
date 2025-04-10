package statusbar.finder.hook

import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE
import com.github.kyuubiran.ezxhelper.Log
import com.hchen.superlyricapi.SuperLyricTool
import statusbar.finder.BuildConfig

abstract class BaseHook {
    var isInit: Boolean = false
    private var isDSPEnabled: Boolean = false
    open fun init() {
        isDSPEnabled = DSP.init(null, BuildConfig.APPLICATION_ID, MODE.HOOK, true)
        Log.i("${BuildConfig.APPLICATION_ID} isDSPEnabled: $isDSPEnabled")
        Log.i("${BuildConfig.APPLICATION_ID} isSuperLyricsEnable ${SuperLyricTool.isEnabled}")
    }
}
