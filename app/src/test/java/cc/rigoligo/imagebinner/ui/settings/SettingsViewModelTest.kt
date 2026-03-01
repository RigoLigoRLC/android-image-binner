package cc.rigoligo.imagebinner.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.domain.SortOrder
import cc.rigoligo.imagebinner.domain.TrashMode
import cc.rigoligo.imagebinner.ui.screens.settings.SettingsViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun api30plus_canSelectSystemTrash_orTrashAlbum() = runTest {
        val manager = SettingsManager(db.settingsDao())
        val viewModel = SettingsViewModel(
            settingsManager = manager,
            sdkIntProvider = { 30 },
            ioDispatcher = ioDispatcher
        )
        flushViewModelWork()

        assertTrue(viewModel.uiState.value.supportsSystemTrash)

        viewModel.setTrashMode(TrashMode.TRASH_ALBUM)
        flushViewModelWork()
        assertEquals(TrashMode.TRASH_ALBUM, viewModel.uiState.value.trashMode)

        viewModel.setTrashMode(TrashMode.SYSTEM_TRASH)
        flushViewModelWork()
        assertEquals(TrashMode.SYSTEM_TRASH, viewModel.uiState.value.trashMode)
        assertEquals(TrashMode.SYSTEM_TRASH, manager.getSettings().trashMode)
    }

    @Test
    fun api24to29_forcesTrashAlbumAndIgnoresSystemTrashSelection() = runTest {
        val manager = SettingsManager(db.settingsDao())
        val viewModel = SettingsViewModel(
            settingsManager = manager,
            sdkIntProvider = { 29 },
            ioDispatcher = ioDispatcher
        )
        flushViewModelWork()

        assertFalse(viewModel.uiState.value.supportsSystemTrash)
        assertEquals(TrashMode.TRASH_ALBUM, viewModel.uiState.value.trashMode)
        assertEquals(TrashMode.TRASH_ALBUM, manager.getSettings().trashMode)

        viewModel.setTrashMode(TrashMode.SYSTEM_TRASH)
        flushViewModelWork()

        assertEquals(TrashMode.TRASH_ALBUM, viewModel.uiState.value.trashMode)
        assertEquals(TrashMode.TRASH_ALBUM, manager.getSettings().trashMode)
    }

    @Test
    fun setDefaultSortOrder_updatesStateAndPersistence() = runTest {
        val manager = SettingsManager(db.settingsDao())
        val viewModel = SettingsViewModel(
            settingsManager = manager,
            sdkIntProvider = { 30 },
            ioDispatcher = ioDispatcher
        )
        flushViewModelWork()

        viewModel.setDefaultSortOrder(SortOrder.NEWEST_FIRST)
        flushViewModelWork()

        assertEquals(SortOrder.NEWEST_FIRST, viewModel.uiState.value.defaultSortOrder)
        assertEquals(SortOrder.NEWEST_FIRST, manager.getSettings().defaultSortOrder)
    }

    private fun TestScope.flushViewModelWork() {
        repeat(3) {
            advanceUntilIdle()
            ioDispatcher.scheduler.runCurrent()
        }
        advanceUntilIdle()
    }
}
