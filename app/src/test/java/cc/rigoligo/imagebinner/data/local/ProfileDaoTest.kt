package cc.rigoligo.imagebinner.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertProfileWithDestinations_roundTripsInPositionOrder() = runBlocking {
        val profileId = db.profileDao().insertProfile("Family", "bucket-src")
        db.profileDao().replaceDestinations(
            profileId,
            listOf("bucket-a", "bucket-b")
        )

        val aggregate = db.profileDao().getProfileAggregate(profileId)
        assertNotNull(aggregate)
        assertEquals("bucket-src", aggregate?.profile?.sourceAlbumId)
        assertEquals(
            listOf("bucket-a", "bucket-b"),
            aggregate?.destinations?.map { it.albumId }
        )
    }

    @Test
    fun upsertSettings_reusesSingleRowIdOne() = runBlocking {
        db.settingsDao().upsert(AppSettingsEntity(defaultSortOrder = "OLDEST_FIRST"))
        db.settingsDao().upsert(AppSettingsEntity(defaultSortOrder = "NEWEST_FIRST"))

        val settings = db.settingsDao().get()
        assertNotNull(settings)
        assertEquals(1, settings?.id)
        assertEquals("NEWEST_FIRST", settings?.defaultSortOrder)
    }
}
