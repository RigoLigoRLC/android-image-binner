package cc.rigoligo.imagebinner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import cc.rigoligo.imagebinner.ui.navigation.AppNavHost
import cc.rigoligo.imagebinner.ui.theme.ImageBinnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageBinnerTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
