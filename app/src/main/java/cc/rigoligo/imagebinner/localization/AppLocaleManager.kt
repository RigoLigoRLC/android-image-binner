package cc.rigoligo.imagebinner.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cc.rigoligo.imagebinner.domain.AppLanguage

class AppLocaleManager {
    fun localeTagFor(language: AppLanguage): String? {
        return when (language) {
            AppLanguage.SYSTEM -> null
            AppLanguage.ENGLISH -> "en"
            AppLanguage.SIMPLIFIED_CHINESE -> "zh-CN"
        }
    }

    fun applyLanguage(language: AppLanguage) {
        val locales = localeTagFor(language)
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
