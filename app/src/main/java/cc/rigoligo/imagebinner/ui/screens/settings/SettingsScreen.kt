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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cc.rigoligo.imagebinner.R
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.AppLanguage
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.domain.SortOrder
import cc.rigoligo.imagebinner.domain.TrashMode
import cc.rigoligo.imagebinner.localization.AppLocaleManager

@Composable
fun SettingsRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val viewModel = remember(applicationContext) {
        val database = AppDatabase.getInstance(applicationContext)
        val localeManager = AppLocaleManager()
        SettingsViewModel(
            settingsManager = SettingsManager(database.settingsDao()),
            applyLanguage = localeManager::applyLanguage
        )
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
        onLanguageSelected = viewModel::setLanguage,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSortOrderSelected: (SortOrder) -> Unit,
    onTrashModeSelected: (TrashMode) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
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
        SettingsSection(title = stringResource(R.string.settings_about_title)) {
            Text(
                text = stringResource(R.string.settings_about_name),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        SettingsSection(title = stringResource(R.string.settings_default_sort_order_title)) {
            SortOrder.entries.forEach { sortOrder ->
                SettingRadioOption(
                    label = sortOrder.toLabel(),
                    selected = state.defaultSortOrder == sortOrder,
                    onClick = { onSortOrderSelected(sortOrder) }
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_trash_behavior_title)) {
            SettingRadioOption(
                label = stringResource(R.string.settings_trash_mode_album),
                selected = state.trashMode == TrashMode.TRASH_ALBUM,
                onClick = { onTrashModeSelected(TrashMode.TRASH_ALBUM) }
            )
            if (state.supportsSystemTrash) {
                SettingRadioOption(
                    label = stringResource(R.string.settings_trash_mode_system),
                    selected = state.trashMode == TrashMode.SYSTEM_TRASH,
                    onClick = { onTrashModeSelected(TrashMode.SYSTEM_TRASH) }
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_system_trash_unavailable),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_language_title)) {
            Text(
                text = stringResource(R.string.settings_language_reload_notice),
                style = MaterialTheme.typography.bodySmall
            )
            AppLanguage.entries.forEach { language ->
                SettingRadioOption(
                    label = language.toLabel(),
                    selected = state.language == language,
                    onClick = { onLanguageSelected(language) }
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

@Composable
private fun SortOrder.toLabel(): String {
    return when (this) {
        SortOrder.OLDEST_FIRST -> stringResource(R.string.sort_order_oldest_first)
        SortOrder.NEWEST_FIRST -> stringResource(R.string.sort_order_newest_first)
    }
}

@Composable
private fun AppLanguage.toLabel(): String {
    return when (this) {
        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system_default)
        AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
        AppLanguage.SIMPLIFIED_CHINESE -> stringResource(R.string.settings_language_simplified_chinese)
    }
}
