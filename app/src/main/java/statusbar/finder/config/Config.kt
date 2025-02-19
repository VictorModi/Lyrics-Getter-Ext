package statusbar.finder.config

import cn.xiaowine.dsp.delegate.Delegate.serialLazy

class Config {
    var translateDisplayType: String by serialLazy("origin")
    var targetPackages: String by serialLazy("")
    var forceRepeat: Boolean by serialLazy(false)
}
