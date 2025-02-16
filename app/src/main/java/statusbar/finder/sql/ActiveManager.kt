package statusbar.finder.sql

import statusbar.finder.ActiveQueries
import statusbar.finder.DatabaseHelper

/**
 * LyricGetterExt - statusbar.finder.sql
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 13:53
 */
object ActiveManager {
    private val queries: ActiveQueries by lazy {
        DatabaseHelper.database?.activeQueries
            ?: throw IllegalStateException("Database not initialized")
    }

    fun insertActiveLog(originId: Long, resultId: Long) {
        queries.insertActive(originId, resultId)
    }

    fun updateActiveLog(originId: Long, resultId: Long) {
        queries.updateActive(originId, resultId)
    }

    fun getResultIdByOriginId(originId: Long): Long? {
        return queries.getResultId(originId).executeAsOneOrNull()
    }
}

