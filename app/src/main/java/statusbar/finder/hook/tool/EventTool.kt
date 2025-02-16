package statusbar.finder.hook.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import java.lang.ref.WeakReference
import cn.lyric.getter.api.data.ExtraData
import cn.lyric.getter.api.data.LyricData
import cn.lyric.getter.api.data.type.OperateType
import cn.xiaowine.xkt.Tool.isNull
import cn.xiaowine.xkt.Tool.observableChange
import com.github.kyuubiran.ezxhelper.Log

/**
 * LyricGetterExt - statusbar.finder.hook.tool
 * @description 抄袭自 https://github.com/xiaowine/Lyric-Getter/blob/master/app/src/main/kotlin/cn/lyric/getter/tool/EventTools.kt
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/17 00:09
 */

@SuppressLint("StaticFieldLeak")
object EventTool {
    private var contextRef: WeakReference<Context>? = null

    private var lastLyricData: LyricData? by observableChange(null) { _, _, newValue ->
        newValue?.let {
            val context = contextRef?.get() ?: return@let
            if (it.lyric.isBlank()) {
                cleanLyric(it.extraData.packageName)
            } else {
                context.sendBroadcast(Intent().apply {
                    action = "Lyric_Data"
                    putExtra("Data", it)
                })
                Log.d(it.toString())
            }
        }
    }

    fun sendLyric(lyric: String, extra: ExtraData? = null) {
        val refinedLyric = lyric.trim()
        if (refinedLyric.isBlank()) return

        lastLyricData = LyricData().apply {
            this.type = OperateType.UPDATE
            this.lyric = refinedLyric
            this.extraData = extra ?: ExtraData().apply {
                packageName = "statusbar.finder"
                customIcon = false
                base64Icon = ""
                useOwnMusicController = false
                delay = 0
            }
        }
    }

    fun cleanLyric(caller: String = "") {
        val context = contextRef?.get() ?: return
        context.sendBroadcast(Intent().apply {
            action = "Lyric_Data"
            val lyricData = LyricData().apply {
                this.type = OperateType.STOP
                this.extraData.packageName = caller.ifEmpty {
                    context.packageName.takeIf { it != "com.android.systemui" } ?: ""
                }
            }
            putExtra("Data", lyricData)
        })
    }

    fun setContext(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }
}

