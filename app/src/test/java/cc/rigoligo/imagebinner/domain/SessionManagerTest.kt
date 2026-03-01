package cc.rigoligo.imagebinner.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {
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
    fun evaluateStartRequest_noSession_returnsStart() {
        val manager = SessionManager(db.sessionDao())

        val decision = manager.evaluateStartRequest(requestedProfileId = 1L)

        assertEquals(StartDecision.Start, decision)
    }

    @Test
    fun evaluateStartRequest_sameProfile_requiresResumeOrRestartChoice() {
        val manager = SessionManager(db.sessionDao())
        manager.createOrReplaceSession(profileId = 42L)

        val decision = manager.evaluateStartRequest(requestedProfileId = 42L)

        assertEquals(StartDecision.RequiresResumeOrRestartChoice, decision)
    }

    @Test
    fun evaluateStartRequest_differentProfile_requiresResumeOrDiscardChoice() {
        val manager = SessionManager(db.sessionDao())
        manager.createOrReplaceSession(profileId = 1L)

        val decision = manager.evaluateStartRequest(requestedProfileId = 2L)

        assertEquals(StartDecision.RequiresResumeOrDiscardChoice, decision)
    }

    @Test
    fun createOrReplaceSession_clearsAssignments_andUsesOldestFirstByDefault() {
        val sessionDao = db.sessionDao()
        sessionDao.upsertAssignment(
            mediaId = "media-1",
            targetAlbumId = "bin-a",
            validityState = SessionAssignmentEntity.VALIDITY_VALID
        )
        val manager = SessionManager(sessionDao)

        manager.createOrReplaceSession(profileId = 99L)

        assertEquals(emptyList<SessionAssignmentEntity>(), sessionDao.getAssignments())
        assertEquals("OLDEST_FIRST", manager.getCurrentSession()?.activeSortOrder)
    }
}
