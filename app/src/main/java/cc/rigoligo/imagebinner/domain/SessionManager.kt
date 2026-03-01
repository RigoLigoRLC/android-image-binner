package cc.rigoligo.imagebinner.domain

import cc.rigoligo.imagebinner.data.local.dao.SessionDao
import cc.rigoligo.imagebinner.data.local.entity.SavedSessionEntity
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity

class SessionManager(
    private val sessionDao: SessionDao
) {
    fun evaluateStartRequest(requestedProfileId: Long): StartDecision {
        val existingSession = sessionDao.getGlobalSession() ?: return StartDecision.Start

        return if (existingSession.profileId == requestedProfileId) {
            StartDecision.RequiresResumeOrRestartChoice
        } else {
            StartDecision.RequiresResumeOrDiscardChoice
        }
    }

    fun createOrReplaceSession(
        profileId: Long,
        currentIndex: Int = 0,
        activeSortOrder: String = DEFAULT_SORT_ORDER
    ) {
        sessionDao.clearAssignments()
        sessionDao.saveGlobalSession(
            profileId = profileId,
            currentIndex = currentIndex,
            activeSortOrder = activeSortOrder
        )
    }

    fun getCurrentSession(): SavedSessionEntity? {
        return sessionDao.getGlobalSession()
    }

    fun getAssignments(): List<SessionAssignmentEntity> {
        return sessionDao.getAssignments()
    }

    fun saveAssignment(mediaId: String, targetAlbumId: String, validityState: String) {
        sessionDao.upsertAssignment(
            mediaId = mediaId,
            targetAlbumId = targetAlbumId,
            validityState = validityState
        )
    }

    fun updateCurrentIndex(currentIndex: Int) {
        updateSession { it.copy(currentIndex = currentIndex) }
    }

    fun updateActiveSortOrder(activeSortOrder: String) {
        updateSession { it.copy(activeSortOrder = activeSortOrder) }
    }

    fun clearSession() {
        sessionDao.clearGlobalSession()
        sessionDao.clearAssignments()
    }

    private fun updateSession(transform: (SavedSessionEntity) -> SavedSessionEntity) {
        val current = sessionDao.getGlobalSession() ?: return
        val updated = transform(current)
        sessionDao.saveGlobalSession(
            profileId = updated.profileId,
            currentIndex = updated.currentIndex,
            activeSortOrder = updated.activeSortOrder
        )
    }

    companion object {
        private const val DEFAULT_SORT_ORDER = "OLDEST_FIRST"
    }
}

enum class StartDecision {
    Start,
    RequiresResumeOrRestartChoice,
    RequiresResumeOrDiscardChoice
}
