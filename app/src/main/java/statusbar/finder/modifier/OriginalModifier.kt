package statusbar.finder.modifier

import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.modifiers
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:40
 */
class OriginalModifier : Modifier {
    override fun modify(mediaInfo: MediaInfo, originId: Long): MediaInfo? {
        return mediaInfo
    }
}
