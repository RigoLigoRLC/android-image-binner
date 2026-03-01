package cc.rigoligo.imagebinner.ui.profiles

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.ui.screens.profiles.ProfilesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {
    private lateinit var db: AppDatabase
    private val testDispatcher = StandardTestDispatcher()
    private val ioDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
    fun addDestination_autoPersistsWithoutExplicitSaveButton() = runTest {
        val manager = ProfileManager(db.profileDao())
        val mediaRepository = MediaStoreRepository(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        )
        val viewModel = ProfilesViewModel(manager, mediaRepository, ioDispatcher = ioDispatcher)

        viewModel.createProfile(name = "Trip", sourceAlbumId = "src")
        flushViewModelWork()
        viewModel.addDestination(albumId = "dest-a")
        flushViewModelWork()

        val selectedProfileId = requireNotNull(viewModel.uiState.value.selectedProfileId)
        val profile = requireNotNull(manager.getProfile(selectedProfileId))
        assertEquals(listOf("dest-a"), profile.destinations.map { it.albumId })
    }

    @Test
    fun init_loadsExistingProfilesFromPersistence() = runTest {
        val manager = ProfileManager(db.profileDao())
        val profile = manager.createProfile(
            name = "Existing",
            sourceAlbumId = "src-existing",
            destinationAlbumIds = listOf("dest-existing")
        )
        val mediaRepository = MediaStoreRepository(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        )

        val viewModel = ProfilesViewModel(manager, mediaRepository, ioDispatcher = ioDispatcher)
        flushViewModelWork()

        assertEquals(listOf(profile.id), viewModel.uiState.value.profiles.map { it.id })
        assertEquals(
            listOf("dest-existing"),
            viewModel.uiState.value.profiles.single().destinationAlbumIds
        )
    }

    @Test
    fun createProfile_withoutSourceAndNoAlbums_doesNotPersistProfile() = runTest {
        val manager = ProfileManager(db.profileDao())
        val mediaRepository = MediaStoreRepository(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        )
        val viewModel = ProfilesViewModel(manager, mediaRepository, ioDispatcher = ioDispatcher)
        flushViewModelWork()

        viewModel.createProfile(name = "ShouldNotSave")
        flushViewModelWork()

        assertEquals(emptyList<Long>(), viewModel.uiState.value.profiles.map { it.id })
        assertEquals(emptyList<Long>(), manager.listProfiles().map { it.id })
    }

    @Test
    fun addDestination_concurrentMutations_preserveAllDestinations() = runTest {
        val manager = ProfileManager(db.profileDao())
        val mediaRepository = MediaStoreRepository(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        )
        val viewModel = ProfilesViewModel(manager, mediaRepository, ioDispatcher = ioDispatcher)
        flushViewModelWork()

        viewModel.createProfile(name = "Race", sourceAlbumId = "src")
        flushViewModelWork()

        val addA = launch { viewModel.addDestination(albumId = "dest-a") }
        val addB = launch { viewModel.addDestination(albumId = "dest-b") }

        // Let both mutations capture selected profile before IO work resumes.
        advanceUntilIdle()
        ioDispatcher.scheduler.runCurrent()
        advanceUntilIdle()
        ioDispatcher.scheduler.runCurrent()
        advanceUntilIdle()

        addA.join()
        addB.join()

        val selectedProfileId = requireNotNull(viewModel.uiState.value.selectedProfileId)
        val persisted = requireNotNull(manager.getProfile(selectedProfileId))
        assertEquals(
            listOf("dest-a", "dest-b"),
            persisted.destinations.map { it.albumId }.sorted()
        )
    }

    @Test
    fun deleteSelectedProfile_removesItFromStateAndPersistence() = runTest {
        val manager = ProfileManager(db.profileDao())
        val mediaRepository = MediaStoreRepository(
            ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        )
        val viewModel = ProfilesViewModel(manager, mediaRepository, ioDispatcher = ioDispatcher)
        flushViewModelWork()

        viewModel.createProfile(name = "Keep", sourceAlbumId = "src-keep")
        flushViewModelWork()
        viewModel.createProfile(name = "Delete", sourceAlbumId = "src-delete")
        flushViewModelWork()

        val deleteId = requireNotNull(viewModel.uiState.value.selectedProfileId)
        viewModel.deleteSelectedProfile()
        flushViewModelWork()

        assertEquals(false, viewModel.uiState.value.profiles.any { it.id == deleteId })
        assertEquals(false, manager.listProfiles().any { it.id == deleteId })
    }

    private fun TestScope.flushViewModelWork() {
        advanceUntilIdle()
        ioDispatcher.scheduler.runCurrent()
        advanceUntilIdle()
    }
}
