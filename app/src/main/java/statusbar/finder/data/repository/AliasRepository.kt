package statusbar.finder.data.repository

import statusbar.finder.ActiveQueries
import statusbar.finder.Alias
import statusbar.finder.AliasQueries
import statusbar.finder.data.db.DatabaseHelper

/**
 * LyricGetterExt - statusbar.finder.data.repository
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/29 17:17
 */
object AliasRepository {
    private val queries: AliasQueries by lazy {
        DatabaseHelper.getDatabase().aliasQueries
    }

    fun getAlias(originId: Long): Alias? {
        return queries.getAlias(originId).executeAsOneOrNull()
    }

    fun updateAlias(originId: Long, title: String?, artist: String?, album: String?) {
        getAlias(originId)?.let {
            queries.updateAlias(
                originId,
                title ?: it.title,
                artist ?: it.artist,
                album ?: it.album
            )
        } ?: run {
            queries.updateAlias(originId, title, artist, album)
        }
    }
}
