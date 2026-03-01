package cc.rigoligo.imagebinner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.rigoligo.imagebinner.data.local.entity.AppSettingsEntity

@Dao
abstract class SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun upsertInternal(settings: AppSettingsEntity): Long

    @Query("SELECT * FROM app_settings WHERE id = 1")
    abstract fun get(): AppSettingsEntity?

    open fun upsert(settings: AppSettingsEntity): Long {
        return upsertInternal(settings.copy(id = AppSettingsEntity.SINGLETON_ID))
    }
}
