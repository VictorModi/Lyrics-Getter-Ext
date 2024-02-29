package statusbar.finder.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * LyricGetterExt - statusbar.finder.hook
 * @description: Coming soon.
 * @author: VictorModi
 * @email: victormodi@outlook.com
 * @date: 2024/2/23 0:17
 */
abstract class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        
    }
}
