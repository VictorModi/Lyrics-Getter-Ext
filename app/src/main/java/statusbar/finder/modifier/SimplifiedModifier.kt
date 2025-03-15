package statusbar.finder.modifier

import statusbar.finder.Modifier
import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.modifiers
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:42
 */
class SimplifiedModifier : Modifier() {
    override fun modify(mediaInfo: MediaInfo): MediaInfo {
        return mediaInfo.toSimple()
    }
}
