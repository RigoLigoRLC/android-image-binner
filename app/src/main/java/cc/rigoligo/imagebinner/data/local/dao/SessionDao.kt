package cc.rigoligo.imagebinner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.rigoligo.imagebinner.data.local.entity.SavedSessionEntity
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity

@Dao
abstract class SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertGlobalSessionInternal(session: SavedSessionEntity): Long

    @Query("SELECT * FROM saved_session WHERE id = 1")
    abstract fun getGlobalSession(): SavedSessionEntity?

    @Query("DELETE FROM saved_session WHERE id = 1")
    abstract fun clearGlobalSession(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertAssignmentInternal(assignment: SessionAssignmentEntity): Long

    @Query("SELECT * FROM session_assignments ORDER BY mediaId ASC")
    abstract fun getAssignments(): List<SessionAssignmentEntity>

    @Query("DELETE FROM session_assignments")
    abstract fun clearAssignments(): Int

    open fun saveGlobalSession(profileId: Long, currentIndex: Int, activeSortOrder: String): Long {
        return upsertGlobalSessionInternal(
            SavedSessionEntity(
                id = SavedSessionEntity.SINGLETON_ID,
                profileId = profileId,
                currentIndex = currentIndex,
                activeSortOrder = activeSortOrder
            )
        )
    }

    open fun upsertAssignment(mediaId: String, targetAlbumId: String, validityState: String): Long {
        return upsertAssignmentInternal(
            SessionAssignmentEntity(
                mediaId = mediaId,
                targetAlbumId = targetAlbumId,
                validityState = validityState
            )
        )
    }
}
