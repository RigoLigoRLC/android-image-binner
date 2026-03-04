package cc.rigoligo.imagebinner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.SettingsManager
import cc.rigoligo.imagebinner.localization.AppLocaleManager
import cc.rigoligo.imagebinner.ui.navigation.AppNavHost
import cc.rigoligo.imagebinner.ui.theme.ImageBinnerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(applicationContext)
        val settingsManager = SettingsManager(database.settingsDao())
        val language = runBlocking(Dispatchers.IO) {
            settingsManager.getLanguage()
        }
        AppLocaleManager().applyLanguage(language)
        enableEdgeToEdge()
        setContent {
            ImageBinnerTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
