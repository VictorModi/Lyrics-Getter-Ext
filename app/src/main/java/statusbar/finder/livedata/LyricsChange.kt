package statusbar.finder.livedata

import androidx.lifecycle.LiveData
import cn.zhaiyifan.lyric.model.Lyric
import statusbar.finder.provider.ILrcProvider

/**
 * LyricGetterExt - statusbar.finder.livedata
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 20:13
 */
class LyricsChange private constructor() : LiveData<LyricsChange.Data>() {

    companion object {
        @Volatile
        private var instance: LyricsChange? = null

        fun getInstance(): LyricsChange {
            return instance ?: synchronized(this) {
                instance ?: LyricsChange().also { instance = it }
            }
        }
    }

    fun notifyResult(data: Data) {
        postValue(data)
    }

    data class Data(
        val lyric: Lyric,
    )
}
