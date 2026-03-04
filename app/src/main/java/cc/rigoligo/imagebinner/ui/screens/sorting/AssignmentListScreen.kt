package cc.rigoligo.imagebinner.ui.screens.sorting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.R

@Composable
fun AssignmentListScreen(
    state: AssignmentListUiState,
    albumNames: Map<String, String>,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) {
        return
    }

    AlertDialog(
        onDismissRequest = onCloseRequest,
        title = {
            Text(
                text = stringResource(R.string.sorting_temporary_assignments_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.items.isEmpty()) {
                    Text(text = stringResource(R.string.sorting_no_assignments))
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.items, key = { it.mediaId }) { item ->
                            val targetLabel = when {
                                item.targetAlbumId == SortingViewModel.TRASH_TARGET_ID -> {
                                    stringResource(R.string.fallback_trash)
                                }
                                else -> albumNames[item.targetAlbumId]
                                    ?: stringResource(R.string.fallback_unknown_album)
                            }
                            val mediaLabel = item.mediaLabel
                                ?: item.mediaFallbackIndex?.let { index ->
                                    stringResource(R.string.fallback_photo_index, index)
                                }
                                ?: stringResource(R.string.fallback_photo)
                            Text(text = stringResource(R.string.sorting_assignment_toast, mediaLabel, targetLabel))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCloseRequest) {
                Text(text = stringResource(R.string.sorting_close))
            }
        }
    )
}
