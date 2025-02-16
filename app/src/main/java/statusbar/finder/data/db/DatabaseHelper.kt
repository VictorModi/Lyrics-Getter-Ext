package statusbar.finder.data.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import statusbar.finder.LyricDatabase

/**
 * LyricGetterExt - statusbar
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 13:42
 */
object DatabaseHelper {
    private const val DATABASE_NAME = "lyric.db"
    private lateinit var database: LyricDatabase

    @Synchronized
    fun init(context: Context) {
        if (DatabaseHelper::database.isInitialized) return
        val driver = AndroidSqliteDriver(LyricDatabase.Schema, context, DATABASE_NAME)
        database = LyricDatabase(driver)
    }

    fun getDatabase(): LyricDatabase {
        if (!DatabaseHelper::database.isInitialized) {
            throw IllegalStateException("DatabaseHelper.init() must be called before using the database.")
        }
        return database
    }
}
