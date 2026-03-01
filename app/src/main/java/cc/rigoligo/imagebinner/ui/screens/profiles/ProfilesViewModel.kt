package cc.rigoligo.imagebinner.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.rigoligo.imagebinner.data.media.AlbumItem
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.Profile
import cc.rigoligo.imagebinner.domain.ProfileManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ProfilesViewModel(
    private val profileManager: ProfileManager,
    private val mediaStoreRepository: MediaStoreRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val mutationMutex = Mutex()

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadProfiles()
            loadAlbums()
        }
    }

    fun createProfile(name: String = nextProfileName(), sourceAlbumId: String? = null) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val sourceId = sourceAlbumId ?: _uiState.value.availableAlbums.firstOrNull()?.id ?: return@withLock
                val normalizedName = name.trim().ifBlank { nextProfileName() }
                val createdProfile = withContext(ioDispatcher) {
                    profileManager.createProfile(
                        name = normalizedName,
                        sourceAlbumId = sourceId
                    )
                }
                upsertSelectedProfile(createdProfile.toUiModel())
            }
        }
    }

    fun selectProfile(profileId: Long) {
        val current = _uiState.value.profiles.firstOrNull { it.id == profileId }
        if (current != null) {
            _uiState.update { it.copy(selectedProfileId = profileId) }
            return
        }

        viewModelScope.launch {
            val persisted = withContext(ioDispatcher) {
                profileManager.getProfile(profileId)
            } ?: return@launch
            upsertSelectedProfile(persisted.toUiModel())
        }
    }

    fun setSourceAlbum(albumId: String) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val selected = _uiState.value.selectedProfile ?: return@withLock
                val updated = withContext(ioDispatcher) {
                    profileManager.updateProfile(
                        profileId = selected.id,
                        name = selected.name,
                        sourceAlbumId = albumId,
                        destinationAlbumIds = selected.destinationAlbumIds
                    )
                }
                upsertSelectedProfile(updated.toUiModel())
            }
        }
    }

    fun addDestination(albumId: String) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val selected = _uiState.value.selectedProfile ?: return@withLock
                if (selected.destinationAlbumIds.contains(albumId)) {
                    return@withLock
                }

                val updatedDestinations = selected.destinationAlbumIds + albumId
                withContext(ioDispatcher) {
                    profileManager.autoSaveDestinations(
                        profileId = selected.id,
                        destinationAlbumIds = updatedDestinations
                    )
                }
                updateSelectedDestinations(updatedDestinations)
            }
        }
    }

    fun removeDestination(albumId: String) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val selected = _uiState.value.selectedProfile ?: return@withLock
                val updatedDestinations = selected.destinationAlbumIds.filterNot { it == albumId }
                withContext(ioDispatcher) {
                    profileManager.autoSaveDestinations(
                        profileId = selected.id,
                        destinationAlbumIds = updatedDestinations
                    )
                }
                updateSelectedDestinations(updatedDestinations)
            }
        }
    }

    fun deleteSelectedProfile() {
        viewModelScope.launch {
            mutationMutex.withLock {
                val selectedId = _uiState.value.selectedProfileId ?: return@withLock
                val deleted = withContext(ioDispatcher) {
                    profileManager.deleteProfile(selectedId)
                }
                if (!deleted) {
                    return@withLock
                }
                _uiState.update { state ->
                    val remaining = state.profiles.filterNot { it.id == selectedId }
                    state.copy(
                        profiles = remaining,
                        selectedProfileId = remaining.firstOrNull()?.id
                    )
                }
            }
        }
    }

    private fun updateSelectedDestinations(destinations: List<String>) {
        val selected = _uiState.value.selectedProfile ?: return
        val updated = selected.copy(destinationAlbumIds = destinations)
        _uiState.update { state ->
            state.copy(
                profiles = state.profiles.map { profile ->
                    if (profile.id == updated.id) {
                        updated
                    } else {
                        profile
                    }
                }
            )
        }
    }

    private suspend fun loadProfiles() {
        val profiles = withContext(ioDispatcher) {
            profileManager.listProfiles()
        }.map { it.toUiModel() }
        _uiState.update { it.copy(profiles = profiles) }
    }

    private suspend fun loadAlbums() {
        val albums = withContext(ioDispatcher) {
            runCatching { mediaStoreRepository.listAlbums() }
                .getOrDefault(emptyList())
        }
        if (albums.isEmpty()) {
            return
        }

        _uiState.update { it.copy(availableAlbums = albums) }
    }

    private fun upsertSelectedProfile(profile: ProfileItemUi) {
        _uiState.update { state ->
            val existingIndex = state.profiles.indexOfFirst { it.id == profile.id }
            val updatedProfiles = if (existingIndex >= 0) {
                state.profiles.toMutableList().also { profiles ->
                    profiles[existingIndex] = profile
                }
            } else {
                state.profiles + profile
            }

            state.copy(
                profiles = updatedProfiles,
                selectedProfileId = profile.id
            )
        }
    }

    private fun nextProfileName(): String {
        return "Profile ${_uiState.value.profiles.size + 1}"
    }

    private fun Profile.toUiModel(): ProfileItemUi {
        return ProfileItemUi(
            id = id,
            name = name,
            sourceAlbumId = sourceAlbumId,
            destinationAlbumIds = destinations.map { it.albumId }
        )
    }
}

data class ProfilesUiState(
    val profiles: List<ProfileItemUi> = emptyList(),
    val selectedProfileId: Long? = null,
    val availableAlbums: List<AlbumItem> = emptyList()
) {
    val selectedProfile: ProfileItemUi?
        get() = profiles.firstOrNull { it.id == selectedProfileId }
}

data class ProfileItemUi(
    val id: Long,
    val name: String,
    val sourceAlbumId: String,
    val destinationAlbumIds: List<String>
)
