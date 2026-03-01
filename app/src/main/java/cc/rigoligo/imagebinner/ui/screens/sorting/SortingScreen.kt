package cc.rigoligo.imagebinner.ui.screens.sorting

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.domain.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

@Composable
fun SortingScreen(
    viewModel: SortingViewModel,
    onBack: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val albumNames by produceState<Map<String, String>>(
        initialValue = emptyMap(),
        key1 = state.destinationAlbumIds
    ) {
        value = withContext(Dispatchers.IO) {
            cc.rigoligo.imagebinner.data.media.MediaStoreRepository(context.contentResolver)
                .listAlbums()
                .associate { album -> album.id to album.name }
        }
    }
    val latestAlbumNames by rememberUpdatedState(albumNames)
    var imageToastMessage by remember { mutableStateOf<String?>(null) }
    var imageToastVisible by remember { mutableStateOf(false) }
    var imageToastRevision by remember { mutableStateOf(0) }
    var pendingSortOrder by remember { mutableStateOf<SortOrder?>(null) }
    val currentAssignmentLabel = state.currentMediaId
        ?.let { mediaId -> state.assignments[mediaId] }
        ?.let { targetAlbumId ->
            if (targetAlbumId == SortingViewModel.TRASH_TARGET_ID) {
                "Trash"
            } else {
                albumNames[targetAlbumId] ?: "Unknown album"
            }
        }
        ?: "Unassigned"

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SortingUiEvent.AssignmentApplied -> {
                    val targetLabel = if (event.targetAlbumId == SortingViewModel.TRASH_TARGET_ID) {
                        "Trash"
                    } else {
                        latestAlbumNames[event.targetAlbumId] ?: "Unknown album"
                    }
                    imageToastMessage = "${event.mediaLabel} -> $targetLabel"
                    imageToastRevision += 1
                    imageToastVisible = true
                }
            }
        }
    }

    LaunchedEffect(imageToastRevision) {
        if (imageToastRevision > 0) {
            val revision = imageToastRevision
            delay(1400)
            if (revision == imageToastRevision) {
                imageToastVisible = false
            }
        }
    }

    LaunchedEffect(imageToastVisible, imageToastRevision) {
        if (!imageToastVisible && imageToastMessage != null) {
            val revision = imageToastRevision
            delay(220)
            if (!imageToastVisible && revision == imageToastRevision) {
                imageToastMessage = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text(text = "Back")
            }
            Text(text = state.overlay.progressLabel, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.showAssignmentList() }) {
                    Text(text = "List")
                }
                TextButton(
                    onClick = onCommit,
                    enabled = state.overlay.canCommit
                ) {
                    Text(text = "Commit")
                }
            }
        }

        Text(text = "Captured: ${formatCapturedAt(state.overlay.capturedAt)}")
        Text(text = "Assigned: ${state.overlay.assignedCount}")
        Text(text = "Current destination: $currentAssignmentLabel")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOrder.entries.forEach { order ->
                val selected = order == state.activeSortOrder
                Button(
                    onClick = { pendingSortOrder = order },
                    enabled = !selected
                ) {
                    Text(text = order.name)
                }
            }
        }
        val requestedSortOrder = pendingSortOrder
        if (requestedSortOrder != null) {
            AlertDialog(
                onDismissRequest = { pendingSortOrder = null },
                title = {
                    Text(text = "Change sort order")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "After switching mode, where should focus go?")
                        TextButton(onClick = {
                            viewModel.setSortOrderOverride(
                                sortOrder = requestedSortOrder,
                                repositionMode = SortRepositionMode.FIRST_UNSORTED
                            )
                            pendingSortOrder = null
                        }) {
                            Text(text = "Jump to first unsorted")
                        }
                        TextButton(onClick = {
                            viewModel.setSortOrderOverride(
                                sortOrder = requestedSortOrder,
                                repositionMode = SortRepositionMode.FIRST_IMAGE
                            )
                            pendingSortOrder = null
                        }) {
                            Text(text = "Jump to first image")
                        }
                        TextButton(onClick = {
                            viewModel.setSortOrderOverride(
                                sortOrder = requestedSortOrder,
                                repositionMode = SortRepositionMode.STAY_AT_PLACE
                            )
                            pendingSortOrder = null
                        }) {
                            Text(text = "Stay at place")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { pendingSortOrder = null }) {
                        Text(text = "Cancel")
                    }
                }
            )
        }

        var zoomScale by remember(state.currentMediaId) { mutableFloatStateOf(1f) }
        val currentMedia = state.currentMedia
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            if (currentMedia == null) {
                Text(
                    text = "No image to display",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentMedia.displayName.ifBlank { "Photo ${state.currentIndex + 1}" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    ZoomableMediaImage(
                        contentResolver = context.contentResolver,
                        mediaId = currentMedia.id,
                        scale = zoomScale,
                        onScaleChanged = { scale -> zoomScale = scale },
                        onSwipePrevious = { viewModel.moveFocusBy(-1) },
                        onSwipeNext = { viewModel.moveFocusBy(1) },
                        onSwipeUpToTrash = viewModel::assignCurrentToTrash,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
            AssignmentDrawerToast(
                visible = imageToastVisible,
                message = imageToastMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        MediaThumbnailStrip(
            mediaItems = state.mediaItems,
            currentMediaId = state.currentMediaId,
            contentResolver = context.contentResolver,
            assignments = state.assignments,
            onSelect = viewModel::focusMedia,
            modifier = Modifier.fillMaxWidth()
        )

        BinRow(
            destinationAlbumIds = state.destinationAlbumIds,
            albumNames = albumNames,
            onAssignDestination = viewModel::assignCurrentToDestination,
            onAssignTrash = viewModel::assignCurrentToTrash
        )

        AssignmentListScreen(
            state = state.assignmentList,
            albumNames = albumNames,
            onCloseRequest = viewModel::hideAssignmentList,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AssignmentDrawerToast(
    visible: Boolean,
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && message != null,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 170),
            initialOffsetY = { -it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 170)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 190),
            targetOffsetY = { -it / 2 }
        ) + fadeOut(animationSpec = tween(durationMillis = 190)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.72f)
        ) {
            Text(
                text = message.orEmpty(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ZoomableMediaImage(
    contentResolver: ContentResolver,
    mediaId: String,
    scale: Float,
    onScaleChanged: (Float) -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipeUpToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = mediaId
    ) {
        value = withContext(Dispatchers.IO) {
            loadMediaBitmap(
                contentResolver = contentResolver,
                mediaId = mediaId,
                targetSizePx = 2000
            )
        }
    }

    val latestScale by rememberUpdatedState(scale)
    val latestOnScaleChanged by rememberUpdatedState(onScaleChanged)
    val latestOnSwipePrevious by rememberUpdatedState(onSwipePrevious)
    val latestOnSwipeNext by rememberUpdatedState(onSwipeNext)
    val latestOnSwipeUpToTrash by rememberUpdatedState(onSwipeUpToTrash)

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(mediaId) {
                forEachGesture {
                    awaitPointerEventScope {
                        var accumulatedDragX = 0f
                        var accumulatedDragY = 0f
                        var maxPointerCount = 0
                        var workingScale = latestScale

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { change -> change.pressed }
                            if (pointerCount == 0) {
                                break
                            }
                            maxPointerCount = maxOf(maxPointerCount, pointerCount)

                            if (maxPointerCount > 1) {
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) {
                                    workingScale = (workingScale * zoomChange).coerceIn(1f, 4f)
                                    latestOnScaleChanged(workingScale)
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } else {
                                val primary = event.changes.firstOrNull() ?: continue
                                val delta = primary.position - primary.previousPosition
                                if (delta.x != 0f || delta.y != 0f) {
                                    accumulatedDragX += delta.x
                                    accumulatedDragY += delta.y
                                    primary.consume()
                                }
                            }
                        }

                        if (maxPointerCount > 1) {
                            return@awaitPointerEventScope
                        }

                        val swipeThresholdPx = 72f
                        if (
                            accumulatedDragY < -swipeThresholdPx &&
                            abs(accumulatedDragY) > abs(accumulatedDragX)
                        ) {
                            latestOnSwipeUpToTrash()
                        } else if (
                            abs(accumulatedDragX) > swipeThresholdPx &&
                            abs(accumulatedDragX) > abs(accumulatedDragY)
                        ) {
                            if (accumulatedDragX > 0f) {
                                latestOnSwipePrevious()
                            } else {
                                latestOnSwipeNext()
                            }
                        }
                    }
                }
            },
        color = Color.Black,
        shape = RoundedCornerShape(8.dp)
    ) {
        if (bitmap == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Unable to load image",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

@Composable
private fun MediaThumbnailStrip(
    mediaItems: List<cc.rigoligo.imagebinner.data.media.PhotoItem>,
    currentMediaId: String?,
    contentResolver: ContentResolver,
    assignments: Map<String, String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentMediaId, mediaItems) {
        val selectedIndex = mediaItems.indexOfFirst { item -> item.id == currentMediaId }
        if (selectedIndex >= 0) {
            val scrollTarget = (selectedIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(scrollTarget)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = mediaItems, key = { it.id }) { mediaItem ->
            val isSelected = mediaItem.id == currentMediaId
            val isAssigned = assignments[mediaItem.id] != null
            val thumbnail by produceState<Bitmap?>(
                initialValue = null,
                key1 = mediaItem.id
            ) {
                value = withContext(Dispatchers.IO) {
                    loadMediaBitmap(
                        contentResolver = contentResolver,
                        mediaId = mediaItem.id,
                        targetSizePx = 240
                    )
                }
            }

            OutlinedButton(
                onClick = { onSelect(mediaItem.id) },
                modifier = Modifier
                    .size(84.dp)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (thumbnail == null) {
                    Text(
                        text = mediaItem.displayName.take(2).ifBlank { "--" },
                        textAlign = TextAlign.Center
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = thumbnail!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isAssigned) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            ) {
                                Text(
                                    text = "OK",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadMediaBitmap(
    contentResolver: ContentResolver,
    mediaId: String,
    targetSizePx: Int
): Bitmap? {
    val mediaLongId = mediaId.toLongOrNull() ?: return null
    val mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaLongId)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val thumbnail = runCatching {
            contentResolver.loadThumbnail(
                mediaUri,
                Size(targetSizePx, targetSizePx),
                null
            )
        }.getOrNull()
        if (thumbnail != null) {
            return thumbnail
        }
    }

    return runCatching {
        contentResolver.openInputStream(mediaUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    }.getOrNull()
}

private fun formatCapturedAt(capturedAt: Long?): String {
    if (capturedAt == null || capturedAt <= 0L) {
        return "-"
    }
    return runCatching {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(capturedAt))
    }.getOrElse { "-" }
}
