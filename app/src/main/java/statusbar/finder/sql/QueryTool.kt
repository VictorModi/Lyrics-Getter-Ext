package statusbar.finder.sql

import androidx.core.util.Pair
import statusbar.finder.data.LyricResult
import statusbar.finder.data.MediaInfo
import statusbar.finder.sql.ActiveManager.getResultIdByOriginId
import statusbar.finder.sql.OriginManager.insertOrGetMediaInfoId
import statusbar.finder.sql.ResManager.getResById

/**
 * LyricGetterExt - statusbar.finder.sql
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 17:40
 */
object QueryTool {
    fun getActiveLyricFromDatabase(mediaInfo: MediaInfo, packageName: String): Pair<LyricResult?, Long> {
        val originId = insertOrGetMediaInfoId(mediaInfo, packageName)
        val lyricResult = getActiveLyricFromDatabaseByOriginId(originId)
        return Pair(lyricResult, originId)
    }

    fun getActiveLyricFromDatabaseByOriginId(originId: Long): LyricResult? {
        val resultId = getResultIdByOriginId(originId) ?: return null
        val res = getResById(resultId) ?: return null
        return LyricResult(res)
    }
}
