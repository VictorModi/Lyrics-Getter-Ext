package statusbar.finder;

import LyricGetterExt.Database;
import android.content.Context;
import app.cash.sqldelight.db.SqlDriver;
import app.cash.sqldelight.driver.android.AndroidSqliteDriver;
import lombok.Getter;

public class DatabaseHelper {
    private static final String DATABASE_NAME = "lyric.db";
    @Getter
    private static Database database;

    public static synchronized void init(Context context) {
        if (database != null) {return;}
        SqlDriver driver = new AndroidSqliteDriver(Database.Companion.getSchema(), context, DATABASE_NAME);
        database = Database.Companion.invoke(driver);
    }
}
