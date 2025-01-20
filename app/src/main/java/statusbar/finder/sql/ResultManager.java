package statusbar.finder.sql;

import statusbar.finder.DatabaseHelper;
import statusbar.finder.Result;
import statusbar.finder.ResultQueries;
import statusbar.finder.provider.ILrcProvider;

import java.util.List;

/**
 * LyricGetterExt - statusbar.finder.sql
 *
 * @author VictorModi
 * @description TODO: coming soon.
 * @email victormodi@outlook.com
 * @date 2025/1/20 下午5:16
 */
public class ResultManager {
    private static ResultQueries queries = null;

    public static Long insertResultData(Long originId, ILrcProvider.LyricResult lyricResult) {
        if (queries == null) { queries = DatabaseHelper.getDatabase().getResultQueries(); }
        assert lyricResult.mResultInfo != null;
        queries.insertResult(
                originId,
                lyricResult.mSource,
                lyricResult.mLyric,
                lyricResult.mTranslatedLyric,
                lyricResult.mDistance,
                lyricResult.mResultInfo.getTitle(),
                lyricResult.mResultInfo.getArtist(),
                lyricResult.mResultInfo.getAlbum()
        );
        return queries.getLastInsertId().executeAsOne();
    }

    public static List<Result> getResultDatByOriginId(Long originId) {
        if (queries == null) { queries = DatabaseHelper.getDatabase().getResultQueries(); }
        return queries.getResultByOriginId(originId).executeAsList();
    }

    public static Result getResultDataById(Long id) {
        if (queries == null) { queries = DatabaseHelper.getDatabase().getResultQueries(); }
        return queries.getResultById(id).executeAsOneOrNull();
    }

    public static Result getResultByOriginIdAndProvider(Long originId, String provider) {
        if (queries == null) { queries = DatabaseHelper.getDatabase().getResultQueries(); }
        return queries.getResultByIdAndProvider(originId, provider).executeAsOneOrNull();
    }
}
