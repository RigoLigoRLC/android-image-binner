package cc.rigoligo.imagebinner.ui.screens.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.media.AlbumItem
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SessionManager

@Composable
fun ProfilesRoute(
    onOpenSorting: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) {
        AppDatabase.getInstance(context)
    }
    val viewModel = remember(database, context) {
        ProfilesViewModel(
            profileManager = ProfileManager(database.profileDao()),
            mediaStoreRepository = MediaStoreRepository(context.contentResolver)
        )
    }
    val sessionManager = remember(database) {
        SessionManager(database.sessionDao())
    }

    ProfilesScreen(
        viewModel = viewModel,
        sessionManager = sessionManager,
        onOpenSorting = onOpenSorting,
        modifier = modifier
    )
}

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    modifier: Modifier = Modifier,
    sessionManager: SessionManager? = null,
    onOpenSorting: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    val selectedProfile = state.selectedProfile

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { viewModel.createProfile() },
            enabled = state.availableAlbums.isNotEmpty()
        ) {
            Text(text = "Create profile")
        }
        if (state.availableAlbums.isEmpty()) {
            Text(text = "No albums available to create a profile.")
        }

        if (state.profiles.isEmpty()) {
            Text(text = "No profiles yet")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == state.selectedProfileId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProfile(profile.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(text = "Source: ${albumNameById(profile.sourceAlbumId, state.availableAlbums)}")
                            Text(text = "Destinations: ${profile.destinationAlbumIds.size}")
                            if (isSelected) {
                                Text(text = "Selected")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showEditor = true },
                enabled = selectedProfile != null
            ) {
                Text(text = "Edit selected")
            }
            Button(
                onClick = { viewModel.deleteSelectedProfile() },
                enabled = selectedProfile != null
            ) {
                Text(text = "Delete selected")
            }
        }

        if (sessionManager != null && selectedProfile != null) {
            SessionStartPromptScaffold(
                requestedProfileId = selectedProfile.id,
                sessionManager = sessionManager,
                onResumeExistingSession = onOpenSorting,
                onStartRequestedProfile = onOpenSorting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showEditor && selectedProfile != null) {
        ProfileEditorDialog(
            profileName = selectedProfile.name,
            sourceAlbumId = selectedProfile.sourceAlbumId,
            destinationAlbumIds = selectedProfile.destinationAlbumIds,
            availableAlbums = state.availableAlbums,
            onDismissRequest = { showEditor = false },
            onSourceAlbumSelected = { albumId ->
                viewModel.setSourceAlbum(albumId)
            },
            onAddDestination = { albumId ->
                viewModel.addDestination(albumId)
            },
            onRemoveDestination = { albumId ->
                viewModel.removeDestination(albumId)
            }
        )
    }
}

private fun albumNameById(albumId: String, albums: List<AlbumItem>): String {
    return albums.firstOrNull { it.id == albumId }?.name ?: "Unknown album"
}
