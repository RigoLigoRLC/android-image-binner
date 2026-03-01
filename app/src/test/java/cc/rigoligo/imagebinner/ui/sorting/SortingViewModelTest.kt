package cc.rigoligo.imagebinner.ui.sorting

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.media.PhotoItem
import cc.rigoligo.imagebinner.domain.Profile
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SessionManager
import cc.rigoligo.imagebinner.domain.SortOrder
import cc.rigoligo.imagebinner.ui.screens.sorting.SortingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SortingViewModelTest {
    private lateinit var db: AppDatabase
    private val mainDispatcher = StandardTestDispatcher()
    private val ioDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun assigningBin_setsOneTargetAndAdvancesIndex() = runTest {
        val (profile, viewModel) = createViewModelWithProfile()

        viewModel.loadSession(profileId = profile.id)
        assertNull(viewModel.uiState.value.currentMediaId)
        flushViewModelWork()
        val loadedState = viewModel.uiState.value
        assertEquals(listOf("m1", "m2"), loadedState.mediaItems.map { it.id })
        val firstMediaId = requireNotNull(loadedState.currentMediaId)

        viewModel.assignCurrentToDestination(albumId = "bin-a")
        assertEquals(0, viewModel.uiState.value.currentIndex)
        flushViewModelWork()

        val state = viewModel.uiState.value
        assertEquals("bin-a", state.assignments[firstMediaId])
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun assignTrash_recordsTrashTargetAndAdvancesIndex() = runTest {
        val (profile, viewModel) = createViewModelWithProfile()
        viewModel.loadSession(profileId = profile.id)
        flushViewModelWork()
        val firstMediaId = requireNotNull(viewModel.uiState.value.currentMediaId)

        viewModel.assignCurrentToTrash()
        assertEquals(0, viewModel.uiState.value.currentIndex)
        flushViewModelWork()

        val state = viewModel.uiState.value
        assertEquals(SortingViewModel.TRASH_TARGET_ID, state.assignments[firstMediaId])
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun sortOverride_whenDone_doesNotRewind() = runTest {
        val (profile, viewModel) = createViewModelWithProfile()
        viewModel.loadSession(profileId = profile.id)
        flushViewModelWork()
        viewModel.assignCurrentToDestination("bin-a")
        flushViewModelWork()
        viewModel.assignCurrentToDestination("bin-a")
        flushViewModelWork()
        val doneState = viewModel.uiState.value
        assertEquals(2, doneState.currentIndex)
        assertNull(doneState.currentMediaId)

        viewModel.setSortOrderOverride(SortOrder.NEWEST_FIRST)
        assertEquals(SortOrder.OLDEST_FIRST, viewModel.uiState.value.activeSortOrder)
        flushViewModelWork()

        val state = viewModel.uiState.value
        assertEquals(2, state.currentIndex)
        assertNull(state.currentMediaId)
        assertEquals(SortOrder.NEWEST_FIRST, state.activeSortOrder)
    }

    private fun createViewModelWithProfile(): Pair<Profile, SortingViewModel> {
        val profileManager = ProfileManager(db.profileDao())
        val profile = profileManager.createProfile(
            name = "Trip",
            sourceAlbumId = "src",
            destinationAlbumIds = listOf("bin-a")
        )
        val sourcePhotos = listOf(
            PhotoItem(id = "m1", albumId = "src", capturedAt = 1L),
            PhotoItem(id = "m2", albumId = "src", capturedAt = 2L)
        )
        val viewModel = SortingViewModel(
            sessionManager = SessionManager(db.sessionDao()),
            profileManager = profileManager,
            ioDispatcher = ioDispatcher,
            listPhotosByAlbum = { albumId, order ->
                val albumPhotos = sourcePhotos.filter { it.albumId == albumId }
                when (order) {
                    SortOrder.OLDEST_FIRST -> albumPhotos.sortedBy { it.capturedAt }
                    SortOrder.NEWEST_FIRST -> albumPhotos.sortedByDescending { it.capturedAt }
                }
            }
        )
        return profile to viewModel
    }

    private fun TestScope.flushViewModelWork() {
        repeat(3) {
            advanceUntilIdle()
            ioDispatcher.scheduler.runCurrent()
        }
        advanceUntilIdle()
    }
}
