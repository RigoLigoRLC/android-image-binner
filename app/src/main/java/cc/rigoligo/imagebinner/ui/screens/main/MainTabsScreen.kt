package cc.rigoligo.imagebinner.ui.screens.main

import android.app.Activity
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cc.rigoligo.imagebinner.R
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.data.media.MediaStoreCommitMediaOperations
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.domain.commit.CommitAssignment
import cc.rigoligo.imagebinner.domain.commit.CommitEngine
import cc.rigoligo.imagebinner.domain.commit.CommitRequest
import cc.rigoligo.imagebinner.domain.commit.CommitRunResult
import cc.rigoligo.imagebinner.ui.screens.profiles.ProfilesRoute
import cc.rigoligo.imagebinner.ui.screens.report.CommitReportScreen
import cc.rigoligo.imagebinner.ui.screens.report.CommitReportViewModel
import cc.rigoligo.imagebinner.ui.screens.report.PreparedCommitReportExport
import cc.rigoligo.imagebinner.ui.screens.settings.SettingsRoute
import cc.rigoligo.imagebinner.ui.screens.sorting.SortingRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MainTabDestination(
    val route: String,
    val labelRes: Int
)

private data class PendingCommitExecution(
    val request: CommitRequest
)

private object MainRoute {
    const val Profiles = "profiles"
    const val Settings = "settings"
    const val Sorting = "sorting"
    const val SortingWithArg = "sorting/{profileId}"
    const val CommitReport = "commit-report"
}

private val mainTabs = listOf(
    MainTabDestination(route = MainRoute.Profiles, labelRes = R.string.main_tab_profiles),
    MainTabDestination(route = MainRoute.Settings, labelRes = R.string.main_tab_settings)
)

@Composable
fun MainTabsScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val database = remember(appContext) {
        AppDatabase.getInstance(appContext)
    }
    val settingsManager = remember(database) {
        SettingsManager(database.settingsDao())
    }
    val profileManager = remember(database) {
        ProfileManager(database.profileDao())
    }
    val coroutineScope = rememberCoroutineScope()
    var latestCommitResult by remember { mutableStateOf<CommitRunResult?>(null) }
    var commitInProgress by remember { mutableStateOf(false) }
    var pendingCommitExecution by remember { mutableStateOf<PendingCommitExecution?>(null) }

    val handleCommitResult: (Result<CommitRunResult>) -> Unit = { runResult ->
        commitInProgress = false
        pendingCommitExecution = null
        runResult
            .onSuccess { result ->
                latestCommitResult = result
                navController.navigate(MainRoute.CommitReport) {
                    popUpTo(MainRoute.SortingWithArg) {
                        inclusive = true
                    }
                }
            }
            .onFailure { throwable ->
                val message = if (throwable is SecurityException) {
                    context.getString(R.string.main_commit_permission_required)
                } else {
                    throwable.message ?: context.getString(R.string.main_commit_failed)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }

    val writeAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingCommitExecution
        if (pending == null) {
            commitInProgress = false
            return@rememberLauncherForActivityResult
        }
        if (result.resultCode != Activity.RESULT_OK) {
            commitInProgress = false
            pendingCommitExecution = null
            Toast.makeText(
                context,
                context.getString(R.string.main_commit_cancelled_permission),
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            handleCommitResult(
                runCatching {
                    executeCommit(
                        appContext = appContext,
                        database = database,
                        request = pending.request
                    )
                }
            )
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination
        ?.hierarchy
        ?.any { destination -> mainTabs.any { tab -> tab.route == destination.route } } == true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainTabs.forEach { tab ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == tab.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {},
                            label = {
                                Text(text = stringResource(tab.labelRes))
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = mainTabs.first().route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainRoute.Profiles) {
                ProfilesRoute(
                    onOpenSorting = { profileId ->
                        navController.navigate("${MainRoute.Sorting}/$profileId")
                    }
                )
            }
            composable(MainRoute.Settings) {
                SettingsRoute()
            }
            composable(MainRoute.SortingWithArg) { entry ->
                val profileId = entry.arguments?.getString("profileId")?.toLongOrNull()
                if (profileId == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    SortingRoute(
                        profileId = profileId,
                        onBack = {
                            navController.popBackStack()
                        },
                        onCommit = {
                            if (commitInProgress) {
                                return@SortingRoute
                            }
                            commitInProgress = true
                            coroutineScope.launch {
                                val preparation = runCatching {
                                    prepareCommitExecution(
                                        context = appContext,
                                        database = database,
                                        profileManager = profileManager,
                                        settingsManager = settingsManager
                                    )
                                }

                                preparation
                                    .onFailure { throwable ->
                                        commitInProgress = false
                                        Toast.makeText(
                                            context,
                                            throwable.message ?: context.getString(R.string.main_commit_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onSuccess { pending ->
                                        val uris = writableMediaUris(pending.request.assignments)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uris.isNotEmpty()) {
                                            pendingCommitExecution = pending
                                            val writeRequest = MediaStore.createWriteRequest(
                                                appContext.contentResolver,
                                                uris
                                            )
                                            writeAccessLauncher.launch(
                                                IntentSenderRequest.Builder(writeRequest.intentSender).build()
                                            )
                                        } else {
                                            handleCommitResult(
                                                runCatching {
                                                    executeCommit(
                                                        appContext = appContext,
                                                        database = database,
                                                        request = pending.request
                                                    )
                                                }
                                            )
                                        }
                                    }
                            }
                        }
                    )
                }
            }
            composable(MainRoute.CommitReport) {
                val runResult = latestCommitResult
                if (runResult == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    val reportViewModel = remember(runResult) {
                        CommitReportViewModel(runResult = runResult)
                    }
                    CommitReportScreen(
                        viewModel = reportViewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onExportReady = { export ->
                            val exported = exportReportToDownloads(appContext, export)
                            Toast.makeText(
                                context,
                                if (exported) {
                                    context.getString(R.string.main_report_export_success)
                                } else {
                                    context.getString(R.string.main_report_export_failed)
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

private suspend fun prepareCommitExecution(
    context: Context,
    database: AppDatabase,
    profileManager: ProfileManager,
    settingsManager: SettingsManager
): PendingCommitExecution = withContext(Dispatchers.IO) {
    val sessionDao = database.sessionDao()
    val savedSession = sessionDao.getGlobalSession()
        ?: error(context.getString(R.string.main_error_no_active_session))
    val assignments = sessionDao.getAssignments()
    if (assignments.isEmpty()) {
        error(context.getString(R.string.main_error_no_assignments))
    }

    val profile = profileManager.getProfile(savedSession.profileId)
        ?: error(context.getString(R.string.main_error_profile_not_found))
    val appSettings = settingsManager.getSettings()
    val commitRequest = CommitRequest(
        assignments = assignments.map { assignment ->
            CommitAssignment(
                mediaId = assignment.mediaId,
                targetAlbumId = assignment.targetAlbumId
            )
        },
        sdkInt = Build.VERSION.SDK_INT,
        trashMode = appSettings.trashMode,
        trashAlbumId = profile.destinations.lastOrNull()?.albumId
    )

    PendingCommitExecution(request = commitRequest)
}

private suspend fun executeCommit(
    appContext: Context,
    database: AppDatabase,
    request: CommitRequest
): CommitRunResult = withContext(Dispatchers.IO) {
    val sessionDao = database.sessionDao()
    val runResult = CommitEngine(
        mediaOperations = MediaStoreCommitMediaOperations(
            contentResolver = appContext.contentResolver
        )
    ).commit(request)

    if (runResult.failureCount == 0) {
        sessionDao.clearGlobalSession()
        sessionDao.clearAssignments()
    }
    runResult
}

private fun writableMediaUris(assignments: List<CommitAssignment>): List<Uri> {
    return assignments
        .mapNotNull { assignment ->
            assignment.mediaId.toLongOrNull()?.let { mediaId ->
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
            }
        }
        .distinct()
}

private fun exportReportToDownloads(
    context: Context,
    export: PreparedCommitReportExport
): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, export.fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, export.mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/ImageBinner"
            )
        }
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(export.content.toByteArray(Charsets.UTF_8))
        } ?: error(context.getString(R.string.main_error_unable_open_destination))
        true
    }.getOrElse {
        resolver.delete(uri, null, null)
        false
    }
}
