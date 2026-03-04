package cc.rigoligo.imagebinner.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cc.rigoligo.imagebinner.data.local.AppDatabase
import cc.rigoligo.imagebinner.R
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
    private lateinit var appContext: android.content.Context

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        db = AppDatabase.getInstance(appContext.applicationContext)
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

        val promptTitle = appContext.getString(R.string.session_prompt_resume_discard_title)
        val resumeLabel = appContext.getString(R.string.session_prompt_resume_existing)
        val discardLabel = appContext.getString(R.string.session_prompt_discard_and_start_new)
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(promptTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(promptTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(resumeLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(discardLabel).assertIsDisplayed()

        composeTestRule.onNodeWithText(resumeLabel).performClick()

        assertSortingScreen(profileA.id)
        assertSessionProfile(profileA.id)
        assertBackNavigationToProfiles()
    }

    @Test
    fun discardChoice_startsRequestedProfileInSortingUi() {
        launchProfilesAndTapStartSorting(profileName = "Profile B")
        val discardLabel = appContext.getString(R.string.session_prompt_discard_and_start_new)
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(discardLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(discardLabel).performClick()

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

        val startSortingLabel = appContext.getString(R.string.session_start_sorting)
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(startSortingLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(startSortingLabel).performClick()
    }

    private fun assertSortingScreen(expectedProfileId: Long) {
        val sortingCurrentText = appContext.getString(
            R.string.sorting_current_destination,
            appContext.getString(R.string.sorting_unassigned)
        )
        val backLabel = appContext.getString(R.string.sorting_back)
        val listLabel = appContext.getString(R.string.sorting_list)
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(sortingCurrentText).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(backLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(listLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(sortingCurrentText).assertIsDisplayed()
        assertSessionProfile(expectedProfileId)
    }

    private fun assertBackNavigationToProfiles() {
        val backLabel = appContext.getString(R.string.sorting_back)
        val startSortingLabel = appContext.getString(R.string.session_start_sorting)
        composeTestRule.onNodeWithText(backLabel).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText(startSortingLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(startSortingLabel).assertIsDisplayed()
    }

    private fun assertSessionProfile(expectedProfileId: Long) {
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            sessionManager.getCurrentSession()?.profileId == expectedProfileId
        }
        assertEquals(expectedProfileId, sessionManager.getCurrentSession()?.profileId)
    }

}
