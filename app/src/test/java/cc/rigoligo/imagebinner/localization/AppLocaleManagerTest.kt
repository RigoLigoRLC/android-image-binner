package cc.rigoligo.imagebinner.localization

import cc.rigoligo.imagebinner.domain.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleManagerTest {
    private val manager = AppLocaleManager()

    @Test
    fun localeTagFor_returnsNullForSystem() {
        assertNull(manager.localeTagFor(AppLanguage.SYSTEM))
    }

    @Test
    fun localeTagFor_returnsEnglishTag() {
        assertEquals("en", manager.localeTagFor(AppLanguage.ENGLISH))
    }

    @Test
    fun localeTagFor_returnsZhCnForSimplifiedChinese() {
        assertEquals("zh-CN", manager.localeTagFor(AppLanguage.SIMPLIFIED_CHINESE))
    }
}
