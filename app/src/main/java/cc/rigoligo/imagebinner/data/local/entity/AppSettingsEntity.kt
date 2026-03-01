package cc.rigoligo.imagebinner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val defaultSortOrder: String = "OLDEST_FIRST",
    val trashMode: String = "SYSTEM_TRASH"
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
