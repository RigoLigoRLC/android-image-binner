package cc.rigoligo.imagebinner.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.rigoligo.imagebinner.R
import cc.rigoligo.imagebinner.ui.screens.main.MainTabsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainTabsNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainTabs_showProfilesAndSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profilesLabel = context.getString(R.string.main_tab_profiles)
        val settingsLabel = context.getString(R.string.main_tab_settings)

        composeTestRule.setContent {
            MainTabsScreen()
        }

        composeTestRule.onNodeWithText(profilesLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(settingsLabel).assertIsDisplayed()
    }
}
