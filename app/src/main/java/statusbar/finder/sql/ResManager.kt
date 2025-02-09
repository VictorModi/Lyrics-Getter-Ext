package statusbar.finder.sql

import android.database.sqlite.SQLiteConstraintException
import statusbar.finder.DatabaseHelper
import statusbar.finder.Res
import statusbar.finder.ResQueries
import statusbar.finder.data.LyricResult
import statusbar.finder.provider.ILrcProvider

/**
 * LyricGetterExt - statusbar.finder.sql
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 13:18
 */
object ResManager {
    private val queries: ResQueries by lazy {
        DatabaseHelper.database?.resQueries
            ?: throw IllegalStateException("Database not initialized")
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

    fun getResByOriginIdAndProvider(originId: Long, provider: String): Res? {
        return queries.getResByIdAndProvider(originId, provider).executeAsOneOrNull()
    }

    fun updateResOffsetById(id: Long, offset: Long) {
        return queries.updateResOffset(offset, id)
    }
}
