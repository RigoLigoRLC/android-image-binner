package cc.rigoligo.imagebinner.ui.screens.sorting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SortingRoute(
    profileId: Long,
    onBack: () -> Unit,
    onCommit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) {
        AppDatabase.getInstance(context)
    }
    val sessionManager = remember(database) {
        SessionManager(database.sessionDao())
    }
    val profileManager = remember(database) {
        ProfileManager(database.profileDao())
    }
    val mediaStoreRepository = remember(context) {
        MediaStoreRepository(context.contentResolver)
    }
    val viewModel = remember(sessionManager, profileManager, mediaStoreRepository) {
        SortingViewModel(
            sessionManager = sessionManager,
            profileManager = profileManager,
            mediaStoreRepository = mediaStoreRepository
        )
    }

    LaunchedEffect(viewModel, profileId) {
        withContext(Dispatchers.IO) {
            viewModel.loadSession(profileId)
        }
    }

    SortingScreen(
        viewModel = viewModel,
        onBack = onBack,
        onCommit = onCommit,
        modifier = modifier
    )
}
