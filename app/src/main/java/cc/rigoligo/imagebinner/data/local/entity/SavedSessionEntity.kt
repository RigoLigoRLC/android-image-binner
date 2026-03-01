package cc.rigoligo.imagebinner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_session")
data class SavedSessionEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val profileId: Long,
    val currentIndex: Int,
    val activeSortOrder: String
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
