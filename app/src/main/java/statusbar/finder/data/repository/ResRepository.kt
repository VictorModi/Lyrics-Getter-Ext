package statusbar.finder.data.repository

import android.database.sqlite.SQLiteConstraintException
import statusbar.finder.Res
import statusbar.finder.ResQueries
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.data.model.LyricResult

/**
 * LyricGetterExt - statusbar.finder.data.repository
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/16 23:09
 */
object ResRepository {
    private val queries: ResQueries by lazy {
        DatabaseHelper.getDatabase().resQueries
    }

    fun insertResData(originId: Long, lyricResult: LyricResult) {
        val resultInfo = lyricResult.resultInfo
            ?: throw IllegalArgumentException("LyricResult.mResultInfo cannot be null")
        try {
            queries.insertRes(
                originId,
                lyricResult.source,
                lyricResult.lyric,
                lyricResult.translatedLyric,
                lyricResult.distance,
                resultInfo.title,
                resultInfo.artist,
                resultInfo.album
            )
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    fun getResByOriginId(originId: Long): List<Res> {
        return queries.getResByOriginId(originId).executeAsList()
    }

    fun getResById(resId: Long): Res? {
        return queries.getResById(resId).executeAsOneOrNull()
    }

    fun updateResOffsetById(id: Long, offset: Long) {
        return queries.updateResOffset(offset, id)
    }

    fun getResByOriginIdAndProvider(originId: Long, provider: String): Res? {
        return queries.getResByIdAndProvider(originId, provider).executeAsOneOrNull()
    }

    fun getProvidersMapByOriginId(originId: Long): Map<String, Long> {
        return queries.getProvidersAndIdByOriginId(originId).executeAsList().associate { it.provider to it.id }
    }

    fun deleteResByOriginId(originId: Long) {
        return queries.deleteResByOriginId(originId)
    }
}
