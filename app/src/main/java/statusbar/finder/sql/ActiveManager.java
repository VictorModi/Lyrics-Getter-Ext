package statusbar.finder.sql;

import statusbar.finder.ActiveQueries;
import statusbar.finder.DatabaseHelper;
import statusbar.finder.OriginQueries;

/**
 * LyricGetterExt - statusbar.finder.sql
 *
 * @author VictorModi
 * @description TODO: coming soon.
 * @email victormodi@outlook.com
 * @date 2025/1/20 下午5:16
 */
public class ActiveManager {
    private static ActiveQueries queries = null;

    public static Long insertActiveLog(Long originId, Long resultId) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getActiveQueries();}
        queries.insertActive(originId, resultId);
        return queries.getLastInsertId().executeAsOne();
    }

    public static void updateActiveLog(Long originId, Long resultId) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getActiveQueries();}
        queries.updateActive(originId, resultId);
    }

    public static Long getResultIdByOriginId(Long originId) {
        if (queries == null) {queries = DatabaseHelper.getDatabase().getActiveQueries();}
        return queries.getResultId(originId).executeAsOneOrNull();
    }
}
