package statusbar.finder.hook.tool

import android.app.Application
import cn.xiaowine.xkt.Tool.isNotNull
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder

/**
 * LyricGetterExt - statusbar.finder.hook.tool
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/15 14:48
 */
object HookTool {
    fun getApplication(callback: (Application) -> Unit) {
        var isLoad = false
        Application::class.java.methodFinder().filterByName("attach").first().createHook {
            after {
                if (isLoad) return@after
                isLoad = true
                callback(it.thisObject as Application)
            }
        }
    }

    fun isTargetClass(targetClassName: String, classLoader: ClassLoader? = null, callback: (Class<*>) -> Unit): Boolean {
        loadClassOrNull(targetClassName, classLoader).isNotNull {
            callback(it)
            return true
        }
        return false
    }
}
