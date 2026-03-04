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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.R
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
    val unknownAlbumLabel = stringResource(R.string.fallback_unknown_album)

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.profile_editor_title, profileName))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = stringResource(R.string.profile_editor_source_album))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    availableAlbums.forEach { album ->
                        val selectedSuffix = if (album.id == sourceAlbumId) {
                            stringResource(R.string.profile_editor_selected_suffix)
                        } else {
                            ""
                        }
                        TextButton(
                            onClick = { onSourceAlbumSelected(album.id) }
                        ) {
                            Text(text = album.name + selectedSuffix)
                        }
                    }
                }

                Text(text = stringResource(R.string.profile_editor_destination_albums))
                destinationAlbumIds.forEach { destinationId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = albumNameById(
                                albumId = destinationId,
                                albums = availableAlbums,
                                unknownAlbumLabel = unknownAlbumLabel
                            )
                        )
                        TextButton(onClick = { onRemoveDestination(destinationId) }) {
                            Text(text = stringResource(R.string.profile_editor_remove))
                        }
                    }
                }

                Text(text = stringResource(R.string.profile_editor_add_destination))
                availableAlbums
                    .filterNot { destinationAlbumIds.contains(it.id) }
                    .forEach { album ->
                        Button(
                            onClick = { onAddDestination(album.id) }
                        ) {
                            Text(text = stringResource(R.string.profile_editor_add_album, album.name))
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.profile_editor_close))
            }
        }
    )
}

private fun albumNameById(
    albumId: String,
    albums: List<AlbumItem>,
    unknownAlbumLabel: String
): String {
    return albums.firstOrNull { it.id == albumId }?.name ?: unknownAlbumLabel
}
