package statusbar.finder.modifier

import statusbar.finder.data.model.MediaInfo
import statusbar.finder.data.repository.AliasRepository

/**
 * LyricGetterExt - statusbar.finder.modifier
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/29 17:27
 */

// 这样写代码脑袋会变得平平的
class AliasModifier : Modifier {
    override fun modify(mediaInfo: MediaInfo, originId: Long): MediaInfo? {
        return AliasRepository.getAlias(originId)?.let { alias ->
            mediaInfo.copy().apply {
                this.title = alias.title ?: title
                this.artist = alias.artist ?: artist
                this.album = alias.album ?: album
            }
        }
    }
}
