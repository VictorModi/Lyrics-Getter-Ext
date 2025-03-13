package statusbar.finder.modifiers

import statusbar.finder.QueryProcessor
import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.modifiers
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/13 17:42
 */
class SimplifiedQueryProcessor : QueryProcessor() {
    override fun modify(mediaInfo: MediaInfo): MediaInfo {
        return mediaInfo.toSimple()
    }
}
