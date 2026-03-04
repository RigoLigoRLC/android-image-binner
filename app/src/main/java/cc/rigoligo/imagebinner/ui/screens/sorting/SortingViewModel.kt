package cc.rigoligo.imagebinner.ui.screens.sorting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.rigoligo.imagebinner.data.local.entity.SessionAssignmentEntity
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.data.media.PhotoItem
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SessionManager
import cc.rigoligo.imagebinner.domain.SortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SortingViewModel(
    private val sessionManager: SessionManager,
    private val profileManager: ProfileManager,
    private val listPhotosByAlbum: (String, SortOrder) -> List<PhotoItem>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    constructor(
        sessionManager: SessionManager,
        profileManager: ProfileManager,
        mediaStoreRepository: MediaStoreRepository,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        sessionManager = sessionManager,
        profileManager = profileManager,
        listPhotosByAlbum = mediaStoreRepository::listPhotosByAlbum,
        ioDispatcher = ioDispatcher
    )

    private val mutationMutex = Mutex()

    private val _uiState = MutableStateFlow(SortingUiState())
    val uiState: StateFlow<SortingUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<SortingUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SortingUiEvent> = _events.asSharedFlow()

    fun loadSession(profileId: Long) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val previousState = _uiState.value
                val nextState = withContext(ioDispatcher) {
                    val profile = profileManager.getProfile(profileId) ?: return@withContext null

                    val existingSession = sessionManager.getCurrentSession()
                    if (existingSession == null || existingSession.profileId != profileId) {
                        sessionManager.createOrReplaceSession(profileId = profileId)
                    }

                    val activeSession = sessionManager.getCurrentSession() ?: return@withContext null
                    val sortOrder = SortOrder.fromStorage(activeSession.activeSortOrder)
                    val media = listPhotosByAlbum(profile.sourceAlbumId, sortOrder)
                    val mediaIds = media.mapTo(mutableSetOf()) { it.id }
                    val assignments = sessionManager.getAssignments()
                        .asSequence()
                        .filter { mediaIds.contains(it.mediaId) }
                        .associate { it.mediaId to it.targetAlbumId }

                    val boundedIndex = activeSession.currentIndex.coerceIn(0, media.size)
                    sessionManager.updateCurrentIndex(boundedIndex)

                    buildState(
                        previous = previousState,
                        profileId = profile.id,
                        sourceAlbumId = profile.sourceAlbumId,
                        destinationAlbumIds = profile.destinations.map { it.albumId },
                        mediaItems = media,
                        assignments = assignments,
                        currentIndex = boundedIndex,
                        activeSortOrder = sortOrder
                    )
                } ?: return@withLock

                _uiState.value = nextState
            }
        }
    }

    fun assignCurrentToDestination(albumId: String) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                if (!state.destinationAlbumIds.contains(albumId)) {
                    return@withLock
                }
                assignCurrentLocked(targetAlbumId = albumId)
            }
        }
    }

    fun assignCurrentToTrash() {
        viewModelScope.launch {
            mutationMutex.withLock {
                assignCurrentLocked(targetAlbumId = TRASH_TARGET_ID)
            }
        }
    }

    fun setSortOrderOverride(
        sortOrder: SortOrder,
        repositionMode: SortRepositionMode = SortRepositionMode.STAY_AT_PLACE
    ) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val currentState = _uiState.value
                val profileId = currentState.profileId ?: return@withLock
                val focusedMediaId = currentState.currentMediaId
                val isSessionDone = currentState.currentMediaId == null &&
                    currentState.currentIndex >= currentState.mediaItems.size

                val nextState = withContext(ioDispatcher) {
                    sessionManager.updateActiveSortOrder(sortOrder.storageValue)

                    val sortedMedia = listPhotosByAlbum(currentState.sourceAlbumId, sortOrder)
                    val nextIndex = when (repositionMode) {
                        SortRepositionMode.STAY_AT_PLACE -> {
                            if (isSessionDone) {
                                sortedMedia.size
                            } else {
                                focusedMediaId?.let { mediaId ->
                                    sortedMedia.indexOfFirst { it.id == mediaId }.takeIf { it >= 0 }
                                } ?: 0
                            }
                        }

                        SortRepositionMode.FIRST_IMAGE -> {
                            if (sortedMedia.isEmpty()) {
                                0
                            } else {
                                0
                            }
                        }

                        SortRepositionMode.FIRST_UNSORTED -> {
                            val firstUnsortedIndex = sortedMedia.indexOfFirst { mediaItem ->
                                !currentState.assignments.containsKey(mediaItem.id)
                            }
                            if (firstUnsortedIndex >= 0) {
                                firstUnsortedIndex
                            } else {
                                sortedMedia.size
                            }
                        }
                    }
                    val boundedIndex = nextIndex.coerceIn(0, sortedMedia.size)

                    sessionManager.updateCurrentIndex(boundedIndex)

                    buildState(
                        previous = currentState,
                        profileId = profileId,
                        sourceAlbumId = currentState.sourceAlbumId,
                        destinationAlbumIds = currentState.destinationAlbumIds,
                        mediaItems = sortedMedia,
                        assignments = currentState.assignments.filterKeys { id ->
                            sortedMedia.any { it.id == id }
                        },
                        currentIndex = boundedIndex,
                        activeSortOrder = sortOrder
                    )
                }

                _uiState.value = nextState
            }
        }
    }

    fun showAssignmentList() {
        viewModelScope.launch {
            mutationMutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        assignmentList = state.assignmentList.copy(isVisible = true)
                    )
                }
            }
        }
    }

    fun hideAssignmentList() {
        viewModelScope.launch {
            mutationMutex.withLock {
                _uiState.update { state ->
                    state.copy(
                        assignmentList = state.assignmentList.copy(isVisible = false)
                    )
                }
            }
        }
    }

    fun moveFocusBy(delta: Int) {
        if (delta == 0) {
            return
        }
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                val lastIndex = state.mediaItems.lastIndex
                if (lastIndex < 0) {
                    return@withLock
                }

                val baseIndex = if (state.currentMediaId == null) {
                    lastIndex
                } else {
                    state.currentIndex.coerceIn(0, lastIndex)
                }
                val nextIndex = (baseIndex + delta).coerceIn(0, lastIndex)
                if (nextIndex == state.currentIndex && state.currentMediaId != null) {
                    return@withLock
                }

                val nextState = withContext(ioDispatcher) {
                    sessionManager.updateCurrentIndex(nextIndex)
                    buildState(
                        previous = state,
                        profileId = state.profileId,
                        sourceAlbumId = state.sourceAlbumId,
                        destinationAlbumIds = state.destinationAlbumIds,
                        mediaItems = state.mediaItems,
                        assignments = state.assignments,
                        currentIndex = nextIndex,
                        activeSortOrder = state.activeSortOrder
                    )
                }

                _uiState.value = nextState
            }
        }
    }

    fun focusMedia(mediaId: String) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val state = _uiState.value
                val nextIndex = state.mediaItems.indexOfFirst { item -> item.id == mediaId }
                if (nextIndex < 0 || nextIndex == state.currentIndex) {
                    return@withLock
                }

                val nextState = withContext(ioDispatcher) {
                    sessionManager.updateCurrentIndex(nextIndex)
                    buildState(
                        previous = state,
                        profileId = state.profileId,
                        sourceAlbumId = state.sourceAlbumId,
                        destinationAlbumIds = state.destinationAlbumIds,
                        mediaItems = state.mediaItems,
                        assignments = state.assignments,
                        currentIndex = nextIndex,
                        activeSortOrder = state.activeSortOrder
                    )
                }

                _uiState.value = nextState
            }
        }
    }

    private suspend fun assignCurrentLocked(targetAlbumId: String) {
        val currentState = _uiState.value
        val mediaId = currentState.currentMediaId ?: return
        val mediaDisplayName = currentState.currentMedia
            ?.displayName
            ?.takeIf { it.isNotBlank() }
        val mediaFallbackIndex = currentState.currentIndex + 1

        val nextState = withContext(ioDispatcher) {
            sessionManager.saveAssignment(
                mediaId = mediaId,
                targetAlbumId = targetAlbumId,
                validityState = SessionAssignmentEntity.VALIDITY_VALID
            )

            val nextIndex = (currentState.currentIndex + 1).coerceAtMost(currentState.mediaItems.size)
            sessionManager.updateCurrentIndex(nextIndex)

            val updatedAssignments = currentState.assignments + (mediaId to targetAlbumId)
            buildState(
                previous = currentState,
                profileId = currentState.profileId,
                sourceAlbumId = currentState.sourceAlbumId,
                destinationAlbumIds = currentState.destinationAlbumIds,
                mediaItems = currentState.mediaItems,
                assignments = updatedAssignments,
                currentIndex = nextIndex,
                activeSortOrder = currentState.activeSortOrder
            )
        }

        _uiState.value = nextState
        _events.tryEmit(
            SortingUiEvent.AssignmentApplied(
                mediaDisplayName = mediaDisplayName,
                mediaFallbackIndex = mediaFallbackIndex,
                targetAlbumId = targetAlbumId
            )
        )
    }

    private fun buildState(
        previous: SortingUiState,
        profileId: Long?,
        sourceAlbumId: String,
        destinationAlbumIds: List<String>,
        mediaItems: List<PhotoItem>,
        assignments: Map<String, String>,
        currentIndex: Int,
        activeSortOrder: SortOrder
    ): SortingUiState {
        val boundedIndex = currentIndex.coerceIn(0, mediaItems.size)
        val currentMedia = mediaItems.getOrNull(boundedIndex)
        val progressCurrent = when {
            mediaItems.isEmpty() -> 0
            currentMedia == null -> mediaItems.size
            else -> boundedIndex + 1
        }
        val mediaById = mediaItems.associateBy { it.id }
        val mediaIndexById = mediaItems
            .mapIndexed { index, photoItem -> photoItem.id to index }
            .toMap()
        val assignmentItems = assignments.entries
            .sortedBy { entry ->
                mediaIndexById[entry.key] ?: Int.MAX_VALUE
            }
            .map { entry ->
                val mediaItem = mediaById[entry.key]
                val mediaLabel = mediaItem
                    ?.displayName
                    ?.takeIf { label -> label.isNotBlank() }
                val mediaFallbackIndex = mediaIndexById[entry.key]?.plus(1)
                AssignmentListItem(
                    mediaId = entry.key,
                    targetAlbumId = entry.value,
                    mediaLabel = mediaLabel,
                    mediaFallbackIndex = mediaFallbackIndex,
                    capturedAt = mediaById[entry.key]?.capturedAt
                )
            }

        return previous.copy(
            profileId = profileId,
            sourceAlbumId = sourceAlbumId,
            destinationAlbumIds = destinationAlbumIds,
            mediaItems = mediaItems,
            currentIndex = boundedIndex,
            currentMediaId = currentMedia?.id,
            currentMedia = currentMedia,
            assignments = assignments,
            activeSortOrder = activeSortOrder,
            overlay = SortingOverlayState(
                progressLabel = "$progressCurrent/${mediaItems.size}",
                capturedAt = currentMedia?.capturedAt,
                assignedCount = assignments.size,
                canCommit = assignments.isNotEmpty()
            ),
            assignmentList = previous.assignmentList.copy(items = assignmentItems)
        )
    }

    companion object {
        const val TRASH_TARGET_ID: String = "__TRASH__"
    }
}

data class SortingUiState(
    val profileId: Long? = null,
    val sourceAlbumId: String = "",
    val destinationAlbumIds: List<String> = emptyList(),
    val mediaItems: List<PhotoItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentMediaId: String? = null,
    val currentMedia: PhotoItem? = null,
    val assignments: Map<String, String> = emptyMap(),
    val activeSortOrder: SortOrder = SortOrder.OLDEST_FIRST,
    val overlay: SortingOverlayState = SortingOverlayState(),
    val assignmentList: AssignmentListUiState = AssignmentListUiState()
)

data class SortingOverlayState(
    val progressLabel: String = "0/0",
    val capturedAt: Long? = null,
    val assignedCount: Int = 0,
    val canCommit: Boolean = false
)

data class AssignmentListUiState(
    val isVisible: Boolean = false,
    val items: List<AssignmentListItem> = emptyList()
)

data class AssignmentListItem(
    val mediaId: String,
    val targetAlbumId: String,
    val mediaLabel: String?,
    val mediaFallbackIndex: Int?,
    val capturedAt: Long?
)

sealed interface SortingUiEvent {
    data class AssignmentApplied(
        val mediaDisplayName: String?,
        val mediaFallbackIndex: Int,
        val targetAlbumId: String
    ) : SortingUiEvent
}

enum class SortRepositionMode {
    STAY_AT_PLACE,
    FIRST_UNSORTED,
    FIRST_IMAGE
}
