package statusbar.finder.hook.tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.os.UserHandle
import cn.lyric.getter.api.data.ExtraData
import cn.lyric.getter.api.data.LyricData
import cn.lyric.getter.api.data.type.OperateType
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

@SuppressLint("StaticFieldLeak", "MissingPermission")
object EventTool {
    private lateinit var context: Context
    private lateinit var user: UserHandle
    private var lastLyricData: LyricData? by observableChange(null) { _, _, newValue ->
        newValue?.run {
            if (lyric.isBlank()) {
                cleanLyric()
            } else {
                context.sendBroadcastAsUser(Intent().apply {
                    action = "Lyric_Data"
                    putExtra("Data", newValue)
                }, UserHandle.getUserHandleForUid(android.os.Process.myUid()))
                Log.d(this.toString())
            }
        }
    }

    fun sendLyric(lyric: String, extra: ExtraData) {
        val refinedLyric = lyric.trim()
        if (refinedLyric.isBlank()) return
        lastLyricData = LyricData().apply {
            this.type = OperateType.UPDATE
            this.lyric = refinedLyric
            this.extraData = extra
        }
    }

    fun sendMetadata(metadata: MediaMetadata) {
        context.sendBroadcastAsUser(Intent().apply {
            action = "Lyric_Data"
            val lyricData = LyricData().apply {
                this.type = OperateType.MEDIA_DATA
                this.extraData.mergeExtra(ExtraData().apply {
                    this.packageName = BuildConfig.APPLICATION_ID
                    this.mediaMetadata = metadata
                    this.artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    this.album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown Album"
                    this.title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title"
                })
            }
            putExtra("Data", lyricData)
        }, UserHandle.getUserHandleForUid(android.os.Process.myUid()))
    }

    fun cleanLyric() {
        context.sendBroadcastAsUser(Intent().apply {
            action = "Lyric_Data"
            val lyricData = LyricData().apply {
                this.type = OperateType.STOP
                this.extraData.mergeExtra(ExtraData().apply {
                    this.packageName = BuildConfig.APPLICATION_ID
                })
            }
            putExtra("Data", lyricData)
        }, UserHandle.getUserHandleForUid(android.os.Process.myUid()))
    }

    fun setContext(context: Context, user: UserHandle) {
        this.context = context
        this.user = user
        Log.i("${BuildConfig.APPLICATION_ID} Initializing EventTool, Context by ${this.context.packageName}")
    }
}
