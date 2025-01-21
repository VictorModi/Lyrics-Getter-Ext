package statusbar.finder.hook

import cn.lyric.getter.api.BuildConfig
import cn.xiaowine.xkt.LogTool
import cn.xiaowine.xkt.LogTool.log
import com.github.kyuubiran.ezxhelper.EzXHelper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LyricGetterExt - statusbar.finder.hook
 * @description Coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2024/2/23 0:17
 */
abstract class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        LogTool.init("Lyrics Getter Ext", { BuildConfig.DEBUG }, BuildConfig.DEBUG)
        if (lpparam.packageName != "com.android.systemui") return
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    private fun initHooks(vararg hook: BaseHook) {
        hook.forEach {
            try {
                if (it.isInit) return@forEach
                it.init()
                it.isInit = true
                "Inited hook: ${it.javaClass.name}".log()
            } catch (e: Exception) {
                e.printStackTrace()
                "Init hook ${it.javaClass.name} failed".log()
            }
        }
    }
}
