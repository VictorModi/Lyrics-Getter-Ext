package statusbar.finder.data.model

import statusbar.finder.Res

/**
 * LyricGetterExt - statusbar.finder.data
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 14:00
 */
data class LyricResult(
    var lyric: String? = null,
    var translatedLyric: String? = null,
    var distance: Long = 0,
    var source: String = "Local",
    var offset: Int = 0,
    var resultInfo: MediaInfo? = null,
    var dataOrigin: DataOrigin = DataOrigin.UNDEFINED
) {
    override fun toString(): String {
        return """
            Distance: $distance
            Source: $source
            Offset: $offset
            Lyric: $lyric
            TranslatedLyric: $translatedLyric
            ResultInfo: $resultInfo
        """.trimIndent()
    }

    constructor(result: Res) : this(
        lyric = result.lyric,
        translatedLyric = result.translated_lyric,
        distance = result.distance ?: -1,
        offset = result.lyric_offset.toInt(),
        source = result.provider,
        dataOrigin = DataOrigin.DATABASE,
        resultInfo = MediaInfo(
            title = result.title ?: "",
            artist = result.artist ?: "",
            album = result.album ?: "",
            distance = result.distance ?: -1,
            duration = -1
        )
    )
}

