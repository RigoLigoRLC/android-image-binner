package cc.rigoligo.imagebinner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cc.rigoligo.imagebinner.data.local.dao.ProfileDao
import cc.rigoligo.imagebinner.data.local.dao.SessionDao
import cc.rigoligo.imagebinner.data.local.dao.SettingsDao
import cc.rigoligo.imagebinner.data.local.entity.AppSettingsEntity
import cc.rigoligo.imagebinner.data.local.entity.ProfileDestinationEntity
import cc.rigoligo.imagebinner.data.local.entity.ProfileEntity
import cc.rigoligo.imagebinner.data.local.entity.SavedSessionEntity
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity

@Database(
    entities = [
        ProfileEntity::class,
        ProfileDestinationEntity::class,
        AppSettingsEntity::class,
        SavedSessionEntity::class,
        SessionAssignmentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun settingsDao(): SettingsDao

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }

        private const val DATABASE_NAME: String = "image_binner.db"
    }
}
