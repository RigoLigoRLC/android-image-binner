package cc.rigoligo.imagebinner.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cc.rigoligo.imagebinner.ui.screens.main.MainTabsScreen
import cc.rigoligo.imagebinner.ui.screens.permission.PermissionGateScreen

private object AppRoute {
    const val PermissionGate = "permission_gate"
    const val MainTabs = "main_tabs"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val requiredPermissions = remember { requiredPhotoPermissions() }
    var hasFullAccess by remember {
        mutableStateOf(context.hasFullPhotoLibraryAccess())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasFullAccess = context.hasFullPhotoLibraryAccess()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFullAccess = context.hasFullPhotoLibraryAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.PermissionGate,
        modifier = modifier
    ) {
        composable(AppRoute.PermissionGate) {
            if (hasFullAccess) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.MainTabs) {
                        popUpTo(AppRoute.PermissionGate) {
                            inclusive = true
                        }
                    }
                }
            }

            PermissionGateScreen(
                onRequestFullAccess = {
                    permissionLauncher.launch(requiredPermissions)
                }
            )
        }
        composable(AppRoute.MainTabs) {
            if (!hasFullAccess) {
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.PermissionGate) {
                        popUpTo(AppRoute.MainTabs) {
                            inclusive = true
                        }
                    }
                }
            }

            MainTabsScreen()
        }
    }
}

private fun requiredPhotoPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun Context.hasFullPhotoLibraryAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
}
