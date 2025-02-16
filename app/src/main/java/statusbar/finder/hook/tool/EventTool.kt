package statusbar.finder.hook.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import cn.lyric.getter.api.data.ExtraData
import cn.lyric.getter.api.data.LyricData
import cn.lyric.getter.api.data.type.OperateType
import cn.xiaowine.xkt.Tool.isNull
import cn.xiaowine.xkt.Tool.observableChange
import com.github.kyuubiran.ezxhelper.Log
import statusbar.finder.BuildConfig

/**
 * LyricGetterExt - statusbar.finder.hook.tool
 * @description 参考自 https://github.com/xiaowine/Lyric-Getter/blob/master/app/src/main/kotlin/cn/lyric/getter/tool/EventTools.kt
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/17 00:09
 */

@SuppressLint("StaticFieldLeak")
object EventTool {
    private lateinit var lyricsGetterContext: Context
    private var lastLyricData: LyricData? by observableChange(null) { _, _, newValue ->
        newValue?.run {
            if (lyric.isBlank()) {
                cleanLyric()
            } else {
                lyricsGetterContext.sendBroadcast(Intent().apply {
                    action = "Lyric_Data"
                    putExtra("Data", newValue)
                })
                Log.d(this.toString())
            }
        }
    }

    fun sendLyric(lyric: String, extra: ExtraData? = null) {
        val refinedLyric = lyric.trim()
        if (refinedLyric.isBlank()) return
        lastLyricData = LyricData().apply {
            this.type = OperateType.UPDATE
            this.lyric = refinedLyric
            if (extra.isNull()) {
                this.extraData.mergeExtra(ExtraData().apply {
                    this.packageName = BuildConfig.APPLICATION_ID
                    this.customIcon = false
                    this.base64Icon = ""
                    this.useOwnMusicController = false
                    this.delay = 0
                })
            }
        }
    }

    fun cleanLyric() {
        lyricsGetterContext.sendBroadcast(Intent().apply {
            action = "Lyric_Data"
            val lyricData = LyricData().apply {
                this.type = OperateType.STOP
                this.extraData.mergeExtra(ExtraData().apply {
                    this.packageName = BuildConfig.APPLICATION_ID
                })
            }
            putExtra("Data", lyricData)
        })
    }

    fun setContext(context: Context) {
        lyricsGetterContext = context
    }
}
