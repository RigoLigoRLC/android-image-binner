package cc.rigoligo.imagebinner.ui.screens.report

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.commit.CommitRequest
import cc.rigoligo.imagebinner.domain.commit.CommitItemResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CommitReportScreen(
    viewModel: CommitReportViewModel,
    onBack: () -> Unit,
    onExportReady: (PreparedCommitReportExport) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val albumNames by produceState<Map<String, String>>(
        initialValue = emptyMap(),
        key1 = state.itemResults
    ) {
        value = withContext(Dispatchers.IO) {
            MediaStoreRepository(context.contentResolver)
                .listAlbums()
                .associate { album -> album.id to album.name }
        }
    }

    val mediaLabels by produceState<Map<String, String>>(
        initialValue = emptyMap(),
        key1 = state.itemResults
    ) {
        value = withContext(Dispatchers.IO) {
            state.itemResults
                .map { result -> result.mediaId }
                .distinct()
                .associateWith { mediaId ->
                    loadMediaDisplayName(
                        contentResolver = context.contentResolver,
                        mediaId = mediaId
                    ) ?: "Photo"
                }
        }
    }

    LaunchedEffect(state.pendingExport) {
        val pendingExport = state.pendingExport ?: return@LaunchedEffect
        onExportReady(pendingExport)
        viewModel.onExportHandled()
    }

    CommitReportScreen(
        state = state,
        onBack = onBack,
        onFailedOnlyChanged = viewModel::setFailedOnlyFilter,
        onExportCsv = {
            viewModel.requestExport(ReportExportFormat.CSV)
        },
        onExportJson = {
            viewModel.requestExport(ReportExportFormat.JSON)
        },
        albumNames = albumNames,
        mediaLabels = mediaLabels,
        modifier = modifier
    )
}

@Composable
fun CommitReportScreen(
    state: CommitReportUiState,
    onBack: () -> Unit,
    onFailedOnlyChanged: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    albumNames: Map<String, String>,
    mediaLabels: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text(text = "Back")
            }
            Text(text = "Commit report", style = MaterialTheme.typography.titleLarge)
            Text(text = "")
        }

        Text(text = "Total: ${state.totalCount}")
        Text(text = "Succeeded: ${state.successCount}")
        Text(text = "Failed: ${state.failureCount}")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = state.showFailedOnly,
                onCheckedChange = onFailedOnlyChanged
            )
            Text(text = "Show failed only")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExportCsv) {
                Text(text = "Export CSV")
            }
            Button(onClick = onExportJson) {
                Text(text = "Export JSON")
            }
        }

        Text(text = if (state.showFailedOnly) "Failed items" else "All items")

        if (state.visibleItems.isEmpty()) {
            Text(text = "No items to show.")
        } else {
            state.visibleItems.forEach { item ->
                val targetLabel = when {
                    item.targetAlbumId == CommitRequest.TRASH_TARGET_ID -> "Trash"
                    else -> albumNames[item.targetAlbumId] ?: "Unknown album"
                }
                ReportItemRow(
                    item = item,
                    mediaLabel = mediaLabels[item.mediaId] ?: "Photo",
                    targetLabel = targetLabel
                )
            }
        }
    }
}

@Composable
private fun ReportItemRow(
    item: CommitItemResult,
    mediaLabel: String,
    targetLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "Media: $mediaLabel")
        Text(text = "Target: $targetLabel")
        Text(text = "Action: ${item.action.name}")
        Text(text = "Status: ${if (item.success) "Success" else "Failed"}")
        if (!item.success) {
            Text(text = "Error: ${item.errorMessage ?: "Unknown error"}")
        }
    }
}

private fun loadMediaDisplayName(
    contentResolver: ContentResolver,
    mediaId: String
): String? {
    val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
    contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Images.Media._ID} = ?",
        arrayOf(mediaId),
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) {
            return null
        }
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        return cursor.getString(nameColumn)?.takeIf { it.isNotBlank() }
    }
    return null
}
