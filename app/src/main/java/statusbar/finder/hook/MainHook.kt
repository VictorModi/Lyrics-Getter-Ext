package statusbar.finder.hook


import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.callbacks.XC_LoadPackage
import statusbar.finder.BuildConfig
import statusbar.finder.hook.app.SystemUI


/**
 * LyricGetterExt - statusbar.finder.hook
 * @description Coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2024/2/23 0:17
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelper.initHandleLoadPackage(lpparam)
        when (lpparam.packageName) {
            "com.android.systemui" -> initHooks(SystemUI)
            else -> return
        }
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
                Log.i("Init hook ${it.javaClass.name} completed")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i("Init hook ${it.javaClass.name} failed")
            }
        }
    }
}
