package statusbar.finder

import statusbar.finder.data.model.MediaInfo
import statusbar.finder.utils.CheckLanguageUtil.isJapanese
import statusbar.finder.utils.CheckLanguageUtil.isLatin
import statusbar.finder.utils.LyricSearchUtil.getSearchKey

/**
 * LyricGetterExt - statusbar.finder
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:38
 */
abstract class QueryProcessor {
    abstract fun modify(mediaInfo: MediaInfo): MediaInfo?

    fun isNoJapaneseButLatin(mediaInfo: MediaInfo): Boolean {
        val searchKey = getSearchKey(mediaInfo)
        return !isJapanese(searchKey) && isLatin(searchKey)
    }
}
