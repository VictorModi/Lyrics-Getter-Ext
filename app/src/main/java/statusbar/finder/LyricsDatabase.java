package statusbar.finder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadata;
import android.util.Log;
import androidx.annotation.Nullable;
import statusbar.finder.provider.ILrcProvider;

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
        db.beginTransaction();
        if (lyricResult == null) {
            try {
                db.execSQL(query, new Object[]{originMediaInfo.getTitle(), originMediaInfo.getArtist(), originMediaInfo.getAlbum(),
                        originMediaInfo.getDuration(), packageName, originMediaInfo.getDuration(), null, null, null, null, null, null, 0});
                db.setTransactionSuccessful();
                return true;
            } catch (Exception e) {
                e.fillInStackTrace();
                return false;
            } finally {
                db.endTransaction();
            }
        }
        try {
            db.execSQL(query, new Object[]{originMediaInfo.getTitle(), originMediaInfo.getArtist(), originMediaInfo.getAlbum(),
                    originMediaInfo.getDuration(), packageName, originMediaInfo.getDuration(), lyricResult.resultInfo.getTitle()
                    , lyricResult.resultInfo.getArtist(), lyricResult.resultInfo.getAlbum(), lyricResult.mLyric, lyricResult.mTranslatedLyric,
                    lyricResult.mSource, 0});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.fillInStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    @SuppressLint("Range")
    public ILrcProvider.LyricResult searchLyricFromDatabase(ILrcProvider.MediaInfo mediaInfo) {
        @SuppressLint("Recycle") Cursor cursor;
        if (mediaInfo.getTitle() == null || mediaInfo.getArtist() == null) {
            return null;
        }

        ILrcProvider.LyricResult result = new ILrcProvider.LyricResult();
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("searchLyricFromDatabase: ", String.format("SearchInfo : %s - %s - %s - %d", mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum(), mediaInfo.getDuration()));
        if (mediaInfo.getAlbum() != null) {
            String query = "SELECT lyric, translated_lyric, lyric_source, distance, _offset FROM Lyrics WHERE origin_title = ? AND origin_artist = ? AND origin_album = ? AND duration = ?";
            cursor = db.rawQuery(query, new String[]{mediaInfo.getTitle(), mediaInfo.getArtist(), mediaInfo.getAlbum(), String.valueOf(mediaInfo.getDuration())});
        } else {
            String query = "SELECT lyric, translated_lyric, lyric_source, distance, _offset FROM Lyrics WHERE origin_title = ? AND origin_artist = ? AND origin_duration = ?";
            cursor = db.rawQuery(query, new String[]{mediaInfo.getTitle(), mediaInfo.getArtist(), String.valueOf(mediaInfo.getDuration())});
        }


        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.mLyric = cursor.getString(cursor.getColumnIndex("lyric"));
                result.mTranslatedLyric = cursor.getString(cursor.getColumnIndex("translated_lyric"));
                result.mSource = cursor.getString(cursor.getColumnIndex("lyric_source"));
                result.mDistance = cursor.getLong(cursor.getColumnIndex("distance"));
                result.mOffset = (int) cursor.getLong(cursor.getColumnIndex("_offset"));

                cursor.close();
                return result;
            }
            cursor.close();
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
