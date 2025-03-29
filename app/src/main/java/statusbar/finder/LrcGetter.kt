package statusbar.finder

import android.content.Context
import android.media.MediaMetadata
import cn.zhaiyifan.lyric.LyricUtils
import cn.zhaiyifan.lyric.model.Lyric
import com.github.houbb.opencc4j.util.ZhConverterUtil
import statusbar.finder.app.event.LyricsResultChange
import statusbar.finder.app.event.LyricsResultChange.Companion.getInstance
import statusbar.finder.data.db.DatabaseHelper.init
import statusbar.finder.data.model.DataOrigin
import statusbar.finder.data.model.LyricResult
import statusbar.finder.data.model.MediaInfo
import statusbar.finder.data.repository.ActiveRepository.insertActiveLog
import statusbar.finder.data.repository.LyricRepository.getActiveLyricFromDatabase
import statusbar.finder.data.repository.LyricRepository.getActiveLyricFromDatabaseByOriginId
import statusbar.finder.data.repository.ResRepository.getResByOriginIdAndProvider
import statusbar.finder.data.repository.ResRepository.insertResData
import statusbar.finder.modifier.*
import statusbar.finder.provider.KugouProvider
import statusbar.finder.provider.MusixMatchProvider
import statusbar.finder.provider.NeteaseProvider
import statusbar.finder.provider.QQMusicProvider
import statusbar.finder.utils.CheckLanguageUtil
import statusbar.finder.utils.LyricSearchUtil
import java.io.IOException

/**
 * LyricGetterExt - statusbar.finder
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:23
 */
object LrcGetter {
    private val providers = arrayOf(
        NeteaseProvider(),
        KugouProvider(),
        QQMusicProvider(),
        MusixMatchProvider()
    )

    private val modifiers = arrayOf(
        AliasModifier(),
        SimplifiedModifier(),
        RemoveParenthesesModifier(),
        OriginalModifier(),
        KatakanaModifier(),
        HiraganaModifier(),
    )

    fun getLyric(context: Context, mediaMetadata: MediaMetadata, sysLang: String, packageName: String): Lyric? {
        return getLyric(context, MediaInfo(mediaMetadata), sysLang, packageName)
    }

    private fun getLyric(context: Context, mediaInfo: MediaInfo, sysLang: String, packageName: String): Lyric? {
        init(context)
        val databaseResult = getActiveLyricFromDatabase(mediaInfo, packageName)
        var currentResult: LyricResult? = databaseResult.first
        currentResult?.let {
            getInstance().notifyResult(LyricsResultChange.Data(mediaInfo, it))
            return LyricUtils.parseLyric(it, mediaInfo, packageName)
        }
        for (modifier in modifiers) {
            modifier.modify(mediaInfo, databaseResult.second)?.let {
                searchLyricsResultByInfo(
                    it,
                    databaseResult.second,
                    sysLang,
                )
            }
            currentResult = getActiveLyricFromDatabaseByOriginId(databaseResult.second)
            currentResult?.let {
                it.dataOrigin = DataOrigin.INTERNET
                getInstance().notifyResult(LyricsResultChange.Data(mediaInfo, it))
                return LyricUtils.parseLyric(it, mediaInfo, packageName)
            }
        }
        return null
    }

    private fun searchLyricsResultByInfo(mediaInfo: MediaInfo, originId: Long, sysLang: String) {
        var bestMatchSource: String? = null
        val bestMatchDistance: Long = 0
        for (provider in providers) {
            try {
                val lyricResult = provider.getLyric(mediaInfo)
                if (lyricResult?.lyric != null) {
                    val allLyrics = if (lyricResult.translatedLyric != null) {
                        LyricUtils.getAllLyrics(false, lyricResult.translatedLyric)
                    } else {
                        LyricUtils.getAllLyrics(false, lyricResult.lyric)
                    }
                    if (!CheckLanguageUtil.isJapanese(allLyrics)) {
                        when (sysLang) {
                            "zh-CN" -> if (lyricResult.translatedLyric != null) {
                                lyricResult.translatedLyric = ZhConverterUtil.toSimple(lyricResult.translatedLyric)
                            } else {
                                lyricResult.lyric = ZhConverterUtil.toSimple(lyricResult.lyric)
                            }

                            "zh-TW" -> if (lyricResult.translatedLyric != null) {
                                lyricResult.translatedLyric =
                                    ZhConverterUtil.toTraditional(lyricResult.translatedLyric)
                            } else {
                                lyricResult.lyric = ZhConverterUtil.toTraditional(lyricResult.lyric)
                            }

                            else -> {}
                        }
                    }
                    insertResData(originId, lyricResult)
                    if (LyricSearchUtil.isLyricContent(lyricResult.lyric)
                        && (bestMatchSource == null || bestMatchDistance > lyricResult.distance)
                    ) {
                        bestMatchSource = lyricResult.source
                    }
                }
            } catch (e: IOException) {
                e.fillInStackTrace()
            }
        }
        if (bestMatchSource != null) {
            val bestMatchResult = checkNotNull(getResByOriginIdAndProvider(originId, bestMatchSource))
            insertActiveLog(originId, bestMatchResult.id)
        }
    }
}
