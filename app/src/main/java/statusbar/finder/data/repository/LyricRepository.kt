package statusbar.finder.data.repository

import cn.zhaiyifan.lyric.model.Lyric
import statusbar.finder.data.model.LyricResult
import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.data.repository
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/16 23:01
 */
object LyricRepository {
    /**
     * 根据 MediaInfo 获取激活的歌词 & OriginId
     */
    fun getActiveLyricFromDatabase(mediaInfo: MediaInfo, packageName: String): Pair<LyricResult?, Long> {
        val originId = OriginRepository.insertOrGetMediaInfoId(mediaInfo, packageName)
        val lyricResult = getActiveLyricFromDatabaseByOriginId(originId)
        return Pair(lyricResult, originId)
    }

    /**
     * 根据 OriginId 获取激活的歌词
     */
    fun getActiveLyricFromDatabaseByOriginId(originId: Long): LyricResult? {
        val resultId = ActiveRepository.getResultIdByOriginId(originId) ?: return null
        val res = ResRepository.getResById(resultId) ?: return null
        return LyricResult(res)
    }


    fun deleteResByOriginIdAndDeleteActive(originId: Long) {
        ActiveRepository.deleteResultByOriginId(originId)
        ResRepository.deleteResByOriginId(originId)
    }
}
