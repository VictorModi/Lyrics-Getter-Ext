package statusbar.finder.hook

import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE
import statusbar.finder.BuildConfig

abstract class BaseHook {
    var isInit: Boolean = false
    open fun init() {
        DSP.init(null, BuildConfig.APPLICATION_ID, MODE.HOOK, true)
    }
}
