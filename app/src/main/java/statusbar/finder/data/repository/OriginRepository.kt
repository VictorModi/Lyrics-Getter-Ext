package statusbar.finder.data.repository

import android.database.sqlite.SQLiteConstraintException
import statusbar.finder.OriginQueries
import statusbar.finder.data.db.DatabaseHelper
import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.data.repository
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/16 23:08
 */
object OriginRepository {
    private val queries: OriginQueries by lazy {
        DatabaseHelper.getDatabase().originQueries
    }

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
}

