package com.mid.varagh.core.model

/** How pages are laid out in the reader. */
enum class ReadingMode {
    /** Vertical continuous scroll. */
    VERTICAL_SCROLL,

    /** Horizontal page-by-page (right-to-left for Persian books when enabled). */
    HORIZONTAL_PAGED,
}

/** App chrome theme (not the page reading theme). */
enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
}

/** UI language. Persian is the primary language of the app. */
enum class AppLanguage(val tag: String) {
    PERSIAN("fa"),
    ENGLISH("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag) } ?: PERSIAN
    }
}

/** Everything the user can change in Settings, plus reader defaults. */
data class UserPreferences(
    val readingTheme: ReadingTheme = ReadingTheme.SEPIA,
    val customReadingColors: CustomReadingColors = CustomReadingColors.Default,
    val readingMode: ReadingMode = ReadingMode.VERTICAL_SCROLL,
    val rightToLeftPaging: Boolean = true,
    val keepScreenOn: Boolean = true,
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = false,
    val language: AppLanguage = AppLanguage.PERSIAN,
    /** 0f = no warm filter, 1f = strongest. */
    val warmFilter: Float = 0f,
    /** null = follow system brightness, otherwise 0.01f..1f window brightness. */
    val readerBrightness: Float? = null,
)
