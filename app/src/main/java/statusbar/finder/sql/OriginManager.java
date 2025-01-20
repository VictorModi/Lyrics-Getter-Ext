package statusbar.finder.sql;

import LyricGetterExt.Database;
import statusbar.finder.DatabaseHelper;
import statusbar.finder.Origin;
import statusbar.finder.OriginQueries;
import statusbar.finder.provider.ILrcProvider;

/**
 * LyricGetterExt - statusbar.finder.sql
 *
 * @author VictorModi
 * @description TODO: coming soon.
 * @email victormodi@outlook.com
 * @date 2025/1/20 下午5:15
 */
public class OriginManager {
    private static OriginQueries queries = null;

    public static Long insertOrGetMediaInfoId(ILrcProvider.MediaInfo mediaInfo, String packageName) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getOriginQueries();}
        Long result = getMediaInfoId(mediaInfo, packageName);
        if (result != null) {return result;}
        queries.insertMediaInfo(
                mediaInfo.getTitle(),
                mediaInfo.getArtist(),
                mediaInfo.getAlbum(),
                mediaInfo.getDuration(),
                packageName
        );
        result = queries.getLastInsertId().executeAsOne();
        return result;
    }

    private static Long getMediaInfoId(ILrcProvider.MediaInfo mediaInfo, String packageName) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getOriginQueries();}
        return queries.getMediaInfoId(
                mediaInfo.getTitle(),
                mediaInfo.getArtist(),
                mediaInfo.getAlbum(),
                packageName
        ).executeAsOneOrNull();
    }

    public static ILrcProvider.MediaInfo getMediaInfoById(Long id) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getOriginQueries();}
        Origin data = queries.getInfoById(id).executeAsOneOrNull();
        if (data == null) {return null;}
        return new ILrcProvider.MediaInfo(data);
    }
}
