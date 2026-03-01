package cc.rigoligo.imagebinner.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.data.media.AlbumItem

@Composable
fun ProfileEditorDialog(
    profileName: String,
    sourceAlbumId: String,
    destinationAlbumIds: List<String>,
    availableAlbums: List<AlbumItem>,
    onDismissRequest: () -> Unit,
    onSourceAlbumSelected: (String) -> Unit,
    onAddDestination: (String) -> Unit,
    onRemoveDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Edit profile: $profileName")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Source album")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    availableAlbums.forEach { album ->
                        val selectedSuffix = if (album.id == sourceAlbumId) " (selected)" else ""
                        TextButton(
                            onClick = { onSourceAlbumSelected(album.id) }
                        ) {
                            Text(text = album.name + selectedSuffix)
                        }
                    }
                }

                Text(text = "Destination albums")
                destinationAlbumIds.forEach { destinationId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = albumNameById(destinationId, availableAlbums))
                        TextButton(onClick = { onRemoveDestination(destinationId) }) {
                            Text(text = "Remove")
                        }
                    }
                }

                Text(text = "Add destination")
                availableAlbums
                    .filterNot { destinationAlbumIds.contains(it.id) }
                    .forEach { album ->
                        Button(
                            onClick = { onAddDestination(album.id) }
                        ) {
                            Text(text = "Add ${album.name}")
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Close")
            }
        }
    )
}

private fun albumNameById(albumId: String, albums: List<AlbumItem>): String {
    return albums.firstOrNull { it.id == albumId }?.name ?: "Unknown album"
}
