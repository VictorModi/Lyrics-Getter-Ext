package statusbar.finder.modifier

import com.github.kyuubiran.ezxhelper.Log
import statusbar.finder.BuildConfig
import statusbar.finder.data.model.MediaInfo

/**
 * LyricGetterExt - statusbar.finder.modifier
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/3/24 21:47
 */
class RemoveParenthesesModifier : Modifier {
    private fun removeBracketsAndContent(str: String): String {
        var result = str
        while (result.contains("(") || result.contains("（")) {
            result = result.replace(Regex("\\([^()]*\\)|（[^（）]*）"), "")
        }
        return result.replace("\\s+".toRegex(), " ").trim()
    }

    override fun modify(mediaInfo: MediaInfo, originId: Long): MediaInfo? {
        Log.d("${BuildConfig.APPLICATION_ID}::removeParenthesesModifier $mediaInfo")
        Log.d("${BuildConfig.APPLICATION_ID}::removeParenthesesModifier ${mediaInfo.title} -> ${removeBracketsAndContent(mediaInfo.title)}")
        return mediaInfo.copy().apply {
            title = removeBracketsAndContent(title)
        }
    }
}
