package statusbar.finder

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * LyricGetterExt - statusbar
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 13:42
 */
object DatabaseHelper {
    private const val DATABASE_NAME = "lyric.db"
    var database: LyricDatabase? = null
        private set

    @Synchronized
    fun init(context: Context?) {
        if (database != null || context == null) {
            return
        }
        val driver = AndroidSqliteDriver(LyricDatabase.Schema, context, DATABASE_NAME)
        database = LyricDatabase(driver)
    }
}
