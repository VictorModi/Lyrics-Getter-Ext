package statusbar.finder.modifier

import com.moji4j.MojiConverter
import statusbar.finder.Modifier
import statusbar.finder.data.model.MediaInfo
import statusbar.finder.utils.CheckLanguageUtil.isLatin
import statusbar.finder.utils.LyricSearchUtil.getSearchKey

/**
 * LyricGetterExt - statusbar.finder.modifiers
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:42
 */
class KatakanaModifier : Modifier() {
    override fun modify(mediaInfo: MediaInfo): MediaInfo? {
        if (isNoJapaneseButLatin(mediaInfo)) return null
        val converter = MojiConverter()
        val convertedMediaInfo = mediaInfo.copy().apply {
            title = converter.convertRomajiToKatakana(title)
            artist = converter.convertRomajiToKatakana(artist)
            album = converter.convertRomajiToKatakana(album)
        }
        if (isLatin(getSearchKey(mediaInfo))) return null
        return convertedMediaInfo
    }
}
