package cc.rigoligo.imagebinner.domain

import cc.rigoligo.imagebinner.data.local.dao.SettingsDao
import cc.rigoligo.imagebinner.data.local.entity.AppSettingsEntity

class SettingsManager(
    private val settingsDao: SettingsDao
) {
    fun getSettings(): AppSettings {
        return settingsDao.get().toDomain()
    }

    fun getLanguage(): AppLanguage {
        return getSettings().language
    }

    fun updateDefaultSortOrder(sortOrder: SortOrder): AppSettings {
        val updated = currentOrDefault().copy(defaultSortOrder = sortOrder.storageValue)
        settingsDao.upsert(updated)
        return updated.toDomain()
    }

    fun updateTrashMode(trashMode: TrashMode): AppSettings {
        val updated = currentOrDefault().copy(trashMode = trashMode.storageValue)
        settingsDao.upsert(updated)
        return updated.toDomain()
    }

    fun updateLanguage(language: AppLanguage): AppSettings {
        val updated = currentOrDefault().copy(language = language.storageValue)
        settingsDao.upsert(updated)
        return updated.toDomain()
    }

    private fun currentOrDefault(): AppSettingsEntity {
        return settingsDao.get() ?: AppSettingsEntity()
    }

    private fun AppSettingsEntity?.toDomain(): AppSettings {
        val settings = this ?: AppSettingsEntity()
        return AppSettings(
            defaultSortOrder = SortOrder.fromStorage(settings.defaultSortOrder),
            trashMode = TrashMode.fromStorage(settings.trashMode),
            language = AppLanguage.fromStorage(settings.language)
        )
    }
}

data class AppSettings(
    val defaultSortOrder: SortOrder,
    val trashMode: TrashMode,
    val language: AppLanguage
)

enum class AppLanguage(val storageValue: String) {
    SYSTEM("SYSTEM"),
    ENGLISH("ENGLISH"),
    SIMPLIFIED_CHINESE("SIMPLIFIED_CHINESE");

    companion object {
        fun fromStorage(value: String): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

enum class SortOrder(val storageValue: String) {
    NEWEST_FIRST("NEWEST_FIRST"),
    OLDEST_FIRST("OLDEST_FIRST");

    companion object {
        fun fromStorage(value: String): SortOrder {
            return entries.firstOrNull { it.storageValue == value } ?: OLDEST_FIRST
        }
    }
}

enum class TrashMode(val storageValue: String) {
    TRASH_ALBUM("TRASH_ALBUM"),
    SYSTEM_TRASH("SYSTEM_TRASH");

    companion object {
        fun fromStorage(value: String): TrashMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM_TRASH
        }
    }
}
