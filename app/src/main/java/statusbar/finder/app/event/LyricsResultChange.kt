package statusbar.finder.app.event

/**
 * LyricGetterExt - statusbar.finder.livedata
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 19:57
 */
import androidx.lifecycle.LiveData
import statusbar.finder.data.model.LyricResult
import statusbar.finder.data.model.MediaInfo

class LyricsResultChange private constructor() : LiveData<LyricsResultChange.Data>() {

    companion object {
        @Volatile
        private var instance: LyricsResultChange? = null

        fun getInstance(): LyricsResultChange {
            return instance ?: synchronized(this) {
                instance ?: LyricsResultChange().also { instance = it }
            }
        }
    }

    fun notifyResult(data: Data) {
        postValue(data)
    }

    data class Data(
        val originInfo: MediaInfo,
        val result: LyricResult?
    )
}

