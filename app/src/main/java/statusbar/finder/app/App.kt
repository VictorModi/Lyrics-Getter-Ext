package statusbar.finder.app

import android.app.Application
import android.util.Log
import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE
import statusbar.finder.BuildConfig
import statusbar.finder.hook.tool.Tool.xpActivation

/**
 * LyricGetterExt - statusbar.finder.app
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/18 23:10
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        xpActivation = DSP.init(this, BuildConfig.APPLICATION_ID, MODE.HOOK, false)
        Log.i("DSP", "xpActivation $xpActivation")
    }
}
