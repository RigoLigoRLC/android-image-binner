package cc.rigoligo.imagebinner.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.domain.SortOrder
import cc.rigoligo.imagebinner.domain.TrashMode

@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val viewModel = remember(applicationContext) {
        val database = AppDatabase.getInstance(applicationContext)
        SettingsViewModel(settingsManager = SettingsManager(database.settingsDao()))
    }

    SettingsScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    SettingsScreen(
        state = state,
        onSortOrderSelected = viewModel::setDefaultSortOrder,
        onTrashModeSelected = viewModel::setTrashMode,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSortOrderSelected: (SortOrder) -> Unit,
    onTrashModeSelected: (TrashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsSection(title = "About") {
            Text(
                text = "Image Binner",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Sort photos manually into profile bins and track commits.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        SettingsSection(title = "Default sort order") {
            SortOrder.entries.forEach { sortOrder ->
                SettingRadioOption(
                    label = sortOrder.toLabel(),
                    selected = state.defaultSortOrder == sortOrder,
                    onClick = { onSortOrderSelected(sortOrder) }
                )
            }
        }

        SettingsSection(title = "Trash behavior") {
            SettingRadioOption(
                label = "Move to trash album",
                selected = state.trashMode == TrashMode.TRASH_ALBUM,
                onClick = { onTrashModeSelected(TrashMode.TRASH_ALBUM) }
            )
            if (state.supportsSystemTrash) {
                SettingRadioOption(
                    label = "Use system trash",
                    selected = state.trashMode == TrashMode.SYSTEM_TRASH,
                    onClick = { onTrashModeSelected(TrashMode.SYSTEM_TRASH) }
                )
            } else {
                Text(
                    text = "System trash is unavailable on Android API 24-29.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        content()
    }
}

@Composable
private fun SettingRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun SortOrder.toLabel(): String {
    return when (this) {
        SortOrder.OLDEST_FIRST -> "Oldest first"
        SortOrder.NEWEST_FIRST -> "Newest first"
    }
}
