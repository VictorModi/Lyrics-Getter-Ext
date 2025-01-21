package statusbar.finder;

import android.content.Context;
import app.cash.sqldelight.db.SqlDriver;
import app.cash.sqldelight.driver.android.AndroidSqliteDriver;
import lombok.Getter;

public class DatabaseHelper {
    private static final String DATABASE_NAME = "lyric.db";
    @Getter
    private static LyricDatabase database;

    public static synchronized void init(Context context) {
        if (database != null) {return;}
        SqlDriver driver = new AndroidSqliteDriver(LyricDatabase.Companion.getSchema(), context, DATABASE_NAME);
        database = LyricDatabase.Companion.invoke(driver);
    }
}
