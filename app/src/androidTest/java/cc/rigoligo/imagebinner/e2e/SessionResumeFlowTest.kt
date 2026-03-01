package cc.rigoligo.imagebinner.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.domain.Profile
import cc.rigoligo.imagebinner.domain.ProfileManager
import cc.rigoligo.imagebinner.domain.SessionManager
import cc.rigoligo.imagebinner.ui.screens.main.MainTabsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionResumeFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var profileManager: ProfileManager
    private lateinit var sessionManager: SessionManager
    private lateinit var profileA: Profile
    private lateinit var profileB: Profile

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        db = AppDatabase.getInstance(context)
        db.clearAllTables()

        profileManager = ProfileManager(db.profileDao())
        sessionManager = SessionManager(db.sessionDao())
        profileA = profileManager.createProfile(
            name = "Profile A",
            sourceAlbumId = "source-a",
            destinationAlbumIds = listOf("bin-a")
        )
        profileB = profileManager.createProfile(
            name = "Profile B",
            sourceAlbumId = "source-b",
            destinationAlbumIds = listOf("bin-b")
        )
        sessionManager.createOrReplaceSession(profileId = profileA.id)
    }

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    @Test
    fun existingSessionFromAnotherProfile_promptsResumeOrDiscard() {
        launchProfilesAndTapStartSorting(profileName = "Profile B")

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("Resume existing session?").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Resume existing session?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume existing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard and start new").assertIsDisplayed()

        composeTestRule.onNodeWithText("Resume existing").performClick()

        assertSortingScreen(profileA.id)
        assertSessionProfile(profileA.id)
        assertBackNavigationToProfiles()
    }

    @Test
    fun discardChoice_startsRequestedProfileInSortingUi() {
        launchProfilesAndTapStartSorting(profileName = "Profile B")
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("Discard and start new").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Discard and start new").performClick()

        assertSortingScreen(profileB.id)
        assertSessionProfile(profileB.id)
    }

    private fun launchProfilesAndTapStartSorting(profileName: String) {
        composeTestRule.setContent {
            MainTabsScreen()
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(profileName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(profileName).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("Start sorting").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start sorting").performClick()
    }

    private fun assertSortingScreen(expectedProfileId: Long) {
        val sortingCurrentText = "Current: Done (swipe up to trash)"
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(sortingCurrentText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("List").assertIsDisplayed()
        composeTestRule.onNodeWithText(sortingCurrentText).assertIsDisplayed()
        assertSessionProfile(expectedProfileId)
    }

    private fun assertBackNavigationToProfiles() {
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("Start sorting").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Start sorting").assertIsDisplayed()
    }

    private fun assertSessionProfile(expectedProfileId: Long) {
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            sessionManager.getCurrentSession()?.profileId == expectedProfileId
        }
        assertEquals(expectedProfileId, sessionManager.getCurrentSession()?.profileId)
    }

}
