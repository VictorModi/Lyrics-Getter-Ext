package statusbar.finder.hook

import cn.lyric.getter.api.BuildConfig
import cn.xiaowine.dsp.DSP

abstract class BaseHook {
    var isInit: Boolean = false
    open fun init() {
//        DSP.init(null, BuildConfig.APPLICATION_ID, MODE.HOOK, true)
    }
}
