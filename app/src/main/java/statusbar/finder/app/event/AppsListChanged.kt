package statusbar.finder.app.event

import androidx.lifecycle.LiveData

/**
 * LyricGetterExt - statusbar.finder.livedata
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/8 19:55
 */
class AppsListChanged private constructor() : LiveData<Void?>() {

    companion object {
        @Volatile
        private var instance: AppsListChanged? = null

        fun getInstance(): AppsListChanged {
            return instance ?: synchronized(this) {
                instance ?: AppsListChanged().also { instance = it }
            }
        }
    }

    fun notifyChange() {
        postValue(null)
    }
}
