package cc.rigoligo.imagebinner.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.local.entity.AppSettingsEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerLanguageTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: SettingsManager

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        manager = SettingsManager(db.settingsDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getSettings_defaultsToSystemLanguage_whenNothingPersisted() {
        val settings = manager.getSettings()

        assertEquals(AppLanguage.SYSTEM, settings.language)
    }

    @Test
    fun updateLanguage_persistsSelection() {
        manager.updateLanguage(AppLanguage.SIMPLIFIED_CHINESE)

        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, manager.getSettings().language)
    }

    @Test
    fun getSettings_fallsBackToSystem_whenStoredLanguageIsUnknown() {
        db.settingsDao().upsert(
            AppSettingsEntity(
                language = "NOT_A_REAL_LANGUAGE"
            )
        )

        assertEquals(AppLanguage.SYSTEM, manager.getSettings().language)
    }
}
