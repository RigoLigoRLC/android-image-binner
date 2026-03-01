package cc.rigoligo.imagebinner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_assignments")
data class SessionAssignmentEntity(
    @PrimaryKey
    val mediaId: String,
    val targetAlbumId: String,
    val validityState: String
) {
    companion object {
        const val VALIDITY_VALID: String = "VALID"
        const val VALIDITY_INVALID_BIN: String = "INVALID_BIN"
    }
}
