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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.R
import cc.rigoligo.imagebinner.data.export.ReportExportStrings
import cc.rigoligo.imagebinner.data.media.MediaStoreRepository
import cc.rigoligo.imagebinner.domain.commit.CommitAction
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
    val fallbackPhotoLabel = stringResource(R.string.fallback_photo)
    val actionMoveToAlbum = stringResource(R.string.report_action_move_to_album)
    val actionMoveToTrashAlbum = stringResource(R.string.report_action_move_to_trash_album)
    val actionMoveToSystemTrash = stringResource(R.string.report_action_move_to_system_trash)
    val statusSuccessCode = stringResource(R.string.report_export_status_success_code)
    val statusFailedCode = stringResource(R.string.report_export_status_failed_code)
    val statusSuccessLabel = stringResource(R.string.report_status_success)
    val statusFailedLabel = stringResource(R.string.report_status_failed)
    val reportFileNamePrefix = stringResource(R.string.report_file_name_prefix)
    val reportFileNameFallback = stringResource(R.string.report_file_name_fallback)
    val reportExportStrings = ReportExportStrings(
        csvHeaderMediaId = context.getString(R.string.report_export_csv_header_media_id),
        csvHeaderTargetAlbumId = context.getString(R.string.report_export_csv_header_target_album_id),
        csvHeaderActionCode = context.getString(R.string.report_export_csv_header_action_code),
        csvHeaderActionLabel = context.getString(R.string.report_export_csv_header_action_label),
        csvHeaderStatusCode = context.getString(R.string.report_export_csv_header_status_code),
        csvHeaderStatusLabel = context.getString(R.string.report_export_csv_header_status_label),
        csvHeaderErrorMessage = context.getString(R.string.report_export_csv_header_error_message),
        actionMoveToAlbum = actionMoveToAlbum,
        actionMoveToTrashAlbum = actionMoveToTrashAlbum,
        actionMoveToSystemTrash = actionMoveToSystemTrash,
        statusSuccessCode = statusSuccessCode,
        statusFailedCode = statusFailedCode,
        statusSuccessLabel = statusSuccessLabel,
        statusFailedLabel = statusFailedLabel
    )

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
                    ) ?: fallbackPhotoLabel
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
            viewModel.requestExport(
                format = ReportExportFormat.CSV,
                strings = CommitReportExportStrings(
                    fileNamePrefix = reportFileNamePrefix,
                    fileNameFallback = reportFileNameFallback,
                    exporterStrings = reportExportStrings
                )
            )
        },
        onExportJson = {
            viewModel.requestExport(
                format = ReportExportFormat.JSON,
                strings = CommitReportExportStrings(
                    fileNamePrefix = reportFileNamePrefix,
                    fileNameFallback = reportFileNameFallback,
                    exporterStrings = reportExportStrings
                )
            )
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
    val fallbackTrashLabel = stringResource(R.string.fallback_trash)
    val fallbackUnknownAlbumLabel = stringResource(R.string.fallback_unknown_album)
    val fallbackPhotoLabel = stringResource(R.string.fallback_photo)
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
                Text(text = stringResource(R.string.report_back))
            }
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(text = "")
        }

        Text(text = stringResource(R.string.report_total, state.totalCount))
        Text(text = stringResource(R.string.report_succeeded, state.successCount))
        Text(text = stringResource(R.string.report_failed, state.failureCount))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = state.showFailedOnly,
                onCheckedChange = onFailedOnlyChanged
            )
            Text(text = stringResource(R.string.report_show_failed_only))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExportCsv) {
                Text(text = stringResource(R.string.report_export_csv))
            }
            Button(onClick = onExportJson) {
                Text(text = stringResource(R.string.report_export_json))
            }
        }

        Text(
            text = if (state.showFailedOnly) {
                stringResource(R.string.report_failed_items)
            } else {
                stringResource(R.string.report_all_items)
            }
        )

        if (state.visibleItems.isEmpty()) {
            Text(text = stringResource(R.string.report_no_items))
        } else {
            state.visibleItems.forEach { item ->
                val targetLabel = when {
                    item.targetAlbumId == CommitRequest.TRASH_TARGET_ID -> fallbackTrashLabel
                    else -> albumNames[item.targetAlbumId] ?: fallbackUnknownAlbumLabel
                }
                ReportItemRow(
                    item = item,
                    mediaLabel = mediaLabels[item.mediaId] ?: fallbackPhotoLabel,
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
    val unknownErrorLabel = stringResource(R.string.report_unknown_error)
    val actionLabel = when (item.action) {
        CommitAction.MOVE_TO_ALBUM -> stringResource(R.string.report_action_move_to_album)
        CommitAction.MOVE_TO_TRASH_ALBUM -> stringResource(R.string.report_action_move_to_trash_album)
        CommitAction.MOVE_TO_SYSTEM_TRASH -> stringResource(R.string.report_action_move_to_system_trash)
    }
    val statusLabel = if (item.success) {
        stringResource(R.string.report_status_success)
    } else {
        stringResource(R.string.report_status_failed)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.report_item_media, mediaLabel))
        Text(text = stringResource(R.string.report_item_target, targetLabel))
        Text(text = stringResource(R.string.report_item_action, actionLabel))
        Text(text = stringResource(R.string.report_item_status, statusLabel))
        if (!item.success) {
            Text(
                text = stringResource(
                    R.string.report_item_error,
                    item.errorMessage ?: unknownErrorLabel
                )
            )
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
