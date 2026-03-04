package cc.rigoligo.imagebinner.ui.screens.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.rigoligo.imagebinner.domain.AppLanguage
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.domain.SortOrder
import cc.rigoligo.imagebinner.domain.TrashMode
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

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val sdkIntProvider: () -> Int = { Build.VERSION.SDK_INT },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val applyLanguage: (AppLanguage) -> Unit = {}
) : ViewModel() {
    private val mutationMutex = Mutex()
    private val supportsSystemTrash: Boolean = sdkIntProvider() >= SYSTEM_TRASH_MIN_API

    private val _uiState = MutableStateFlow(
        SettingsUiState(supportsSystemTrash = supportsSystemTrash)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mutationMutex.withLock {
                val settings = withContext(ioDispatcher) {
                    settingsManager.getSettings()
                }
                val resolvedTrashMode = supportedTrashMode(settings.trashMode)
                if (resolvedTrashMode != settings.trashMode) {
                    withContext(ioDispatcher) {
                        settingsManager.updateTrashMode(resolvedTrashMode)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        defaultSortOrder = settings.defaultSortOrder,
                        trashMode = resolvedTrashMode,
                        language = settings.language
                    )
                }
            }
        }
    }

    fun setDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            mutationMutex.withLock {
                withContext(ioDispatcher) {
                    settingsManager.updateDefaultSortOrder(sortOrder)
                }
                _uiState.update { state ->
                    state.copy(defaultSortOrder = sortOrder)
                }
            }
        }
    }

    fun setTrashMode(trashMode: TrashMode) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val resolvedMode = supportedTrashMode(trashMode)
                withContext(ioDispatcher) {
                    settingsManager.updateTrashMode(resolvedMode)
                }
                _uiState.update { state ->
                    state.copy(trashMode = resolvedMode)
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            mutationMutex.withLock {
                withContext(ioDispatcher) {
                    settingsManager.updateLanguage(language)
                }
                _uiState.update { state ->
                    state.copy(language = language)
                }
                applyLanguage(language)
            }
        }
    }

    private fun supportedTrashMode(requested: TrashMode): TrashMode {
        return if (supportsSystemTrash) {
            requested
        } else {
            TrashMode.TRASH_ALBUM
        }
    }

    companion object {
        private const val SYSTEM_TRASH_MIN_API: Int = 30
    }
}

data class SettingsUiState(
    val defaultSortOrder: SortOrder = SortOrder.OLDEST_FIRST,
    val trashMode: TrashMode = TrashMode.TRASH_ALBUM,
    val supportsSystemTrash: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM
)
