package cc.rigoligo.imagebinner.ui.profiles

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import cc.rigoligo.imagebinner.data.media.AlbumItem
import cc.rigoligo.imagebinner.ui.screens.profiles.ProfileEditorDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileEditorUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addDestination_allowsUserSelectingSpecificAlbum() {
        var addedAlbumId: String? = null

        composeTestRule.setContent {
            ProfileEditorDialog(
                profileName = "Trip",
                sourceAlbumId = "src",
                destinationAlbumIds = emptyList(),
                availableAlbums = listOf(
                    AlbumItem(id = "src", name = "Source", photoCount = 3),
                    AlbumItem(id = "dest-a", name = "Bin A", photoCount = 4),
                    AlbumItem(id = "dest-b", name = "Bin B", photoCount = 2)
                ),
                onDismissRequest = {},
                onSourceAlbumSelected = {},
                onAddDestination = { addedAlbumId = it },
                onRemoveDestination = {}
            )
        }

        composeTestRule.onNodeWithText("Add Bin B").performClick()

        assertEquals("dest-b", addedAlbumId)
    }
}
