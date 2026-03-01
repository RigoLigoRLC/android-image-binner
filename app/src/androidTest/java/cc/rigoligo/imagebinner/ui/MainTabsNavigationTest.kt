package cc.rigoligo.imagebinner.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeTestRule.setContent {
            MainTabsScreen()
        }

        composeTestRule.onNodeWithText("Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
