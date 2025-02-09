package statusbar.finder.data

/**
 * LyricGetterExt - statusbar.finder.data
 * @description TODO: coming soon.
 * @author VictorModi
 * @email victormodi@outlook.com
 * @date 2025/2/9 14:02
 */
enum class DataOrigin {
    UNDEFINED,
    INTERNET,
    DATABASE;

    fun getCapitalizedName(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
