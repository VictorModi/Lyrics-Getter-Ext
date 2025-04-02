package statusbar.finder.modifier

import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:38
 */
interface Modifier {
    fun modify(mediaInfo: MediaInfo, originId: Long): MediaInfo?
}
