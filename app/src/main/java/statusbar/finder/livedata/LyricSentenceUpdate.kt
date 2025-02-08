package statusbar.finder.livedata

import androidx.lifecycle.LiveData

/**
 * LyricGetterExt - statusbar.finder.livedata
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 19:56
 */

class LyricSentenceUpdate private constructor() : LiveData<LyricSentenceUpdate.Data>() {

    companion object {
        @Volatile
        private var instance: LyricSentenceUpdate? = null

        fun getInstance(): LyricSentenceUpdate {
            return instance ?: synchronized(this) {
                instance ?: LyricSentenceUpdate().also { instance = it }
            }
        }
    }

    fun notifyLyrics(data: Data) {
        postValue(data)
    }

    data class Data(
        val lyric: String,
        val translatedLyric: String?,
        val lyricsIndex: Int,
        val delay: Int
    )
}
