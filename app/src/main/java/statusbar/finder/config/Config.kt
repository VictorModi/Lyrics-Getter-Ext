package statusbar.finder.config

import cn.xiaowine.dsp.delegate.Delegate.serial

class Config {
    var translateDisplayType: String by serial("origin")
    var targetPackages: String by serial("")
    var forceRepeat: Boolean by serial(false)
}
