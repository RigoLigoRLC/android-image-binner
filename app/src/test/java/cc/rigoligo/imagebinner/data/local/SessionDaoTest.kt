package cc.rigoligo.imagebinner.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDaoTest {
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
    fun saveSession_replacesExistingGlobalSession() = runBlocking {
        val dao = db.sessionDao()

        dao.saveGlobalSession(profileId = 10L, currentIndex = 3, activeSortOrder = "OLDEST_FIRST")
        dao.saveGlobalSession(profileId = 20L, currentIndex = 0, activeSortOrder = "NEWEST_FIRST")

        val session = dao.getGlobalSession()
        assertNotNull(session)
        assertEquals(20L, session?.profileId)
        assertEquals(0, session?.currentIndex)
        assertEquals("NEWEST_FIRST", session?.activeSortOrder)
    }

    @Test
    fun upsertAssignment_replacesTargetAndValidityForSameMediaId() = runBlocking {
        val dao = db.sessionDao()

        dao.upsertAssignment(
            mediaId = "media-1",
            targetAlbumId = "bin-a",
            validityState = SessionAssignmentEntity.VALIDITY_VALID
        )
        dao.upsertAssignment(
            mediaId = "media-1",
            targetAlbumId = "bin-b",
            validityState = SessionAssignmentEntity.VALIDITY_INVALID_BIN
        )

        val assignments = dao.getAssignments()
        assertEquals(1, assignments.size)
        assertEquals("bin-b", assignments.single().targetAlbumId)
        assertEquals(SessionAssignmentEntity.VALIDITY_INVALID_BIN, assignments.single().validityState)
    }

    @Test
    fun clearSessionAndAssignments_removesSavedRows() = runBlocking {
        val dao = db.sessionDao()

        dao.saveGlobalSession(profileId = 10L, currentIndex = 3, activeSortOrder = "OLDEST_FIRST")
        dao.upsertAssignment(
            mediaId = "media-1",
            targetAlbumId = "bin-a",
            validityState = SessionAssignmentEntity.VALIDITY_VALID
        )

        dao.clearGlobalSession()
        dao.clearAssignments()

        assertNull(dao.getGlobalSession())
        assertEquals(emptyList<SessionAssignmentEntity>(), dao.getAssignments())
    }
}
