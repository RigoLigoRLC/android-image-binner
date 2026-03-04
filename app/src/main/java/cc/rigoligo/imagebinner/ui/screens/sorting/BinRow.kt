package cc.rigoligo.imagebinner.ui.screens.sorting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.R

@Composable
fun BinRow(
    destinationAlbumIds: List<String>,
    albumNames: Map<String, String>,
    onAssignDestination: (String) -> Unit,
    onAssignTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        destinationAlbumIds.forEach { albumId ->
            Button(onClick = { onAssignDestination(albumId) }) {
                Text(text = albumNames[albumId] ?: stringResource(R.string.fallback_unknown_album))
            }
        }
        OutlinedButton(onClick = onAssignTrash) {
            Text(text = stringResource(R.string.fallback_trash))
        }
    }
}
