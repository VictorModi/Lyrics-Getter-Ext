package statusbar.finder.data.repository

import statusbar.finder.ActiveQueries
import statusbar.finder.data.db.DatabaseHelper

/**
 * LyricGetterExt - statusbar.finder.data.repository
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/16 23:07
 */
object ActiveRepository {
    private val queries: ActiveQueries by lazy {
        DatabaseHelper.getDatabase().activeQueries
    }

    fun insertActiveLog(originId: Long, resultId: Long) {
        queries.insertActive(originId, resultId)
    }

    fun getResultIdByOriginId(originId: Long): Long? {
        return queries.getResultId(originId).executeAsOneOrNull()
    }

    fun updateResultIdByOriginId(originId: Long, resultId: Long) {
        queries.updateActive(resultId, originId)
    }
}
