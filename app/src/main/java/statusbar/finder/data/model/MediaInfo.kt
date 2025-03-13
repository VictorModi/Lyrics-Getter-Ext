package statusbar.finder.data.model

import android.media.MediaMetadata
import com.github.houbb.opencc4j.util.ZhConverterUtil
import statusbar.finder.Origin

/**
 * LyricGetterExt - statusbar.finder.data
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 14:03
 */
data class MediaInfo(
    var title: String = "",
    var artist: String = "",
    var album: String = "",
    var duration: Long = -1,
    var distance: Long = -1
) {
    constructor(mediaMetadata: MediaMetadata) : this(
        title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
        artist = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
        album = mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "",
        duration = mediaMetadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
        distance = -1
    )

    constructor(originData: Origin) : this(
        title = originData.title,
        artist = originData.artist,
        album = originData.album ?: "",
        duration = originData.duration ?: -1,
        distance = -1
    )

    fun toSimple(): MediaInfo {
        return this.copy().apply {
            title = ZhConverterUtil.toSimple(title)
            artist = ZhConverterUtil.toSimple(artist)
            album = ZhConverterUtil.toSimple(album)
        }
    }
}
