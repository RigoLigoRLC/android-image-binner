package cc.rigoligo.imagebinner.domain

import cc.rigoligo.imagebinner.data.local.dao.ProfileAggregate
import cc.rigoligo.imagebinner.data.local.dao.ProfileDao

class ProfileManager(
    private val profileDao: ProfileDao
) {
    fun listProfiles(): List<Profile> {
        return profileDao.getAllProfileAggregates().map { it.toDomain() }
    }

    fun createProfile(
        name: String,
        sourceAlbumId: String,
        destinationAlbumIds: List<String> = emptyList()
    ): Profile {
        val profileId = profileDao.insertProfile(name = name, sourceAlbumId = sourceAlbumId)
        profileDao.replaceDestinations(profileId = profileId, albumIds = destinationAlbumIds)
        return requireNotNull(getProfile(profileId))
    }

    fun updateProfile(
        profileId: Long,
        name: String,
        sourceAlbumId: String,
        destinationAlbumIds: List<String>
    ): Profile {
        val updated = profileDao.updateProfile(
            profileId = profileId,
            name = name,
            sourceAlbumId = sourceAlbumId
        )
        require(updated) { "Profile $profileId does not exist" }

        profileDao.replaceDestinations(profileId = profileId, albumIds = destinationAlbumIds)
        return requireNotNull(getProfile(profileId))
    }

    fun autoSaveDestinations(profileId: Long, destinationAlbumIds: List<String>) {
        profileDao.replaceDestinations(profileId = profileId, albumIds = destinationAlbumIds)
    }

    fun getProfile(profileId: Long): Profile? {
        return profileDao.getProfileAggregate(profileId)?.toDomain()
    }

    fun deleteProfile(profileId: Long): Boolean {
        return profileDao.deleteProfile(profileId)
    }

    private fun ProfileAggregate.toDomain(): Profile {
        return Profile(
            id = profile.id,
            name = profile.name,
            sourceAlbumId = profile.sourceAlbumId,
            destinations = destinations.map {
                ProfileDestination(albumId = it.albumId)
            }
        )
    }
}

data class Profile(
    val id: Long,
    val name: String,
    val sourceAlbumId: String,
    val destinations: List<ProfileDestination>
)

data class ProfileDestination(
    val albumId: String
)
