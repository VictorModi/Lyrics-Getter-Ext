package statusbar.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import statusbar.finder.provider.ILrcProvider;

import java.util.Arrays;

public class LyricsDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Lyrics.db";
    private static final int DATABASE_VERSION = 2;
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS Lyrics (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "origin_title TEXT NOT NULL," +
            "origin_artist TEXT," +
            "origin_album TEXT," +
            "origin_package_name TEXT," +
            "duration BIGINT," +
            "distance BIGINT," +
            "result_title TEXT," +
            "result_artist TEXT," +
            "result_album TEXT," +
            "lyric TEXT," +
            "translated_lyric TEXT," +
            "lyric_source TEXT," +
            "added_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "_offset INTEGER)";


    public LyricsDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    public boolean insertLyricIntoDatabase(ILrcProvider.LyricResult lyricResult, ILrcProvider.MediaInfo originMediaInfo, String packageName) {
        if (originMediaInfo.getTitle() == null) {
            return false;
        }

        String query = "INSERT INTO Lyrics (" +
                "origin_title, origin_artist, origin_album, origin_package_name, " +
                "duration, distance, result_title, result_artist, result_album, " +
                "lyric, translated_lyric, lyric_source, _offset) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteDatabase db = this.getWritableDatabase();

        if (searchLyricFromDatabase(originMediaInfo, packageName) != null) return true;

        try {
            db.beginTransaction();

            Object[] params = getParams(lyricResult, originMediaInfo, packageName);
            db.execSQL(query, params);
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e("DatabaseError", "Insert failed", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    private static Object[] getParams(ILrcProvider.LyricResult lyricResult, ILrcProvider.MediaInfo originMediaInfo, String packageName) {
        Object[] params;
        if (lyricResult == null) {
            params = new Object[]{originMediaInfo.getTitle(), originMediaInfo.getArtist(), originMediaInfo.getAlbum(),
                    packageName, originMediaInfo.getDuration(), -1, null, null, null, null, null, null, 0};
        } else {
            params = new Object[]{originMediaInfo.getTitle(), originMediaInfo.getArtist(), originMediaInfo.getAlbum(),
                    packageName, originMediaInfo.getDuration(), lyricResult.mDistance, lyricResult.mResultInfo.getTitle(),
                    lyricResult.mResultInfo.getArtist(), lyricResult.mResultInfo.getAlbum(), lyricResult.mLyric,
                    lyricResult.mTranslatedLyric, lyricResult.mSource, 0};
        }
        return params;
    }

    @SuppressLint("Range")
    public ILrcProvider.LyricResult searchLyricFromDatabase(ILrcProvider.MediaInfo mediaInfo, String packageName) {
        if (mediaInfo.getTitle() == null || mediaInfo.getArtist() == null) {
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT lyric, translated_lyric, lyric_source, distance, duration, _offset, result_title, result_artist, result_album " +
                    "FROM Lyrics WHERE origin_title = ? AND origin_artist = ? AND origin_package_name = ?";

            String[] args = {mediaInfo.getTitle(), mediaInfo.getArtist(), packageName};
            if (mediaInfo.getAlbum() != null) {
                query += " AND origin_album = ?";
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = mediaInfo.getAlbum();
            }

            cursor = db.rawQuery(query, args);

            if (cursor.moveToFirst()) {
                ILrcProvider.LyricResult result = new ILrcProvider.LyricResult();
                result.mLyric = cursor.getString(cursor.getColumnIndex("lyric"));
                result.mTranslatedLyric = cursor.getString(cursor.getColumnIndex("translated_lyric"));
                result.mSource = cursor.getString(cursor.getColumnIndex("lyric_source"));
                result.mDistance = cursor.getLong(cursor.getColumnIndex("distance"));
                result.mOffset = (int) cursor.getLong(cursor.getColumnIndex("_offset"));

                result.mResultInfo = new ILrcProvider.MediaInfo(
                        cursor.getString(cursor.getColumnIndex("result_title")),
                        cursor.getString(cursor.getColumnIndex("result_artist")),
                        cursor.getString(cursor.getColumnIndex("result_album")),
                        cursor.getLong(cursor.getColumnIndex("duration")),
                        cursor.getLong(cursor.getColumnIndex("distance"))
                );

                result.mOrigin = ILrcProvider.Origin.DATABASE;
                return result;
            }
        } catch (Exception e) {
            Log.e("DatabaseError", "Search failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            String copyDataSQL ="INSERT INTO " +
                    "LyricsTemp " +
                    "(origin_title, origin_artist, origin_album, duration, distance, lyric, translated_lyric, lyric_source, added_date, _offset) " +
                    "SELECT " +
                    "song, artist, album, duration, distance, lyric, translated_lyric, lyric_source, added_date, _offset " +
                    "FROM Lyrics";
            String dropOldTableSQL = "DROP TABLE IF EXISTS Lyrics";
            String renameNewTableSQL = "ALTER TABLE LyricsTemp RENAME TO Lyrics";
            db.beginTransaction();
            try {
                db.execSQL(CREATE_TABLE);
                db.execSQL(copyDataSQL);
                db.execSQL(dropOldTableSQL);
                db.execSQL(renameNewTableSQL);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
