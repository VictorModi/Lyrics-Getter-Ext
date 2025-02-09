package statusbar.finder.sql

import android.database.sqlite.SQLiteConstraintException
import statusbar.finder.DatabaseHelper
import statusbar.finder.OriginQueries
import statusbar.finder.data.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.sql
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 13:52
 */
object OriginManager {
    private val queries: OriginQueries by lazy {
        DatabaseHelper.database?.originQueries
            ?: throw IllegalStateException("Database not initialized")
    }

    @JvmStatic
    fun insertOrGetMediaInfoId(mediaInfo: MediaInfo, packageName: String): Long {
        getOriginId(mediaInfo, packageName)?.let { return it }
        return try {
            queries.insertMediaInfo(
                mediaInfo.title,
                mediaInfo.artist,
                mediaInfo.album,
                mediaInfo.duration,
                packageName
            )
            queries.getLastInsertId().executeAsOne()
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
            -1L
        }
    }

    fun getOriginId(mediaInfo: MediaInfo, packageName: String): Long? {
        return queries.getMediaInfoId(
            mediaInfo.title,
            mediaInfo.artist,
            mediaInfo.album,
            packageName
        ).executeAsOneOrNull()
    }

    @JvmStatic
    fun getMediaInfoById(id: Long): MediaInfo? {
        return queries.getInfoById(id).executeAsOneOrNull()?.let { MediaInfo(it) }
    }
}
