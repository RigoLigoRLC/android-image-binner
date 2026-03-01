package cc.rigoligo.imagebinner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cc.rigoligo.imagebinner.data.local.entity.ProfileDestinationEntity
import cc.rigoligo.imagebinner.data.local.entity.ProfileEntity

@Dao
abstract class ProfileDao {
    @Insert
    protected abstract fun insertProfileEntity(profile: ProfileEntity): Long

    @Insert
    protected abstract fun insertDestinationEntities(destinations: List<ProfileDestinationEntity>): List<Long>

    @Query("DELETE FROM profile_destinations WHERE profileId = :profileId")
    protected abstract fun deleteDestinationsForProfile(profileId: Long): Int

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    protected abstract fun getProfileById(profileId: Long): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY id ASC")
    protected abstract fun getAllProfilesOrdered(): List<ProfileEntity>

    @Query("SELECT * FROM profile_destinations WHERE profileId = :profileId ORDER BY position ASC")
    protected abstract fun getDestinationsByProfileId(profileId: Long): List<ProfileDestinationEntity>

    @Query("UPDATE profiles SET name = :name, sourceAlbumId = :sourceAlbumId WHERE id = :profileId")
    protected abstract fun updateProfileEntity(profileId: Long, name: String, sourceAlbumId: String): Int

    @Query("DELETE FROM profiles WHERE id = :profileId")
    protected abstract fun deleteProfileEntity(profileId: Long): Int

    @Transaction
    open fun insertProfile(name: String, sourceAlbumId: String): Long {
        return insertProfileEntity(
            ProfileEntity(
                name = name,
                sourceAlbumId = sourceAlbumId
            )
        )
    }

    @Transaction
    open fun replaceDestinations(profileId: Long, albumIds: List<String>) {
        deleteDestinationsForProfile(profileId)
        if (albumIds.isEmpty()) {
            return
        }

        insertDestinationEntities(
            albumIds.mapIndexed { index, albumId ->
                ProfileDestinationEntity(
                    profileId = profileId,
                    position = index,
                    albumId = albumId
                )
            }
        )
    }

    @Transaction
    open fun getProfileAggregate(profileId: Long): ProfileAggregate? {
        val profile = getProfileById(profileId) ?: return null
        val destinations = getDestinationsByProfileId(profileId)
        return ProfileAggregate(profile = profile, destinations = destinations)
    }

    @Transaction
    open fun getAllProfileAggregates(): List<ProfileAggregate> {
        return getAllProfilesOrdered().map { profile ->
            ProfileAggregate(
                profile = profile,
                destinations = getDestinationsByProfileId(profile.id)
            )
        }
    }

    @Transaction
    open fun updateProfile(profileId: Long, name: String, sourceAlbumId: String): Boolean {
        return updateProfileEntity(
            profileId = profileId,
            name = name,
            sourceAlbumId = sourceAlbumId
        ) > 0
    }

    @Transaction
    open fun deleteProfile(profileId: Long): Boolean {
        return deleteProfileEntity(profileId) > 0
    }
}

data class ProfileAggregate(
    val profile: ProfileEntity,
    val destinations: List<ProfileDestinationEntity>
)
